package woozlabs.echo.domain.gmail.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Thread;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.tasks.v2.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import woozlabs.echo.domain.gmail.dto.autoForwarding.AutoForwardingResponse;
import woozlabs.echo.domain.gmail.dto.draft.*;
import woozlabs.echo.domain.gmail.dto.history.*;
import woozlabs.echo.domain.gmail.dto.message.*;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesResponse;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadTotalCountResponse;
import woozlabs.echo.domain.gmail.dto.pubsub.PubSubWatchRequest;
import woozlabs.echo.domain.gmail.dto.pubsub.PubSubWatchResponse;
import woozlabs.echo.domain.gmail.dto.thread.*;
import woozlabs.echo.domain.gmail.entity.FcmToken;
import woozlabs.echo.domain.gmail.entity.PubSubHistory;
import woozlabs.echo.domain.gmail.exception.GmailException;
import woozlabs.echo.domain.gmail.repository.FcmTokenRepository;
import woozlabs.echo.domain.gmail.repository.PubSubHistoryRepository;
import woozlabs.echo.domain.gmail.util.GmailUtility;
import woozlabs.echo.domain.gmail.validator.PubSubValidator;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.entity.MemberAccount;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.domain.member.repository.query.MemberAccountQueryRepository;
import woozlabs.echo.domain.member.service.AccountService;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;
import woozlabs.echo.global.utils.GlobalUtility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static woozlabs.echo.global.constant.GlobalConstant.*;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableAsync
public class GmailService {
    // constant value
    private final String projectId = "echo-email-app";
    private final String locationId = "us-central1";
    private final String queueId = "echo-email-lazy-send-queue";
    // injection & init
    private final MultiThreadGmailService multiThreadGmailService;
    private final AccountRepository accountRepository;
    private final PubSubHistoryRepository pubSubHistoryRepository;
    private final MemberAccountQueryRepository memberAccountQueryRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final GmailUtility gmailUtility;
    private final PubSubValidator pubSubValidator;
    private final AccountService accountService;
    private final RedisTemplate<String, Object> redisTemplate;

    public GmailThreadListResponse getQueryUserEmailThreads(String accessToken, String pageToken, Long maxResults, String q, String aAUid) {
        // last login update
        accountService.findAccountAndUpdateLastLogin(aAUid);

        Gmail gmailService = gmailUtility.createGmailService(accessToken);
        // ---- temp data ----
        LocalDate currentDate = LocalDate.now();
        Boolean isBilling = Boolean.FALSE;
        // -------------------
        ListThreadsResponse response = getQueryListThreadsResponse(pageToken, maxResults, q, gmailService);
        List<Thread> threads = response.getThreads(); // get threads
        threads = isEmptyResult(threads);
        List<GmailThreadListThreads> detailedThreads = getDetailedThreads(threads, gmailService); // get detailed threads
        if(pageToken != null){
            validatePaymentThread(detailedThreads, currentDate);
        }
        return GmailThreadListResponse.builder()
                .threads(detailedThreads)
                .nextPageToken(response.getNextPageToken())
                .build();
    }

    public GmailThreadGetResponse getUserEmailThread(String accessToken, String id){
        Gmail gmailService = gmailUtility.createGmailService(accessToken);
        GmailThreadGetResponse gmailThreadGetResponse = new GmailThreadGetResponse();
        Thread thread = getOneThreadResponse(id, gmailService);
        List<Message> messages = thread.getMessages();
        List<GmailThreadGetMessagesFrom> froms = new ArrayList<>();
        List<GmailThreadGetMessagesCc> ccs = new ArrayList<>();
        List<GmailThreadGetMessagesBcc> bccs = new ArrayList<>();
        Map<String, GmailThreadListAttachments> attachments = new HashMap<>();
        Map<String, GmailThreadListInlineImages> inlineImages = new HashMap<>();
        List<GmailThreadGetMessagesResponse> convertedMessages = new ArrayList<>();
        List<String> labelIds = new ArrayList<>();
        Map<String, String> messageIdMapping = new HashMap<>();
        for (int idx = 0; idx < messages.size(); idx++) {
            int idxForLambda = idx;
            Message message = messages.get(idx);
            MessagePart payload = message.getPayload();
            convertedMessages.add(GmailThreadGetMessagesResponse.toGmailThreadGetMessages(message, messageIdMapping));
            List<MessagePartHeader> headers = payload.getHeaders(); // parsing header
            labelIds.addAll(message.getLabelIds());
            if (idxForLambda == messages.size() - 1) {
                Long date = convertedMessages.get(convertedMessages.size() - 1).getTimestamp();
                gmailThreadGetResponse.setSnippet(message.getSnippet());
                gmailThreadGetResponse.setTimestamp(date);
            }
            // get attachments
            getThreadsAttachments(payload, attachments, inlineImages);
            headers.forEach((header) -> {
                String headerName = header.getName().toUpperCase();
                // first message -> extraction subject
                if (idxForLambda == 0 && headerName.equals(THREAD_PAYLOAD_HEADER_SUBJECT_KEY)) {
                    gmailThreadGetResponse.setSubject(header.getValue());
                }
            });
            GmailThreadGetMessagesResponse gmailThreadGetMessage = convertedMessages.get(convertedMessages.size() - 1);
            froms.add(gmailThreadGetMessage.getFrom());
            ccs.addAll(gmailThreadGetMessage.getCc());
            bccs.addAll(gmailThreadGetMessage.getBcc());
        }
        gmailThreadGetResponse.setLabelIds(labelIds.stream().distinct().collect(Collectors.toList()));
        gmailThreadGetResponse.setId(id);
        gmailThreadGetResponse.setHistoryId(thread.getHistoryId());
        gmailThreadGetResponse.setFrom(froms.stream().distinct().toList());
        gmailThreadGetResponse.setCc(ccs.stream().distinct().toList());
        gmailThreadGetResponse.setBcc(bccs.stream().distinct().toList());
        gmailThreadGetResponse.setThreadSize(messages.size());
        gmailThreadGetResponse.setAttachments(attachments);
        gmailThreadGetResponse.setAttachmentSize(attachments.size());
        gmailThreadGetResponse.setInlineImages(inlineImages);
        gmailThreadGetResponse.setInlineImageSize(inlineImages.size());
        gmailThreadGetResponse.setMessages(convertedMessages);
        return gmailThreadGetResponse;
    }

