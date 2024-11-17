package woozlabs.echo.domain.gmail.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import woozlabs.echo.domain.gmail.dto.autoForwarding.AutoForwardingResponse;
import woozlabs.echo.domain.gmail.dto.draft.*;
import woozlabs.echo.domain.gmail.dto.history.GmailHistoryListResponse;
import woozlabs.echo.domain.gmail.dto.message.*;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadTotalCountResponse;
import woozlabs.echo.domain.gmail.dto.pubsub.PubSubWatchRequest;
import woozlabs.echo.domain.gmail.dto.pubsub.PubSubWatchResponse;
import woozlabs.echo.domain.gmail.dto.thread.*;
import woozlabs.echo.domain.gmail.service.GmailService;
import woozlabs.echo.domain.gmail.util.GmailUtility;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.global.dto.ResponseDto;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GmailController {
    private final GmailService gmailService;
    private final GmailUtility gmailUtility;
    // threads
    @GetMapping("/api/v1/gmail/threads")
    public ResponseEntity<ResponseDto> getQueryThreads(HttpServletRequest httpServletRequest,
                                                       @RequestParam(value = "pageToken", required = false) String pageToken,
                                                       @RequestParam(value = "maxResults", required = false, defaultValue = "50") Long maxResults,
                                                       @RequestParam(value = "q") String q,
                                                       @RequestParam("aAUid") String aAUid){
        log.info("Request to get threads");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailThreadListResponse response = gmailService.getQueryUserEmailThreads(accessToken, pageToken, maxResults, q, aAUid);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/v1/gmail/threads/search")
    public ResponseEntity<ResponseDto> searchThreads(@RequestParam(value = "from", required = false) String from,
                                                     @RequestParam(value = "to", required = false) String to,
                                                     @RequestParam(value = "subject", required = false) String subject,
                                                     @RequestParam(value = "q", required = false) String query, HttpServletRequest httpServletRequest,
                                                     @RequestParam("aAUid") String aAUid){
        log.info("Request to search threads");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailSearchParams params = GmailSearchParams.builder()
                .from(from).to(to).subject(subject).query(query).build();
        GmailThreadSearchListResponse response = gmailService.searchUserEmailThreads(accessToken, params);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/v1/gmail/threads/{id}")
    public ResponseEntity<ResponseDto> getThread(HttpServletRequest httpServletRequest, @PathVariable("id") String id,
                                                 @RequestParam("aAUid") String aAUid){
        log.info("Request to get thread");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailThreadGetResponse response = gmailService.getUserEmailThread(accessToken, id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/api/v1/gmail/threads/{id}/trash")
    public ResponseEntity<ResponseDto> trashThread(HttpServletRequest httpServletRequest, @PathVariable("id") String id,
                                                   @RequestParam("aAUid") String aAUid){
        log.info("Request to trash thread");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailThreadTrashResponse response = gmailService.trashUserEmailThread(accessToken, id);
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/api/v1/gmail/threads/{id}")
    public ResponseEntity<ResponseDto> deleteThread(HttpServletRequest httpServletRequest, @PathVariable("id") String id,
                                                    @RequestParam("aAUid") String aAUid){
        log.info("Request to delete thread");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailThreadDeleteResponse response = gmailService.deleteUserEmailThread(accessToken, id);
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/api/v1/gmail/threads/{id}/modify")
    public ResponseEntity<ResponseDto> updateThread(HttpServletRequest httpServletRequest,
                                                    @PathVariable("id") String id,
                                                    @RequestBody GmailThreadUpdateRequest request,
                                                    @RequestParam("aAUid") String aAUid){
        log.info("Request to update thread");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailThreadUpdateResponse response = gmailService.updateUserEmailThread(accessToken, id, request);
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

    @GetMapping("/api/v1/gmail/threads/count")
    public ResponseEntity<ResponseDto> getThreadsTotalCount(HttpServletRequest httpServletRequest,
                                                            @RequestParam("label") String label,
                                                            @RequestParam("aAUid") String aAUid){
        log.info("Request to get total count of messages");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailThreadTotalCountResponse response = gmailService.getUserEmailThreadsTotalCount(accessToken, label);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // messages
    @GetMapping("/api/v1/gmail/messages/{messageId}")
    public ResponseEntity<?> getMessage(HttpServletRequest httpServletRequest,
                                                  @PathVariable("messageId") String messageId,
                                                  @RequestParam("aAUid") String aAUid){
        log.info("Request to get message({})", messageId);
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailMessageGetResponse response = gmailService.getUserEmailMessage(accessToken, messageId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/api/v1/gmail/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(HttpServletRequest httpServletRequest,
                                           @PathVariable("messageId") String messageId,
                                           @RequestParam("aAUid") String aAUid){
        log.info("Request to delete message");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        gmailService.deleteMessage(accessToken, messageId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/api/v1/gmail/messages/{messageId}/modify")
    public ResponseEntity<?> updateMessage(HttpServletRequest httpServletRequest,
                                           @PathVariable("messageId") String messageId,
                                           @RequestBody GmailMessageUpdateRequest request,
                                           @RequestParam("aAUid") String aAUid){
        log.info("Request to update message");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailMessageUpdateResponse response = gmailService.updateUserEmailMessage(accessToken, messageId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/v1/gmail/messages/{messageId}/attachments/{id}")
    public ResponseEntity<?> getAttachment(HttpServletRequest httpServletRequest,
                                                     @PathVariable("messageId") String messageId, @PathVariable("id") String id,
                                                @RequestParam("aAUid") String aAUid){
        log.info("Request to get attachment in message");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailMessageAttachmentResponse response = gmailService.getAttachment(accessToken, messageId, id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/v1/gmail/messages/{messageId}/attachments/{id}/download")
    public ResponseEntity<?> downloadAttachment(HttpServletRequest httpServletRequest,
                                                @PathVariable("messageId") String messageId, @PathVariable("id") String attachmentId,
                                                @RequestParam("fileName") String fileName,
                                                @RequestParam("aAUid") String aAUid){
        log.info("Request to download attachment in message");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailMessageAttachmentDownloadResponse response = gmailService.downloadAttachment(accessToken, messageId, attachmentId);
        HttpHeaders headers = new HttpHeaders(); // set response header
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(response.getByteData().length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(response.getByteData());
    }

    @PostMapping("/api/v1/gmail/messages/send")
    public ResponseEntity<?> sendMessageWithAttachment(HttpServletRequest httpServletRequest,
                                         @RequestParam("aAUid") String aAUid, @RequestBody GmailMessageSendRequestByWebHook request){
        log.info("Request to send message");
        try{
            String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
            GmailMessageLazySendRequest lazySendRequest = gmailService.getLazySendRequestDto(request);
            List<byte[]> attachmentsData = lazySendRequest.decodeFiles();
            List<String> fileNames = lazySendRequest.getOriginalFileNames();
            String toEmailAddresses = lazySendRequest.getToEmailAddresses();
            String ccEmailAddresses = lazySendRequest.getCcEmailAddresses();
            String bccEmailAddresses = lazySendRequest.getBccEmailAddresses();
            String subject = lazySendRequest.getSubject();
            String bodyText = lazySendRequest.getBody();
            String type = lazySendRequest.getType();
            String messageId = lazySendRequest.getMessageId();
            // request dto setting
            GmailMessageSendRequestWithAtt gmailMessageSendRequestWithAtt = GmailMessageSendRequestWithAtt.builder()
                    .toEmailAddresses(Arrays.asList(toEmailAddresses.split(",")))
                    .ccEmailAddresses(ccEmailAddresses != null ? Arrays.asList(ccEmailAddresses.split(",")) : new ArrayList<>())
                    .bccEmailAddresses(bccEmailAddresses != null ? Arrays.asList(bccEmailAddresses.split(",")) : new ArrayList<>())
                    .subject(subject)
                    .bodyText(bodyText)
                    .sendType(type)
                    .messageId(messageId)
                    .build();
            gmailMessageSendRequestWithAtt.setFiles(attachmentsData);
            gmailMessageSendRequestWithAtt.setFileNames(fileNames);
            // main logic
            if(type.equals(SendType.NORMAL.getValue())){
                gmailService.sendUserEmailMessageWithAtt(accessToken, gmailMessageSendRequestWithAtt);
            }else if(type.equals(SendType.REPLY.getValue())){
                gmailService.sendEmailReply(accessToken, gmailMessageSendRequestWithAtt);
            }else if(type.equals(SendType.FORWARD.getValue())){
                gmailService.sendEmailForwarding(accessToken, gmailMessageSendRequestWithAtt);
            }else{
                throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE, "Invalid send type");
            }
            return new ResponseEntity<>(HttpStatus.CREATED);
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE, e.getMessage());
        }
    }

    @PostMapping("/api/v1/gmail/messages/send/cancel")
    public ResponseEntity<?> cancelSendMessage(HttpServletRequest httpServletRequest,
                                                    @RequestParam("taskId") String taskId,
                                                    @RequestParam("aAUid") String aAUid) {
        log.info("Request to cancel send message");
        gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        gmailService.cancelSendMessage(aAUid, taskId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/api/v1/gmail/messages/send/lazy")
    public ResponseEntity<?> sendLazyUserEmailMessage(HttpServletRequest httpServletRequest,
                                         @RequestParam("mailto") String toEmailAddresses,
                                         @RequestParam(value = "cc", required = false) String ccEmailAddresses,
                                         @RequestParam(value = "bcc", required = false) String bccEmailAddresses,
                                         @RequestParam("subject") String subject,
                                         @RequestParam("body") String bodyText,
                                         @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                         @RequestParam("aAUid") String aAUid,
                                         @RequestParam(value = "type", required = false) String type,
                                         @RequestParam(value = "messageId", required = false) String messageId) {
        log.info("Request to lazy-send message");
        if(files == null) files = new ArrayList<>();
        if(type == null) type = SendType.NORMAL.getValue();
        List<String> fileNames = new ArrayList<>();
        for(MultipartFile multipartFile : files){
            // check exceed maximum
            if(multipartFile.getSize() > 25 * 1000 * 1000) throw new CustomErrorException(ErrorCode.EXCEED_ATTACHMENT_FILE_SIZE);
            fileNames.add(multipartFile.getOriginalFilename()); // add attachment file name
        }
        // request dto setting
        GmailMessageLazySendRequest request = GmailMessageLazySendRequest.builder()
                .toEmailAddresses(toEmailAddresses)
                .ccEmailAddresses(ccEmailAddresses)
                .bccEmailAddresses(bccEmailAddresses)
                .subject(subject)
                .body(bodyText)
                .files(files)
                .type(type)
                .messageId(messageId)
                .originalFileNames(fileNames)
                .build();
        request.encodeFiles(); // encoding files
        // request send message to cloud task
        GmailMessageLazySendResponse response = gmailService.sendMessageWithCloudTask(httpServletRequest, request, aAUid);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/api/v1/gmail/messages/send/schedule")
    public ResponseEntity<?> sendScheduleEmail(HttpServletRequest httpServletRequest,
                                               @RequestParam("mailto") String toEmailAddresses,
                                               @RequestParam(value = "cc", required = false) String ccEmailAddresses,
                                               @RequestParam(value = "bcc", required = false) String bccEmailAddresses,
                                               @RequestParam("subject") String subject,
                                               @RequestParam("body") String bodyText,
                                               @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                               @RequestParam("aAUid") String aAUid,
                                               @RequestParam(value = "type", required = false) String type,
                                               @RequestParam(value = "messageId", required = false) String messageId,
                                               @RequestParam(value = "scheduleTime", required = false) String scheduleTime) {
        log.info("Request to schedule send message");
        try{
            String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
            List<byte[]> attachmentsData = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            if(files == null) files = new ArrayList<>();
            // validation file size
            for(MultipartFile multipartFile : files){
                // check exceed maximum
                if(multipartFile.getSize() > 25 * 1000 * 1000) throw new CustomErrorException(ErrorCode.EXCEED_ATTACHMENT_FILE_SIZE);
                attachmentsData.add(multipartFile.getBytes()); // add attachment data
                fileNames.add(multipartFile.getOriginalFilename()); // add attachment file name
            }
            // request dto setting
            GmailMessageSendRequestWithAtt request = GmailMessageSendRequestWithAtt.builder()
                    .toEmailAddresses(Arrays.asList(toEmailAddresses.split(",")))
                    .ccEmailAddresses(ccEmailAddresses != null ? Arrays.asList(ccEmailAddresses.split(",")) : new ArrayList<>())
                    .bccEmailAddresses(bccEmailAddresses != null ? Arrays.asList(bccEmailAddresses.split(",")) : new ArrayList<>())
                    .subject(subject)
                    .bodyText(bodyText)
                    .sendType(type)
                    .messageId(messageId)
                    .build();
            request.setFiles(attachmentsData);
            request.setFileNames(fileNames);
            // main logic
            if(type.equals(SendType.NORMAL.getValue())){
                gmailService.sendUserEmailMessageWithAtt(accessToken, request);
            }else if(type.equals(SendType.REPLY.getValue())){
                gmailService.sendEmailReply(accessToken, request);
            }else if(type.equals(SendType.FORWARD.getValue())){
                gmailService.sendEmailForwarding(accessToken, request);
            }else{
                throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE, "Invalid send type");
            }
            return new ResponseEntity<>(HttpStatus.CREATED);
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE, e.getMessage());
        }
    }

    @GetMapping("/api/v1/gmail/drafts")
    public ResponseEntity<?> getDrafts(HttpServletRequest httpServletRequest,
                                                    @RequestParam(value = "pageToken", required = false) String pageToken,
                                                    @RequestParam(value = "maxResults", required = false, defaultValue = "50") Long maxResults,
                                                    @RequestParam(value = "q", required = false) String q,
                                                    @RequestParam("aAUid") String aAUid){
        log.info("Request to get drafts");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailDraftListResponse response = gmailService.getUserEmailDrafts(accessToken, pageToken, maxResults, q);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/v1/gmail/drafts/{draftId}")
    public ResponseEntity<?> getDraft(HttpServletRequest httpServletRequest,
                                      @RequestParam("aAUid") String aAUid,
                                      @PathVariable("draftId") String draftId){
        log.info("Request to get draft");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailDraftGetResponse response = gmailService.getUserEmailDraft(accessToken, draftId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/v1/gmail/drafts/create")
    public ResponseEntity<?> createDraft(HttpServletRequest httpServletRequest,
                                                   @RequestParam(value = "mailto", required = false) String toEmailAddresses,
                                                   @RequestParam(value = "cc", required = false) String ccEmailAddresses,
                                                   @RequestParam(value = "bcc", required = false) String bccEmailAddresses,
                                                   @RequestParam(value = "subject", required = false) String subject,
                                                   @RequestParam(value = "body", required = false) String bodyText,
                                                   @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                                   @RequestParam("aAUid") String aAUid,
                                                   @RequestParam(value = "threadId", required = false) String threadId){
        log.info("Request to create draft");
        try{
            List<File> attachments = new ArrayList<>();
            String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
            GmailDraftCommonRequest request = new GmailDraftCommonRequest();
            List<String> emailList = Arrays.asList(toEmailAddresses.split(","));
            List<String> ccList = ccEmailAddresses != null ?
                    Arrays.asList(ccEmailAddresses.split(",")) :
                    new ArrayList<>();
            List<String> bccList = bccEmailAddresses != null ?
                    Arrays.asList(bccEmailAddresses.split(",")):
                    new ArrayList<>();
            request.setToEmailAddresses(emailList);
            request.setCcEmailAddresses(ccList);
            request.setBccEmailAddresses(bccList);
            request.setSubject(subject);
            request.setBodyText(bodyText);
            if(files == null) files = new ArrayList<>();
            for(MultipartFile multipartFile : files){
                // check exceed maximum
                if(multipartFile.getSize() > 25 * 1000 * 1000) throw new CustomErrorException(ErrorCode.EXCEED_ATTACHMENT_FILE_SIZE);
                File tmpFile = gmailUtility.convertMultipartFileToTempFile(multipartFile);
                attachments.add(tmpFile);
            }
            request.setFiles(attachments);
            request.setThreadId(threadId);
            GmailDraftCreateResponse response = gmailService.createUserEmailDraft(accessToken, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_DRAFTS_SEND_API_ERROR_MESSAGE, e.getMessage());
        }
    }

    @PutMapping("/api/v1/gmail/drafts/{id}")
    public ResponseEntity<?> modifyDraft(HttpServletRequest httpServletRequest,
                                                   @PathVariable("id") String id,
                                                   @RequestParam("mailto") String toEmailAddresses,
                                                   @RequestParam(value = "cc", required = false) String ccEmailAddresses,
                                                   @RequestParam(value = "bcc", required = false) String bccEmailAddresses,
                                                   @RequestParam("subject") String subject,
                                                   @RequestParam("body") String bodyText,
                                                   @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                                   @RequestParam("aAUid") String aAUid){
        log.info("Request to modify draft");
        try{
            List<File> attachments = new ArrayList<>();
            String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
            GmailDraftCommonRequest request = new GmailDraftCommonRequest();
            List<String> emailList = Arrays.asList(toEmailAddresses.split(","));
            request.setToEmailAddresses(emailList);
            List<String> ccList = ccEmailAddresses != null ?
                    Arrays.asList(ccEmailAddresses.split(",")) :
                    new ArrayList<>();
            List<String> bccList = bccEmailAddresses != null ?
                    Arrays.asList(bccEmailAddresses.split(",")):
                    new ArrayList<>();
            request.setSubject(subject);
            request.setCcEmailAddresses(ccList);
            request.setBccEmailAddresses(bccList);
            request.setBodyText(bodyText);
            if(files == null) files = new ArrayList<>();
            for(MultipartFile multipartFile : files){
                // check exceed maximum
                if(multipartFile.getSize() > 25 * 1000 * 1000) throw new CustomErrorException(ErrorCode.EXCEED_ATTACHMENT_FILE_SIZE);
                File tmpFile = gmailUtility.convertMultipartFileToTempFile(multipartFile);
                attachments.add(tmpFile);
            }
            request.setFiles(attachments);
            GmailDraftUpdateResponse response = gmailService.updateUserEmailDraft(accessToken, id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_DRAFTS_MODIFY_API_ERROR_MESSAGE, e.getMessage());
        }
    }

    @PostMapping(value = "/api/v1/gmail/drafts/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendDraft(HttpServletRequest httpServletRequest,
                                                   @RequestParam("mailto") String toEmailAddresses,
                                                   @RequestParam("subject") String subject,
                                                   @RequestParam(value = "cc", required = false) String ccEmailAddresses,
                                                   @RequestParam(value = "bcc", required = false) String bccEmailAddresses,
                                                   @RequestParam("body") String bodyText,
                                                   @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                                   @RequestParam("aAUid") String aAUid) {
        log.info("Request to send draft");
        try {
            List<File> attachments = new ArrayList<>();
            String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
            GmailDraftCommonRequest request = new GmailDraftCommonRequest();
            List<String> emailList = Arrays.asList(toEmailAddresses.split(","));
            List<String> ccList = ccEmailAddresses != null ?
                    Arrays.asList(ccEmailAddresses.split(",")) :
                    new ArrayList<>();
            List<String> bccList = bccEmailAddresses != null ?
                    Arrays.asList(bccEmailAddresses.split(",")):
                    new ArrayList<>();
            request.setToEmailAddresses(emailList);
            request.setCcEmailAddresses(ccList);
            request.setBccEmailAddresses(bccList);
            request.setSubject(subject);
            request.setBodyText(bodyText);
            if(files == null) files = new ArrayList<>();
            for(MultipartFile multipartFile : files){
                // check exceed maximum
                if(multipartFile.getSize() > 25 * 1000 * 1000) throw new CustomErrorException(ErrorCode.EXCEED_ATTACHMENT_FILE_SIZE);
                File tmpFile = gmailUtility.convertMultipartFileToTempFile(multipartFile);
                attachments.add(tmpFile);
            }
            request.setFiles(attachments);
            GmailDraftSendResponse response = gmailService.sendUserEmailDraft(accessToken, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (Exception e){
            throw new CustomErrorException(ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE, ErrorCode.REQUEST_GMAIL_USER_MESSAGES_SEND_API_ERROR_MESSAGE.getMessage());
        }
    }

    @DeleteMapping("/api/v1/gmail/drafts/{id}")
    public ResponseEntity<?> deleteDraft(HttpServletRequest httpServletRequest, @PathVariable("id") String id,
                                         @RequestParam("aAUid") String aAUid) {
        log.info("Request to delete draft");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        gmailService.deleteDraft(accessToken, id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/api/v1/gmail/watch")
    public ResponseEntity<?> postWatch(HttpServletRequest httpServletRequest, @RequestBody PubSubWatchRequest request,
                                       @RequestParam("aAUid") String aAUid){
        log.info("Request to watch pub/sub");
        Account activeAccount = gmailUtility.getActiveAccount(httpServletRequest, aAUid);
        PubSubWatchResponse response = gmailService.subscribePubSub(activeAccount, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/v1/gmail/stop")
    public ResponseEntity<?> getStop(HttpServletRequest httpServletRequest,
                                     @RequestParam(value = "aAUid", required = false) String aAUid){
        log.info("Request to stop pub/sub");
        String uid = (String) httpServletRequest.getAttribute("uid");
        gmailService.stopPubSub(uid, aAUid);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/api/v1/gmail/histories")
    public ResponseEntity<?> getHistories(HttpServletRequest httpServletRequest,
                                                    @RequestParam("historyId") String historyId,
                                                    @RequestParam(value = "pageToken", required = false) String pageToken,
                                          @RequestParam("aAUid") String aAUid){
        log.info("Request to get histories from {}", historyId);
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        GmailHistoryListResponse response = gmailService.getHistories(accessToken, historyId, pageToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/v1/gmail/auto-forwarding")
    public ResponseEntity<?> setAutoForwarding(HttpServletRequest httpServletRequest, @RequestParam("q") String q, @RequestParam("email") String email,
                                               @RequestParam("aAUid") String aAUid){
        log.info("Request to set auto forwarding");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        AutoForwardingResponse response = gmailService.setUpAutoForwarding(accessToken, q, email);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/v1/gmail/gen-verification-label")
    public ResponseEntity<?> generateVerificationLabel(HttpServletRequest httpServletRequest, @RequestParam("aAUid") String aAUid){
        log.info("Request to generate verification label");
        String accessToken = gmailUtility.getActiveAccountAccessToken(httpServletRequest, aAUid);
        gmailService.generateVerificationLabel(accessToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}