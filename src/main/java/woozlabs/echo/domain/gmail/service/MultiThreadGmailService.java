package woozlabs.echo.domain.gmail.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Thread;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import woozlabs.echo.domain.gmail.dto.draft.*;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesBcc;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesCc;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesFrom;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesResponse;
import woozlabs.echo.domain.gmail.dto.thread.*;
import woozlabs.echo.domain.gmail.exception.GmailException;
import woozlabs.echo.domain.gmail.util.GmailUtility;
import woozlabs.echo.global.utils.GlobalUtility;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static woozlabs.echo.global.constant.GlobalConstant.*;

@Service
@RequiredArgsConstructor
public class MultiThreadGmailService {
    private final String DRAFT_LABEL_ID = "DRAFT";
    public GmailThreadListThreads multiThreadRequestGmailThreadGetForList(Thread thread, Gmail gmailService, Map<String, String> messageIdMapping){
        try {
            // init
            String id = thread.getId();
            BigInteger historyId = thread.getHistoryId();
            GmailThreadListThreads gmailThreadListThreads = new GmailThreadListThreads();
            Thread detailedThread = gmailService.users().threads().get(USER_ID, id)
                    .setFormat(THREADS_GET_FULL_FORMAT)
                    .execute();
            List<Message> messages = detailedThread.getMessages();
            Map<String, GmailThreadListAttachments> attachments = new HashMap<>();
            List<GmailThreadGetMessagesResponse> convertedMessages = new ArrayList<>();
            for(int idx = 0;idx < messages.size();idx++){
                int idxForLambda = idx;
                Message message = messages.get(idx);
                MessagePart payload = message.getPayload();
                GmailThreadGetMessagesResponse convertedMessage = GmailThreadGetMessagesResponse.toGmailThreadGetMessages(message, messageIdMapping);
                convertedMessage.convertMessageIdInReference(messageIdMapping);
                List<MessagePartHeader> headers = payload.getHeaders(); // parsing header
                if(idxForLambda == messages.size()-1){
                    Long date = convertedMessage.getTimestamp();
                    gmailThreadListThreads.setSnippet(message.getSnippet());
                    gmailThreadListThreads.setTimestamp(date);
                }
                // get attachments
                getThreadsAttachments(payload, attachments);
                headers.forEach((header) -> {
                    String headerName = header.getName().toUpperCase();
                    // first message -> extraction subject
                    if (idxForLambda == 0 && headerName.equals(THREAD_PAYLOAD_HEADER_SUBJECT_KEY)) {
                        gmailThreadListThreads.setSubject(header.getValue());
                    }
                });
                // (Checking) is it draft in threads
                if(message.getLabelIds().contains(DRAFT_LABEL_ID)) continue;
                convertedMessages.add(convertedMessage);
            }
            gmailThreadListThreads.setId(id);
            gmailThreadListThreads.setHistoryId(historyId);
            gmailThreadListThreads.setMessages(convertedMessages);
            return gmailThreadListThreads;
        } catch (IOException e) {
            throw new GmailException(e.getMessage());
        }
    }

    public GmailDraftListDrafts multiThreadRequestGmailDraftGetForList(Draft draft, Gmail gmailService){
        try {
            // init
            String id = draft.getId();
            GmailDraftListDrafts gmailDraftListDrafts = new GmailDraftListDrafts();
            Draft detailedDraft = gmailService.users().drafts().get(USER_ID, id)
                    .setFormat(THREADS_GET_FULL_FORMAT)
                    .execute();
            Message message = detailedDraft.getMessage();
            Map<String, GmailThreadListAttachments> attachments = new HashMap<>();
            // 임시 추가
            Map<String, String> messageIdMapping = new HashMap<>();
            GmailDraftGetMessageResponse convertedMessage = GmailDraftGetMessageResponse.toGmailDraftGetMessage(message); // convert message to dto
            MessagePart payload = message.getPayload();
            List<MessagePartHeader> headers = payload.getHeaders(); // parsing header
            List<String> labelIds = message.getLabelIds();
            Long date = convertedMessage.getTimestamp();
            gmailDraftListDrafts.setSnippet(message.getSnippet());
            gmailDraftListDrafts.setTimestamp(date);
            getThreadsAttachments(payload, attachments);
            headers.forEach((header) -> {
                String headerName = header.getName().toUpperCase();
                // first message -> extraction subject
                if (headerName.equals(THREAD_PAYLOAD_HEADER_SUBJECT_KEY)) {
                    gmailDraftListDrafts.setSubject(header.getValue());
                }
            });
            List<GmailDraftGetMessagesFrom> froms = List.of(convertedMessage.getFrom());
            List<GmailDraftGetMessagesCc> ccs = convertedMessage.getCc();
            List<GmailDraftGetMessagesBcc> bccs = convertedMessage.getBcc();
            gmailDraftListDrafts.setLabelIds(labelIds.stream().distinct().collect(Collectors.toList()));
            gmailDraftListDrafts.setId(id);
            gmailDraftListDrafts.setFrom(froms.stream().distinct().toList());
            gmailDraftListDrafts.setCc(ccs.stream().distinct().toList());
            gmailDraftListDrafts.setBcc(bccs.stream().distinct().toList());
            gmailDraftListDrafts.setThreadSize(1);
            gmailDraftListDrafts.setAttachments(attachments);
            gmailDraftListDrafts.setAttachmentSize(attachments.size());
            gmailDraftListDrafts.setMessage(convertedMessage);
            //gmailThreadListThreads.setGoogleDriveAttachmentSize(googleDriveAttachments.size());
            //gmailThreadListThreads.setGoogleDriveAttachments(googleDriveAttachments.stream().distinct().toList());
            return gmailDraftListDrafts;
        } catch (IOException e) {
            throw new GmailException(e.getMessage());
        }
    }


