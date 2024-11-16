package woozlabs.echo.domain.gmail.dto.draft;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.Data;
import woozlabs.echo.global.utils.GlobalUtility;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static woozlabs.echo.global.constant.GlobalConstant.*;
import static woozlabs.echo.global.utils.GlobalUtility.splitCcAndBcc;
import static woozlabs.echo.global.utils.GlobalUtility.splitSenderData;

@Data
public class GmailDraftGetMessageResponse {
    private String id;
    private String subject;
    private Long timestamp;
    private String timezone = ""; // timezone
    private GmailDraftGetMessagesFrom from;
    private List<GmailDraftGetMessagesCc> cc = new ArrayList<>();
    private List<GmailDraftGetMessagesBcc> bcc = new ArrayList<>();
    private List<GmailDraftGetMessagesTo> to = new ArrayList<>();
    private String threadId; // thread id
    private List<String> labelIds;
    private List<String> references = new ArrayList<>();
    private String snippet;
    private BigInteger historyId;
    private GmailDraftGetPayload payload;
    private Map<String, GmailDraftListAttachments> attachments;
    private Map<String, GmailDraftListInlineImages> inlineImages;

    public static GmailDraftGetMessageResponse toGmailDraftGetMessage(Message message) {
        GmailDraftGetMessageResponse gmailDraftGetMessage = new GmailDraftGetMessageResponse();
        MessagePart payload = message.getPayload();
        GmailDraftGetPayload convertedPayload = new GmailDraftGetPayload(payload);
        List<MessagePartHeader> headers = payload.getHeaders(); // parsing header
        Map<String, GmailDraftListAttachments> attachments = new HashMap<>();
        Map<String, GmailDraftListInlineImages> inlineImages = new HashMap<>();
        getDraftsAttachments(payload, attachments, inlineImages);
        for(MessagePartHeader header: headers) {
            switch (header.getName().toUpperCase()) {
                case THREAD_PAYLOAD_HEADER_FROM_KEY -> {
                    String sender = header.getValue();
                    List<String> splitSender = splitSenderData(sender);
                    if (splitSender.size() == 2) {
                        gmailDraftGetMessage.setFrom(GmailDraftGetMessagesFrom.builder()
                                .name(splitSender.get(0))
                                .email(splitSender.get(1))
                                .build()
                        );
                    } else {
                        gmailDraftGetMessage.setFrom(GmailDraftGetMessagesFrom.builder()
                                .name(header.getValue())
                                .email(header.getValue())
                                .build()
                        );
                    }
                }case THREAD_PAYLOAD_HEADER_CC_KEY -> {
                    String oneCc = header.getValue();
                    List<List<String>> splitSender = splitCcAndBcc(oneCc);
                    if (!splitSender.isEmpty()){
                        List<GmailDraftGetMessagesCc> data = splitSender.stream().map((ss) -> {
                            GmailDraftGetMessagesCc gmailDraftGetMessagesCc = new GmailDraftGetMessagesCc();
                            gmailDraftGetMessagesCc.setName(ss.get(0));
                            gmailDraftGetMessagesCc.setEmail(ss.get(1));
                            return gmailDraftGetMessagesCc;
                        }).toList();
                        gmailDraftGetMessage.setCc(data);
                    }
                }case THREAD_PAYLOAD_HEADER_BCC_KEY -> {
                    String oneBcc = header.getValue();
                    List<List<String>> splitSender = splitCcAndBcc(oneBcc);
                    if(!splitSender.isEmpty()){
                        List<GmailDraftGetMessagesBcc> data = splitSender.stream().map((ss) -> {
                            GmailDraftGetMessagesBcc gmailDraftGetMessagesBcc = new GmailDraftGetMessagesBcc();
                            gmailDraftGetMessagesBcc.setName(ss.get(0));
                            gmailDraftGetMessagesBcc.setEmail(ss.get(1));
                            return gmailDraftGetMessagesBcc;
                        }).toList();
                        gmailDraftGetMessage.setBcc(data);
                    }
                }case THREAD_PAYLOAD_HEADER_TO_KEY -> {
                    String oneTo = header.getValue();
                    List<List<String>> splitSender = splitCcAndBcc(oneTo);
                    if (!splitSender.isEmpty()) {
                        List<GmailDraftGetMessagesTo> data = splitSender.stream().map((ss) -> {
                            GmailDraftGetMessagesTo gmailDraftGetMessagesTo = new GmailDraftGetMessagesTo();
                            gmailDraftGetMessagesTo.setName(ss.get(0));
                            gmailDraftGetMessagesTo.setEmail(ss.get(1));
                            return gmailDraftGetMessagesTo;
                        }).toList();
                        gmailDraftGetMessage.setTo(data);
                    }
                }case MESSAGE_PAYLOAD_HEADER_DATE_KEY -> {
                    String timestamp = header.getValue();
                    extractAndSetDateTime(timestamp, gmailDraftGetMessage);
                }case MESSAGE_PAYLOAD_HEADER_SUBJECT_KEY -> {
                    String subject = header.getValue();
                    gmailDraftGetMessage.setSubject(subject);
                }
            }
        }
        gmailDraftGetMessage.setTimestamp(message.getInternalDate());
        gmailDraftGetMessage.setId(message.getId());
        gmailDraftGetMessage.setThreadId(message.getThreadId());
        gmailDraftGetMessage.setLabelIds(message.getLabelIds());
        gmailDraftGetMessage.setSnippet(message.getSnippet());
        gmailDraftGetMessage.setHistoryId(message.getHistoryId());
        gmailDraftGetMessage.setPayload(convertedPayload);
        gmailDraftGetMessage.setAttachments(attachments);
        gmailDraftGetMessage.setInlineImages(inlineImages);
        return gmailDraftGetMessage;
    }