    public GmailThreadTrashResponse trashUserEmailThread(String accessToken, String id){
        Gmail gmailService = gmailUtility.createGmailService(accessToken);
        try{
            Thread trashedThread = gmailService.users().threads().trash(USER_ID, id)
                    .setPrettyPrint(Boolean.TRUE)
                    .execute();
            return new GmailThreadTrashResponse(trashedThread.getId());
        }catch (IOException e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREAD_TRASH_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_THREAD_TRASH_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public GmailThreadDeleteResponse deleteUserEmailThread(String accessToken, String id) {
        Gmail gmailService = gmailUtility.createGmailService(accessToken);
        try {
            gmailService.users().threads().delete(USER_ID, id)
                    .setPrettyPrint(Boolean.TRUE)
                    .execute();
            return new GmailThreadDeleteResponse(id);
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREAD_DELETE_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_THREAD_DELETE_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public GmailThreadSearchListResponse searchUserEmailThreads(String accessToken, GmailSearchParams params){
        Gmail gmailService = gmailUtility.createGmailService(accessToken);
        ListThreadsResponse response = getSearchListThreadsResponse(params, gmailService);
        List<Thread> threads = response.getThreads();
        threads = isEmptyResult(threads);
        List<GmailThreadSearchListThreads> searchedThreads = getSimpleThreads(threads); // get detailed threads
        return GmailThreadSearchListResponse.builder()
                .threads(searchedThreads)
                .nextPageToken(response.getNextPageToken())
                .build();
    }

    public GmailMessageGetResponse getUserEmailMessage(String accessToken, String messageId){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Message message = gmailService.users().messages().get(USER_ID, messageId).execute();
            GmailMessageGetResponse response = GmailMessageGetResponse.toGmailMessageGet(message, gmailUtility);
            GmailReferenceExtractionResponse references = extractReferences(message, gmailService);
            response.setReferences(references.getReferences());
            return response;
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_GET_API_ERROR_MESSAGE, e.getMessage());
        }
    }

    public GmailMessageGetResponse getUserEmailMessageWithoutVerification(String uid, String messageId) throws Exception {
        Account account = accountRepository.findByUid(uid).orElseThrow(
                () -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));
        String accessToken = account.getAccessToken();
        Gmail gmailService = gmailUtility.createGmailService(accessToken);
        Message message = gmailService.users().messages().get(USER_ID, messageId).execute();
        return GmailMessageGetResponse.toGmailMessageGet(message, gmailUtility);
    }

    public GmailMessageAttachmentResponse getAttachment(String accessToken, String messageId, String id){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            MessagePartBody attachment = gmailService.users().messages()
                    .attachments()
                    .get(USER_ID, messageId, id)
                    .execute();
            String standardBase64 = attachment.getData()
                    .replace('-', '+')
                    .replace('_', '/');
            // Add padding if necessary
            int paddingCount = (4 - (standardBase64.length() % 4)) % 4;
            for (int i = 0; i < paddingCount; i++) {
                standardBase64 += "=";
            }
            byte[] decodedBinaryContent = java.util.Base64.getDecoder().decode(standardBase64);
            //byte[] attachmentData = java.util.Base64.getDecoder().decode(attachment.getData());
            String standardData = java.util.Base64.getEncoder().encodeToString(decodedBinaryContent);
            return GmailMessageAttachmentResponse.builder()
                    .attachmentId(attachment.getAttachmentId())
                    .size(attachment.getSize())
                    .data(standardData)
                    .build();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_ATTACHMENTS_GET_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_MESSAGES_ATTACHMENTS_GET_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    @Async
    public void sendUserEmailMessageWithAtt(String accessToken, GmailMessageSendRequestWithAtt request){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Profile profile = gmailService.users().getProfile(USER_ID).execute();
            String fromEmailAddress = profile.getEmailAddress();
            request.setFromEmailAddress(fromEmailAddress);
            MimeMessage mimeMessage = createEmailWithAtt(request);
            String uploadUrl = initiateResumableSession(accessToken);
            uploadEmailData(accessToken, uploadUrl, mimeMessage);
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE,
                    e.getMessage()
            );
        }
    }