    private void getThreadsAttachments(MessagePart part, Map<String, GmailThreadListAttachments> attachments) throws IOException {
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
            }
        }else{ // recursion
            for(MessagePart subPart : part.getParts()){
                getThreadsAttachments(subPart, attachments);
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
            }
        }
    }

    private List<String> getGoogleDriveAttachments(Message message) throws UnsupportedEncodingException {
        List<String> googleDriveAttachmentsInMessage = new ArrayList<>();
        if(message.getPayload().getParts() == null){
            return googleDriveAttachmentsInMessage;
        }
        for (MessagePart part : message.getPayload().getParts()) {
            if ("text/html".equals(part.getMimeType())) {
                String standardBase64 = part.getBody().getData()
                        .replace('-', '+')
                        .replace('_', '/');
                // Add padding if necessary
                int paddingCount = (4 - (standardBase64.length() % 4)) % 4;
                for (int i = 0; i < paddingCount; i++) {
                    standardBase64 += "=";
                }
                byte[] decodedBinaryContent = java.util.Base64.getDecoder().decode(standardBase64);
                String decodedData = new String(decodedBinaryContent, StandardCharsets.UTF_8);
                Pattern pattern = Pattern.compile("https://docs\\.google\\.com[^\\s]*");
                Matcher matcher = pattern.matcher(decodedData);
                while (matcher.find()) {
                    googleDriveAttachmentsInMessage.add(matcher.group());
                }
            }
        }
        return googleDriveAttachmentsInMessage;
    }

//    private void getDraftsAttachments(MessagePart part, List<GmailDraftListAttachments> attachments) throws IOException {
//        if(part.getParts() == null){ // base condition
//            if(part.getFilename() != null && !part.getFilename().isBlank() && !GlobalUtility.isInlineFile(part)){
//                MessagePartBody body = part.getBody();
//                List<MessagePartHeader> headers = part.getHeaders();
//                GmailDraftListAttachments attachment = GmailDraftListAttachments.builder().build();
//                for(MessagePartHeader header : headers){
//                    if(header.getName().toUpperCase().equals(THREAD_PAYLOAD_HEADER_CONTENT_ID_KEY)){
//                        String contentId = header.getValue();
//                        contentId = contentId.replace("<", "").replace(">", "");
//                        attachment.setContentId(contentId);
//                    }
//                }
//                attachment.setMimeType(part.getMimeType());
//                attachment.setAttachmentId(body.getAttachmentId());
//                attachment.setSize(body.getSize());
//                attachment.setFileName(part.getFilename());
//                if(!attachments.contains(attachment)){
//                    attachments.add(attachment);
//                }
//            }
//        }else{ // recursion
//            for(MessagePart subPart : part.getParts()){
//                getDraftsAttachments(subPart, attachments);
//            }
//            if(part.getFilename() != null && !part.getFilename().isBlank() && !GlobalUtility.isInlineFile(part)){
//                MessagePartBody body = part.getBody();
//                List<MessagePartHeader> headers = part.getHeaders();
//                GmailDraftListAttachments attachment = GmailDraftListAttachments.builder().build();
//                for(MessagePartHeader header : headers){
//                    if(header.getName().toUpperCase().equals(THREAD_PAYLOAD_HEADER_CONTENT_ID_KEY)){
//                        String contentId = header.getValue();
//                        contentId = contentId.replace("<", "").replace(">", "");
//                        attachment.setContentId(contentId);
//                    }
//                }
//                attachment.setMimeType(part.getMimeType());
//                attachment.setAttachmentId(body.getAttachmentId());
//                attachment.setSize(body.getSize());
//                attachment.setFileName(part.getFilename());
//                if(!attachments.contains(attachment)){
//                    attachments.add(attachment);
//                }
//            }
//        }
//    }
}