    private static void extractAndSetDateTime(String date, GmailDraftGetMessageResponse gmailDraftGetMessage) {
        List<Pattern> patterns = List.of(
                Pattern.compile("([+-]\\d{4})$"),
                Pattern.compile("\\(([A-Z]{3,4})\\)$"),
                Pattern.compile("([A-Z]{3,4})$")
        );
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(date);
            if (matcher.find()) {
                String timezonePart = matcher.group(1);
                if(!pattern.pattern().equals(Pattern.compile("([+-]\\d{4})$").pattern())){
                    timezonePart = GlobalUtility.getStandardTimeZone(timezonePart);
                }
                convertToIanaTimezone(gmailDraftGetMessage, timezonePart);
                break;
            }
        }
    }

    private static void convertToIanaTimezone(GmailDraftGetMessageResponse gmailDraftGetMessage, String timezonePart) {
        try {
            ZoneOffset offset = ZoneOffset.of(timezonePart);
            for (String zoneId : ZoneOffset.getAvailableZoneIds()) {
                ZoneId zone = ZoneId.of(zoneId);
                if (zone.getRules().getOffset(Instant.now()).equals(offset)) {
                    gmailDraftGetMessage.setTimezone(zoneId);
                    break;
                }
            }
        } catch (Exception e) {
            gmailDraftGetMessage.setTimezone(null);
        }
    }

    private static void getDraftsAttachments(MessagePart part, Map<String, GmailDraftListAttachments> attachments, Map<String, GmailDraftListInlineImages> inlineImages) {
        if(part.getParts() == null){ // base condition
            if(part.getFilename() != null && !part.getFilename().isBlank() && !GlobalUtility.isInlineFile(part)){
                MessagePartBody body = part.getBody();
                List<MessagePartHeader> headers = part.getHeaders();
                GmailDraftListAttachments attachment = GmailDraftListAttachments.builder().build();
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
                GmailDraftListInlineImages inlineImage = GmailDraftListInlineImages.builder().build();
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
                getDraftsAttachments(subPart, attachments, inlineImages);
            }
            if(part.getFilename() != null && !part.getFilename().isBlank() && !GlobalUtility.isInlineFile(part)){
                MessagePartBody body = part.getBody();
                List<MessagePartHeader> headers = part.getHeaders();
                GmailDraftListAttachments attachment = GmailDraftListAttachments.builder().build();
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
                GmailDraftListInlineImages inlineImage = GmailDraftListInlineImages.builder().build();
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
}