    @Async
    public void sendEmailReply(String accessToken, GmailMessageSendRequestWithAtt request){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Profile profile = gmailService.users().getProfile(USER_ID).execute();
            String fromEmailAddress = profile.getEmailAddress();
            request.setFromEmailAddress(fromEmailAddress);
            // set message setting for reply
            Message lastMessage = gmailService.users().messages().get(USER_ID, request.getMessageId()).execute();
            String threadSubject = lastMessage.getPayload().getHeaders().stream()
                    .filter(header -> header.getName().equalsIgnoreCase("Subject"))
                    .findFirst()
                    .map(MessagePartHeader::getValue)
                    .orElse("");
            String originalMessageId = "";
            String originalReferences = "";
            for(MessagePartHeader header : lastMessage.getPayload().getHeaders()){
                if(header.getName().equalsIgnoreCase("Message-ID")){
                    originalMessageId = header.getValue();
                }else if(header.getName().equalsIgnoreCase("References")) {
                    originalReferences = header.getValue();
                }
            }
            if(!validateChangedSubject(request.getSubject(), threadSubject)){ // send reply
                request.setSubject(request.getSubject());
                MimeMessage mimeMessage = createEmailWithAtt(request);
                // 답장 관련 헤더 설정
                if (!originalMessageId.isEmpty()) mimeMessage.setHeader("In-Reply-To", originalMessageId);
                if(!originalReferences.isEmpty()) mimeMessage.setHeader("References", originalReferences + " " + originalMessageId);
                Message message = createMessage(mimeMessage);
                message.setThreadId(lastMessage.getThreadId());
                gmailService.users().messages().send(USER_ID, message).execute();
                return;
            }
            request.setSubject(request.getSubject());
            MimeMessage mimeMessage = createEmailWithAtt(request);
            Message message = createMessage(mimeMessage);
            gmailService.users().messages().send(USER_ID, message).execute();
        }catch (Exception e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE,
                    e.getMessage()
            );
        }
    }

    @Async
    public void sendEmailForwarding(String accessToken, GmailMessageSendRequestWithAtt request){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Profile profile = gmailService.users().getProfile(USER_ID).execute();
            String fromEmailAddress = profile.getEmailAddress();
            request.setFromEmailAddress(fromEmailAddress);
            // set reply message
            Message lastMessage = gmailService.users().messages().get(USER_ID, request.getMessageId()).execute();
            String threadSubject = lastMessage.getPayload().getHeaders().stream()
                    .filter(header -> header.getName().equalsIgnoreCase("Subject"))
                    .findFirst()
                    .map(MessagePartHeader::getValue)
                    .orElse("");
            String originalMessageId = lastMessage.getPayload().getHeaders().stream()
                    .filter(header -> header.getName().equalsIgnoreCase("Message-ID"))
                    .findFirst()
                    .map(MessagePartHeader::getValue)
                    .orElse("");
            if(!validateChangedSubject(request.getSubject(), threadSubject)){ // send reply
                request.setSubject(request.getSubject());
                MimeMessage mimeMessage = createEmailWithAtt(request);
                // 답장 관련 헤더 설정
                if (!originalMessageId.isEmpty()) {
                    mimeMessage.setHeader("In-Reply-To", originalMessageId);
                    mimeMessage.setHeader("References", originalMessageId);
                }
                Message message = createMessage(mimeMessage);
                message.setThreadId(lastMessage.getThreadId());
                gmailService.users().messages().send(USER_ID, message).execute();
                return;
            }
            request.setSubject(request.getSubject());
            MimeMessage mimeMessage = createEmailWithAtt(request);
            Message message = createMessage(mimeMessage);
            gmailService.users().messages().send(USER_ID, message).execute();
        }catch (Exception e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE,
                    e.getMessage()
            );
        }
    }

    public GmailMessageLazySendRequest getLazySendRequestDto(GmailMessageSendRequestByWebHook dto){
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        GmailMessageLazySendRequest redisResponse = (GmailMessageLazySendRequest) ops.getAndDelete(dto.getTaskId());
        if(redisResponse == null){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE.getMessage()
            );
        }
        return redisResponse;
    }

    public void sendScheduleEmail(GmailMessageSendRequestWithAtt request, LocalDateTime scheduleTime){
        // scheduling validation
        // create job

    }

    public GmailThreadTotalCountResponse getUserEmailThreadsTotalCount(String accessToken, String label){
        Gmail gmailService = gmailUtility.createGmailService(accessToken);
        int totalCount = getTotalCountThreads(gmailService, label);
        return GmailThreadTotalCountResponse.builder()
                .totalCount(totalCount)
                .build();
    }

    @Async
    public GmailDraftSendResponse sendUserEmailDraft(String accessToken, GmailDraftCommonRequest request){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Profile profile = gmailService.users().getProfile(USER_ID).execute();
            String fromEmailAddress = profile.getEmailAddress();
            request.setFromEmailAddress(fromEmailAddress);
            MimeMessage mimeMessage = createDraft(request);
            Message message = createMessage(mimeMessage);
            // create draft
            Draft draft = new Draft();
            draft.setMessage(message);
            Message responseMessage = gmailService.users().drafts().send(USER_ID, draft).execute();
            return GmailDraftSendResponse.builder()
                    .id(responseMessage.getId())
                    .threadId(responseMessage.getThreadId())
                    .labelsId(responseMessage.getLabelIds())
                    .snippet(responseMessage.getSnippet()).build();
        }catch (Exception e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_DRAFTS_SEND_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_DRAFTS_SEND_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public void deleteDraft(String accessToken, String id) {
        try {
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            gmailService.users().drafts().delete(USER_ID, id).execute();
        } catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_DRAFTS_DELETE_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_DRAFTS_DELETE_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public GmailDraftListResponse getUserEmailDrafts(String accessToken, String pageToken, Long maxResult, String q){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            ListDraftsResponse draftsResponse = getListDraftsResponse(gmailService, pageToken, maxResult, q);
            // ---- temp data ----
            LocalDate currentDate = LocalDate.now();
            Boolean isBilling = Boolean.FALSE;
            // -------------------
            List<Draft> drafts = draftsResponse.getDrafts();
            drafts = isEmptyResult(drafts);
            List<GmailDraftListDrafts> detailedDrafts = getDetailedDrafts(drafts, gmailService); // get detailed threads
            List<GmailDraftDetailInList> responseDrafts = detailedDrafts.stream().map((detailedDraft) -> {
                GmailDraftDetailInList responseDraft = new GmailDraftDetailInList();
                responseDraft.setDraftId(detailedDraft.getId());
                responseDraft.setMessage(detailedDraft.getMessage());
                return responseDraft;
            }).toList();
            if(pageToken != null){
                validatePaymentDraft(detailedDrafts, currentDate);
            }
            return GmailDraftListResponse.builder()
                    .drafts(responseDrafts)
                    .nextPageToken(draftsResponse.getNextPageToken())
                    .build();
        }catch (IOException e) {
            throw new IllegalArgumentException("Error occurred while getting drafts");
        }
    }

    public GmailDraftGetResponse getUserEmailDraft(String accessToken, String id){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Draft draft = getOneDraftResponse(id, gmailService);
            GmailDraftGetMessageResponse message = GmailDraftGetMessageResponse.toGmailDraftGetMessage(draft.getMessage());
            return GmailDraftGetResponse.builder()
                    .draftId(draft.getId())
                    .message(message)
                    .build();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_DRAFTS_GET_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_DRAFTS_GET_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public GmailDraftUpdateResponse updateUserEmailDraft(String accessToken, String id, GmailDraftCommonRequest request){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Profile profile = gmailService.users().getProfile(USER_ID).execute();
            // get existing draft
            Draft previousDraft = gmailService.users().drafts().get(USER_ID, id).execute();
            String previousThreadId = previousDraft.getMessage().getThreadId();
            String fromEmailAddress = profile.getEmailAddress();
            request.setFromEmailAddress(fromEmailAddress);
            MimeMessage mimeMessage = createDraft(request);
            Message message = createMessage(mimeMessage);
            message.setThreadId(previousThreadId);
            // create new draft
            Draft draft = new Draft().setMessage(message);
            draft = gmailService.users().drafts().update(USER_ID, id, draft).execute();
            return GmailDraftUpdateResponse.builder()
                    .draftId(draft.getId())
                    .messageId(draft.getMessage().getId())
                    .build();
        }catch (Exception e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_DRAFTS_UPDATE_API_ERROR_MESSAGE,
                    e.getMessage()
            );
        }
    }

    public GmailDraftCreateResponse createUserEmailDraft(String accessToken, GmailDraftCommonRequest request){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            Profile profile = gmailService.users().getProfile(USER_ID).execute();
            String fromEmailAddress = profile.getEmailAddress();
            request.setFromEmailAddress(fromEmailAddress);
            MimeMessage mimeMessage = createDraft(request);
            Message message = createMessage(mimeMessage);
            message.setThreadId(request.getThreadId());
            // create new draft
            Draft draft = new Draft().setMessage(message);
            Draft newDraft = gmailService.users().drafts().create(USER_ID, draft).execute();
            return GmailDraftCreateResponse.builder()
                    .messageId(newDraft.getMessage().getId())
                    .draftId(newDraft.getId())
                    .build();
        }catch (Exception e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE.getMessage()
            );
        }finally {
            for (File file : request.getFiles()) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    public GmailThreadUpdateResponse updateUserEmailThread(String accessToken, String id, GmailThreadUpdateRequest request){
        try {
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            ModifyThreadRequest modifyThreadRequest = new ModifyThreadRequest();
            modifyThreadRequest.setAddLabelIds(request.getAddLabelIds());
            modifyThreadRequest.setRemoveLabelIds(request.getRemoveLabelIds());
            gmailService.users().threads().modify(USER_ID, id, modifyThreadRequest).execute();
            return GmailThreadUpdateResponse.builder()
                    .addLabelIds(request.getAddLabelIds())
                    .removeLabelIds(request.getRemoveLabelIds())
                    .build();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREADS_MODIFY_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_THREADS_MODIFY_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public GmailMessageUpdateResponse updateUserEmailMessage(String accessToken, String id, GmailMessageUpdateRequest request){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            ModifyMessageRequest modifyMessageRequest = new ModifyMessageRequest();
            modifyMessageRequest.setAddLabelIds(request.getAddLabelIds());
            modifyMessageRequest.setRemoveLabelIds(request.getRemoveLabelIds());
            gmailService.users().messages().modify(USER_ID, id, modifyMessageRequest).execute();
            return GmailMessageUpdateResponse.builder()
                    .addLabelIds(request.getAddLabelIds())
                    .removeLabelIds(request.getRemoveLabelIds())
                    .build();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_MODIFY_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_MESSAGES_MODIFY_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    @Transactional
    public PubSubWatchResponse subscribePubSub(Account activeAccount, PubSubWatchRequest dto){
        try {
            List<FcmToken> fcmTokens = fcmTokenRepository.findByAccount(activeAccount);
            pubSubValidator.validateWatch(fcmTokens);
            String accessToken = activeAccount.getAccessToken();
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            WatchRequest watchRequest = new WatchRequest()
                    .setLabelIds(dto.getLabelIds())
                    .setLabelFilterBehavior("include")
                    .setTopicName("projects/echo-email-app/topics/gmail");
            WatchResponse watchResponse = gmailService.users().watch(USER_ID, watchRequest).execute();
            Optional<PubSubHistory> pubSubHistory = pubSubHistoryRepository.findByAccount(activeAccount);
            if(pubSubHistory.isEmpty()){
                PubSubHistory newHistory = PubSubHistory.builder()
                        .historyId(watchResponse.getHistoryId())
                        .account(activeAccount).build();
                pubSubHistoryRepository.save(newHistory);
            }else{
                PubSubHistory findHistory = pubSubHistory.get();
                findHistory.updateHistoryId(watchResponse.getHistoryId());
            }
            return PubSubWatchResponse.builder()
                    .historyId(watchResponse.getHistoryId())
                    .expiration(watchResponse.getExpiration()).build();
        }catch (Exception e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_WATCH_API_ERROR_MESSAGE,
                    e.getMessage()
            );
        }
    }

    public void stopPubSub(String uid, String aAUid){
        try{
            if(aAUid != null){
                Account account = accountRepository.findByUid(aAUid).orElseThrow(
                        () -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));
                String accessToken = account.getAccessToken();
                Gmail gmailService = gmailUtility.createGmailService(accessToken);
                gmailService.users().stop(USER_ID).execute();
            }else{
                List<MemberAccount> memberAccounts = memberAccountQueryRepository.findByMemberPrimaryUid(uid);
                for(MemberAccount memberAccount : memberAccounts){
                    Account account = memberAccount.getAccount();
                    String accessToken = account.getAccessToken();
                    Gmail gmailService = gmailUtility.createGmailService(accessToken);
                    gmailService.users().stop(USER_ID).execute();
                }
            }
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_STOP_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_STOP_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public GmailHistoryListResponse getHistories(String accessToken, String historyId, String pageToken){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            ListHistoryResponse historyResponse = gmailService
                    .users()
                    .history()
                    .list(USER_ID)
                    .setLabelId(HISTORY_INBOX_LABEL)
                    .setPageToken(pageToken)
                    .setStartHistoryId(new BigInteger(historyId))
                    .execute();
            List<History> histories = historyResponse.getHistory(); // get histories
            GmailHistoryListResponse response = GmailHistoryListResponse.builder()
                    .nextPageToken(historyResponse.getNextPageToken())
                    .historyId(historyResponse.getHistoryId())
                    .build();
            if(histories == null) return response;
            // convert history format
            List<GmailHistoryListData> historyListData = histories.stream().map((history) -> {
                List<GmailHistoryListMessageAdded> messagesAdded = history.getMessagesAdded() != null
                        ? history.getMessagesAdded().stream()
                        .map(GmailHistoryListMessageAdded::toGmailHistoryListMessageAdded)
                        .toList()
                        : Collections.emptyList();

                List<GmailHistoryListMessageDeleted> messagesDeleted = history.getMessagesDeleted() != null
                        ? history.getMessagesDeleted().stream()
                        .map(GmailHistoryListMessageDeleted::toGmailHistoryListMessageDeleted)
                        .toList()
                        : Collections.emptyList();

                List<GmailHistoryListLabelAdded> labelsAdded = history.getLabelsAdded() != null
                        ? history.getLabelsAdded().stream()
                        .map(GmailHistoryListLabelAdded::toGmailHistoryListLabelAdded)
                        .toList()
                        : Collections.emptyList();

                List<GmailHistoryListLabelRemoved> labelsRemoved = history.getLabelsRemoved() != null
                        ? history.getLabelsRemoved().stream()
                        .map(GmailHistoryListLabelRemoved::toGmailHistoryListLabelRemoved)
                        .toList()
                        : Collections.emptyList();
                return GmailHistoryListData.builder()
                        .messagesAdded(messagesAdded)
                        .messagesDeleted(messagesDeleted)
                        .labelsAdded(labelsAdded)
                        .labelsRemoved(labelsRemoved)
                        .build();
            }).toList();
            response.setHistory(historyListData);
            return response;
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_HISTORY_LIST_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_HISTORY_LIST_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public AutoForwardingResponse setUpAutoForwarding(String accessToken, String q, String email){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            addForwardingAddress(email, gmailService);
            createFilter(q, email, gmailService);
            return AutoForwardingResponse.builder()
                    .q(q)
                    .forwardingEmail(email)
                    .build();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_SETTINGS_FILTERS_CREATE_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_SETTINGS_FILTERS_CREATE_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public void generateVerificationLabel(String accessToken){
        try{
            // find echo verification label
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            ListLabelsResponse listLabelsResponse = gmailService.users().labels().list(USER_ID).execute();
            for(Label label : listLabelsResponse.getLabels()){
                if(label.getName().equals(PARENT_VERIFICATION_LABEL + "/" + CHILD_VERIFICATION_LABEL)){
                    return;
                }
            }
            // create echo verification label
            Label parentLabel = new Label()
                    .setName(PARENT_VERIFICATION_LABEL)
                    .setLabelListVisibility("labelShow")
                    .setMessageListVisibility("show");
            gmailService.users().labels().create(USER_ID, parentLabel).execute();
            Label childLabel = new Label()
                    .setName(PARENT_VERIFICATION_LABEL + "/" + CHILD_VERIFICATION_LABEL)
                    .setLabelListVisibility("labelShow")
                    .setMessageListVisibility("show");
            gmailService.users().labels().create(USER_ID, childLabel).execute();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_LABELS_CREATE_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_LABELS_CREATE_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public GmailMessageAttachmentDownloadResponse downloadAttachment(String accessToken, String messageId, String attachmentId){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            MessagePartBody attachPart = gmailService
                    .users()
                    .messages()
                    .attachments()
                    .get(USER_ID, messageId, attachmentId).execute();
            String standardBase64 = attachPart.getData()
                    .replace('-', '+')
                    .replace('_', '/');
            // Add padding if necessary
            int paddingCount = (4 - (standardBase64.length() % 4)) % 4;
            for (int i = 0; i < paddingCount; i++) {
                standardBase64 += "=";
            }
            byte[] decodedBinaryContent = java.util.Base64.getDecoder().decode(standardBase64);
            ByteArrayResource resource = new ByteArrayResource(decodedBinaryContent);
            return GmailMessageAttachmentDownloadResponse.builder()
                    .attachmentId(attachmentId)
                    .size(attachPart.getSize())
                    .byteData(decodedBinaryContent)
                    .resource(resource)
                    .build();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_ATTACHMENTS_GET_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_MESSAGES_ATTACHMENTS_GET_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    // cancel message test
    public GmailMessageLazySendResponse sendMessageWithCloudTask(HttpServletRequest httpServletRequest, GmailMessageLazySendRequest request, String aAUid){
        try{
            // create cloud task queue object
            CloudTasksClient tasksClient = CloudTasksClient.create(CloudTasksSettings.newBuilder()
                    .setCredentialsProvider(this::getDefaultServiceAccount)
                    .build());
            QueueName cloudTaskQueue = QueueName.of(projectId, locationId, queueId);
            String url = String.format("https://api-dev.monomail.co/api/v1/gmail/messages/send?aAUid=%s", aAUid);
            // http request obj
            String taskId = UUID.randomUUID() + "_" + aAUid;
            String jsonPayload = "{\"taskId\":\"" + taskId + "\"}";
            org.apache.http.HttpEntity entity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .setBody(ByteString.copyFromUtf8(jsonPayload))
                    .setUrl(url)
                    .putHeaders("Content-Type", entity.getContentType().getValue())
                    .putHeaders("Authorization", httpServletRequest.getHeader("Authorization"))
                    .build();
            // Config delay for the lazy sending email
            Task.Builder taskBuilder = Task.newBuilder();
            // Set the HTTP request in the task
            String taskName = TaskName.of(projectId, locationId, queueId, taskId).toString();
            taskBuilder
                    .setHttpRequest(httpRequest)
                    .setName(taskName)
                    .build();
            Instant delayTime = Instant.now().plusSeconds(10);
            com.google.protobuf.Timestamp scheduleTime = Timestamp.newBuilder()
                    .setSeconds(delayTime.getEpochSecond())
                    .setNanos(delayTime.getNano())
                    .build();
            taskBuilder.setScheduleTime(scheduleTime);
            // save request data in redis
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            ops.set(taskId, request);
            tasksClient.createTask(cloudTaskQueue, taskBuilder.build());
            tasksClient.close();
            log.info("Register lazy send message request to Cloud Task");
            return GmailMessageLazySendResponse.builder()
                    .taskId(taskId)
                    .build();
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE,
                    e.getMessage()
            );
        }
    }

    public void cancelSendMessage(String aAUid, String taskId) {
        try{
            // create cloud task queue object
            CloudTasksClient tasksClient = CloudTasksClient.create(CloudTasksSettings.newBuilder()
                    .setCredentialsProvider(this::getDefaultServiceAccount)
                    .build());
            // validation check
            TaskName taskName = TaskName.of(projectId, locationId, queueId, taskId);
            DeleteTaskRequest deleteTaskRequest = DeleteTaskRequest.newBuilder()
                    .setName(taskName.toString())
                    .build();
            tasksClient.deleteTask(deleteTaskRequest);
            log.info("Cancel lazy send message request to Cloud Task");
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_CANCEL_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_CANCEL_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    public void deleteMessage(String accessToken, String id){
        try{
            Gmail gmailService = gmailUtility.createGmailService(accessToken);
            gmailService.users().messages().delete(USER_ID, id).execute();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_DELETE_API_ERROR_MESSAGE,
                    e.getMessage()
            );
        }
    }

    private GoogleCredentials getDefaultServiceAccount() throws IOException {
        return GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    private Boolean validateChangedSubject(String subject, String threadSubject) {
        if (subject.startsWith("Re: ")) {
            subject = subject.substring(4); // Remove "Re: "
        } else if (subject.startsWith("Fwd: ")) {
            subject = subject.substring(5); // Remove "Fwd: "
        }
        if (threadSubject.startsWith("Re: ")) {
            threadSubject = threadSubject.substring(4); // Remove "Re: "
        } else if (threadSubject.startsWith("Fwd: ")) {
            threadSubject = threadSubject.substring(5); // Remove "Fwd: "
        }
        if(subject.strip().equals(threadSubject.strip())) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    // Methods : get something
    private List<GmailThreadListThreads> getDetailedThreads(List<Thread> threads, Gmail gmailService) {
        //int nThreads = Runtime.getRuntime().availableProcessors();
        int nThreads = 25;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        List<CompletableFuture<GmailThreadListThreads>> futures = threads.stream()
                .map((thread) -> {
                    CompletableFuture<GmailThreadListThreads> future = new CompletableFuture<>();
                    Map<String, String> messageIdMapping = new HashMap<>();
                    executor.execute(() -> {
                        try{
                            GmailThreadListThreads result = multiThreadGmailService
                                    .multiThreadRequestGmailThreadGetForList(thread, gmailService, messageIdMapping);
                            future.complete(result);
                        }catch (Exception e){
                            log.error(REQUEST_GMAIL_USER_MESSAGES_GET_API_ERR_MSG);
                            future.completeExceptionally(new GmailException(REQUEST_GMAIL_USER_MESSAGES_GET_API_ERR_MSG));
                        }
                    });
                    return future;
                }).toList();
        List<GmailThreadListThreads> getThreadsResult = futures.stream().map((future) -> {
            try{
                return future.get();
            }catch (Exception e){
                log.error(e.getMessage());
                throw new GmailException(REQUEST_GMAIL_USER_MESSAGES_GET_API_ERR_MSG);
            }
        }).toList();
        return getThreadsResult.stream().filter((getThreadResult) -> !getThreadResult.getMessages().isEmpty()).toList();
    }

    private List<GmailDraftListDrafts> getDetailedDrafts(List<Draft> drafts, Gmail gmailService) {
        //int nThreads = Runtime.getRuntime().availableProcessors();
        int nThreads = 25;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        List<CompletableFuture<GmailDraftListDrafts>> futures = drafts.stream()
                .map((draft) -> {
                    CompletableFuture<GmailDraftListDrafts> future = new CompletableFuture<>();
                    executor.execute(() -> {
                        try{
                            GmailDraftListDrafts result = multiThreadGmailService
                                    .multiThreadRequestGmailDraftGetForList(draft, gmailService);
                            future.complete(result);
                        }catch (Exception e){
                            log.error(REQUEST_GMAIL_USER_MESSAGES_GET_API_ERR_MSG);
                            future.completeExceptionally(new GmailException(REQUEST_GMAIL_USER_MESSAGES_GET_API_ERR_MSG));
                        }
                    });
                    return future;
                }).toList();
        return futures.stream().map((future) -> {
            try{
                return future.get();
            }catch (Exception e){
                log.error(e.getMessage());
                throw new GmailException(REQUEST_GMAIL_USER_MESSAGES_GET_API_ERR_MSG);
            }
        }).collect(Collectors.toList());
    }

    private void validatePaymentThread(List<GmailThreadListThreads> detailedThreads, LocalDate currentDate) {
        if(!detailedThreads.isEmpty()){
            // get first thread date
            GmailThreadListThreads firstThread = detailedThreads.get(0);
            GmailThreadGetMessagesResponse lastMessageInFirstThread = firstThread.getMessages().get(0);
            Long timeStamp = lastMessageInFirstThread.getTimestamp();
            // calc date 60 days ago
            Instant sixtyDaysAgoInstant = currentDate.minusDays(60).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant threadInstant = Instant.ofEpochMilli(timeStamp);

            if (threadInstant.isBefore(sixtyDaysAgoInstant)) {
                throw new CustomErrorException(ErrorCode.BILLING_ERROR_MESSAGE, ErrorCode.BILLING_ERROR_MESSAGE.getMessage());
            }
        }
    }

    private void validatePaymentDraft(List<GmailDraftListDrafts> detailedDrafts, LocalDate currentDate) {
        if(!detailedDrafts.isEmpty()){
            // get first thread date
            GmailDraftListDrafts firstDraft = detailedDrafts.get(0);
            GmailDraftGetMessageResponse draftMessage = firstDraft.getMessage(); // 분리 필요
            Long timeStamp = draftMessage.getTimestamp();
            // calc date 60 days ago
            Instant sixtyDaysAgoInstant = currentDate.minusDays(60).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant draftInstant = Instant.ofEpochMilli(timeStamp);

            if (draftInstant.isBefore(sixtyDaysAgoInstant)) {
                throw new CustomErrorException(ErrorCode.BILLING_ERROR_MESSAGE, ErrorCode.BILLING_ERROR_MESSAGE.getMessage());
            }
        }
    }

    private List<GmailThreadSearchListThreads> getSimpleThreads(List<Thread> threads){
        List<GmailThreadSearchListThreads> gmailThreadSearchListThreads = new ArrayList<>();
        threads.forEach((thread) ->{
            GmailThreadSearchListThreads gmailThreadSearchListThread = new GmailThreadSearchListThreads();
            gmailThreadSearchListThread.setId(thread.getId());
            gmailThreadSearchListThreads.add(gmailThreadSearchListThread);
        });
        return gmailThreadSearchListThreads;
    }

    private ListThreadsResponse getQueryListThreadsResponse(String pageToken, Long maxResults, String q, Gmail gmailService) {
        try{
            return gmailService.users().threads()
                    .list(USER_ID)
                    .setMaxResults(maxResults)
                    .setPageToken(pageToken)
                    .setPrettyPrint(Boolean.TRUE)
                    .setQ(q)
                    .execute();
        }catch (GoogleJsonResponseException e){
            switch (e.getStatusCode()) {
                case 401 ->
                        throw new CustomErrorException(ErrorCode.INVALID_ACCESS_TOKEN, ErrorCode.INVALID_ACCESS_TOKEN.getMessage());
                case 429 ->
                        throw new CustomErrorException(ErrorCode.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS.getMessage());
                case 400 -> {
                    if (e.getDetails().getMessage().contains("Invalid pageToken")) {
                        throw new CustomErrorException(ErrorCode.INVALID_NEXT_PAGE_TOKEN, ErrorCode.INVALID_NEXT_PAGE_TOKEN.getMessage());
                    }
                    throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE, ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE.getMessage());
                }
                default ->
                        throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE, ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE.getMessage());
            }
        }catch (IOException e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE, ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE.getMessage());
        }
    }

    private ListThreadsResponse getSearchListThreadsResponse(GmailSearchParams params, Gmail gmailService){
        String q = params.createQ();
        try{
             return gmailService.users().threads()
                    .list(USER_ID)
                    .setMaxResults(THREADS_LIST_MAX_LENGTH)
                    .setPrettyPrint(Boolean.TRUE)
                    .setQ(q)
                    .execute();
        }catch (IOException e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    private Thread getOneThreadResponse(String id, Gmail gmailService) {
        try{
            return gmailService.users().threads()
                    .get(USER_ID, id)
                    .setFormat(THREADS_GET_FULL_FORMAT)
                    .setPrettyPrint(Boolean.TRUE)
                    .execute();
        }catch (IOException e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREAD_GET_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_THREAD_GET_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    private ListDraftsResponse getListDraftsResponse(Gmail gmailService, String pageToken, Long maxResults, String q) throws IOException{
        return gmailService.users().drafts()
                .list(USER_ID)
                .setMaxResults(maxResults)
                .setPrettyPrint(Boolean.TRUE)
                .setPageToken(pageToken)
                .setQ(q)
                .execute();
    }

    private Draft getOneDraftResponse(String id, Gmail gmailService) throws IOException{
        return gmailService.users().drafts()
                .get(USER_ID, id)
                .setFormat(DRAFTS_GET_FULL_FORMAT)
                .setPrettyPrint(Boolean.TRUE)
                .execute();
    }

    // Methods : create something
    private MimeMessage createEmailWithAtt(GmailMessageSendRequestWithAtt request) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        // setting base
        email.setFrom(new InternetAddress(request.getFromEmailAddress()));
        // handling multiple recipients
        for (String recipient : request.getToEmailAddresses()) {
            email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(recipient));
        }
        // handling multiple CC recipients
        if (request.getCcEmailAddresses() != null) {
            for (String ccRecipient : request.getCcEmailAddresses()) {
                email.addRecipient(jakarta.mail.Message.RecipientType.CC, new InternetAddress(ccRecipient));
            }
        }
        // handling multiple BCC recipients
        if (request.getBccEmailAddresses() != null) {
            for (String bccRecipient : request.getBccEmailAddresses()) {
                email.addRecipient(jakarta.mail.Message.RecipientType.BCC, new InternetAddress(bccRecipient));
            }
        }
        email.setSubject(request.getSubject());
        // setting body
        Multipart multipart = new MimeMultipart();
        MimeBodyPart htmlPart = new MimeBodyPart();
        String bodyText = request.getBodyText();
        Document doc = Jsoup.parse(bodyText);
        Element body = doc.body();
        List<GmailMessageInlineImage> base64Images = new ArrayList<>();
        Pattern pattern = Pattern.compile("data:(.*?);base64,([^\"']*)");
        int cidNum = 0;
        for(Element element : body.children()){
            if(element.tagName().equals("img") && element.attr("src").startsWith("data:")){
                String src = element.attr("src");
                Matcher matcher = pattern.matcher(src);
                if (matcher.find()) {
                    String mimeType = matcher.group(1);
                    String base64Data = matcher.group(2);
                    byte[] imageData = java.util.Base64.getDecoder().decode(base64Data);
                    base64Images.add(new GmailMessageInlineImage(mimeType, imageData));
                }
                element.attr("src", "cid:image" + cidNum);
            }
        }
        htmlPart.setContent(body.toString(), "text/html");
        multipart.addBodyPart(htmlPart);

        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            for (int i = 0; i < request.getFiles().size(); i++) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setContent(request.getFiles().get(i), "application/octet-stream");
                attachmentPart.setFileName(request.getFileNames().get(i));
                multipart.addBodyPart(attachmentPart);
            }
        }

        for(int i = 0;i < base64Images.size();i++){
            GmailMessageInlineImage inlineFile = base64Images.get(i);
            MimeBodyPart imagePart = new MimeBodyPart();
            imagePart.setContent(inlineFile.getData(), inlineFile.getMimeType());
            imagePart.setFileName("image.png");
            imagePart.setContentID("<image" + i + ">");
            imagePart.setDisposition(MimeBodyPart.INLINE);
            multipart.addBodyPart(imagePart);
        }
        email.setContent(multipart);
        return email;
    }

    private void uploadEmailData(String accessToken, String uploadUrl, MimeMessage email) throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        RestTemplate restTemplate = new RestTemplate();
        email.writeTo(buffer);
        byte[] emailData = buffer.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("message/rfc822"));
        headers.setBearerAuth(accessToken);
        headers.setContentLength(emailData.length);

        HttpEntity<byte[]> entity = new HttpEntity<>(emailData, headers);
        restTemplate.exchange(uploadUrl, HttpMethod.PUT, entity, String.class);
    }

    private String initiateResumableSession(String accessToken) {
        String initiateUrl = String.format("https://www.googleapis.com/upload/gmail/v1/users/%s/messages/send?uploadType=resumable", USER_ID);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.add("X-Upload-Content-Type", "message/rfc822");

        HttpEntity<String> entity = new HttpEntity<>("", headers);
        String uploadUrl = restTemplate.exchange(initiateUrl, HttpMethod.POST, entity, String.class).getHeaders().getLocation().toString();

        return uploadUrl;
    }

    private MimeMessage createDraft(GmailDraftCommonRequest request) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        // setting base
        email.setFrom(new InternetAddress(request.getFromEmailAddress()));
        // handling multiple recipients
        for (String recipient : request.getToEmailAddresses()) {
            email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(recipient));
        }
        // handling multiple CC recipients
        if (request.getCcEmailAddresses() != null) {
            for (String ccRecipient : request.getCcEmailAddresses()) {
                email.addRecipient(jakarta.mail.Message.RecipientType.CC, new InternetAddress(ccRecipient));
            }
        }
        // handling multiple BCC recipients
        if (request.getBccEmailAddresses() != null) {
            for (String bccRecipient : request.getBccEmailAddresses()) {
                email.addRecipient(jakarta.mail.Message.RecipientType.BCC, new InternetAddress(bccRecipient));
            }
        }
        email.setSubject(request.getSubject());
        // setting body
        Multipart multipart = new MimeMultipart();
        MimeBodyPart htmlPart = new MimeBodyPart();
        String bodyText = request.getBodyText();
        Document doc = Jsoup.parse(bodyText);
        doc.outputSettings().charset("UTF-8");
        Element body = doc.body();
        List<GmailMessageInlineImage> base64Images = new ArrayList<>();
        Pattern pattern = Pattern.compile("data:(.*?);base64,([^\"']*)");
        int cidNum = 0;
        for(Element element : body.children()){
            if(element.tagName().equals("img") && element.attr("src").startsWith("data:")){
                String src = element.attr("src");
                Matcher matcher = pattern.matcher(src);
                if (matcher.find()) {
                    String mimeType = matcher.group(1);
                    String base64Data = matcher.group(2);
                    byte[] imageData = java.util.Base64.getDecoder().decode(base64Data);
                    base64Images.add(new GmailMessageInlineImage(mimeType, imageData));
                }
                element.attr("src", "cid:image" + cidNum);
            }
        }
        htmlPart.setContent(body.toString(), "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        for(File file : request.getFiles()){
            MimeBodyPart fileMimeBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(file);
            fileMimeBodyPart.setDataHandler(new DataHandler(source));
            fileMimeBodyPart.setFileName(file.getName());
            multipart.addBodyPart(fileMimeBodyPart);
        }

        for(int i = 0;i < base64Images.size();i++){
            GmailMessageInlineImage inlineFile = base64Images.get(i);
            MimeBodyPart imagePart = new MimeBodyPart();
            imagePart.setContent(inlineFile.getData(), inlineFile.getMimeType());
            imagePart.setFileName("image.png");
            imagePart.setContentID("<image" + i + ">");
            imagePart.setDisposition(MimeBodyPart.INLINE);
            multipart.addBodyPart(imagePart);
        }
        email.setContent(multipart);
        return email;
    }


    private Message createMessage(MimeMessage emailContent) throws MessagingException, IOException{
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private <T> List<T> isEmptyResult(List<T> list){
        if(list == null) return new ArrayList<>();
        return list;
    }

    private int getTotalCountThreads(Gmail gmailService, String label){
        try{
            Label result = gmailService.users().labels()
                    .get(USER_ID, label)
                    .execute();
            return result.getThreadsTotal();
        }catch (IOException e) {
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_LABELS_GET_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_LABELS_GET_API_ERROR_MESSAGE.getMessage()
            );
        }
    }

    private void getThreadsAttachments(MessagePart part, Map<String, GmailThreadListAttachments> attachments, Map<String, GmailThreadListInlineImages> inlineImages) {
        if(part.getParts() == null){ // base condition
            if(part.getFilename() != null && !part.getFilename().isBlank() && !GlobalUtility.isInlineFile(part)){
                MessagePartBody body = part.getBody();
                List<MessagePartHeader> headers = part.getHeaders();
                GmailThreadListAttachments attachment = GmailThreadListAttachments.builder().build();
                String contentId = "";
                for(MessagePartHeader header : headers){
                    if(header.getName().toUpperCase().equals(THREAD_PAYLOAD_HEADER_CONTENT_ID_KEY)){
                        contentId = header.getValue();
                        contentId = contentId.replace("<", "").replace(">", "");
                    }
                }
                attachment.setMimeType(part.getMimeType());
                attachment.setAttachmentId(body.getAttachmentId());
                attachment.setSize(body.getSize());
                attachment.setFileName(part.getFilename());
                if(!attachments.containsKey(contentId)){
                    attachments.put(contentId, attachment);
                }
            }else if(part.getFilename() != null && !part.getFilename().isBlank() && GlobalUtility.isInlineFile(part)){
                MessagePartBody body = part.getBody();
                List<MessagePartHeader> headers = part.getHeaders();
                GmailThreadListInlineImages inlineImage = GmailThreadListInlineImages.builder().build();
                String contentId = "";
                for(MessagePartHeader header : headers){
                    if(header.getName().toUpperCase().equals(THREAD_PAYLOAD_HEADER_CONTENT_ID_KEY)){
                        contentId = header.getValue();
                        contentId = contentId.replace("<", "").replace(">", "");
                    }
                }
                inlineImage.setMimeType(part.getMimeType());
                inlineImage.setAttachmentId(body.getAttachmentId());
                inlineImage.setSize(body.getSize());
                inlineImage.setFileName(part.getFilename());
                if(!inlineImages.containsKey(contentId)){
                    inlineImages.put(contentId, inlineImage);
                }
            }
        }else{ // recursion
            for(MessagePart subPart : part.getParts()){
                getThreadsAttachments(subPart, attachments, inlineImages);
            }
            if(part.getFilename() != null && !part.getFilename().isBlank() && !GlobalUtility.isInlineFile(part)){
                MessagePartBody body = part.getBody();
                List<MessagePartHeader> headers = part.getHeaders();
                GmailThreadListAttachments attachment = GmailThreadListAttachments.builder().build();
                String contentId = "";
                for(MessagePartHeader header : headers){
                    if(header.getName().toUpperCase().equals(THREAD_PAYLOAD_HEADER_CONTENT_ID_KEY)){
                        contentId = header.getValue();
                        contentId = contentId.replace("<", "").replace(">", "");
                    }
                }
                attachment.setMimeType(part.getMimeType());
                attachment.setAttachmentId(body.getAttachmentId());
                attachment.setSize(body.getSize());
                attachment.setFileName(part.getFilename());
                if(!attachments.containsKey(contentId)){
                    attachments.put(contentId, attachment);
                }
            }else if(part.getFilename() != null && !part.getFilename().isBlank() && GlobalUtility.isInlineFile(part)){
                MessagePartBody body = part.getBody();
                List<MessagePartHeader> headers = part.getHeaders();
                GmailThreadListInlineImages inlineImage = GmailThreadListInlineImages.builder().build();
                String contentId = "";
                for(MessagePartHeader header : headers){
                    if(header.getName().toUpperCase().equals(THREAD_PAYLOAD_HEADER_CONTENT_ID_KEY)){
                        contentId = header.getValue();
                        contentId = contentId.replace("<", "").replace(">", "");
                    }
                }
                inlineImage.setMimeType(part.getMimeType());
                inlineImage.setAttachmentId(body.getAttachmentId());
                inlineImage.setSize(body.getSize());
                inlineImage.setFileName(part.getFilename());
                if(!inlineImages.containsKey(contentId)){
                    inlineImages.put(contentId, inlineImage);
                }
            }
        }
    }

    private void addForwardingAddress(String forwardingEmailAddress, Gmail gmailService) throws IOException {
        ForwardingAddress forwardingAddress = new ForwardingAddress().setForwardingEmail(forwardingEmailAddress);
        gmailService.users().settings().forwardingAddresses().create(USER_ID, forwardingAddress).execute();
    }

    private void createFilter(String q, String forwardTo, Gmail gmailService) throws IOException {
        FilterCriteria filterCriteria = new FilterCriteria().setQuery(q);
        FilterAction filterAction = new FilterAction().setForward(forwardTo);
        Filter filter = new Filter().setCriteria(filterCriteria).setAction(filterAction);
        gmailService.users().settings().filters().create(USER_ID, filter).execute();
    }

    private GmailReferenceExtractionResponse extractReferences(Message message, Gmail gmailService) {
        try{
            // init reference extraction dto
            String messageId = message.getId();
            GmailReferenceExtractionResponse response = new GmailReferenceExtractionResponse();
            // get thread data
            String threadId = message.getThreadId();
            Thread thread = gmailService.users().threads()
                    .get(USER_ID, threadId)
                    .setFormat(THREADS_GET_METADATA_FORMAT)
                    .setPrettyPrint(Boolean.TRUE)
                    .execute();
            // extraction reference data
            List<Message> messages = thread.getMessages();
            Map<String, String> messageIdMapping = new HashMap<>();
            List<GmailReferenceExtraction> referenceExtractions = messages.stream().map((msg) -> {
                GmailReferenceExtraction gmailReferenceExtraction = new GmailReferenceExtraction();
                gmailReferenceExtraction.setGmailReferenceExtraction(msg, messageIdMapping);
                return gmailReferenceExtraction;
            }).toList();
            for(GmailReferenceExtraction referenceExtraction : referenceExtractions){
                if(referenceExtraction.getMessageId().equals(messageId)) {
                    referenceExtraction.convertMessageIdInReference(messageIdMapping);
                    response.setReferences(referenceExtraction.getReferences());
                }
            }
            return response;
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE,
                    ErrorCode.REQUEST_GMAIL_USER_THREADS_GET_API_ERROR_MESSAGE.getMessage()
            );
        }
    }
}