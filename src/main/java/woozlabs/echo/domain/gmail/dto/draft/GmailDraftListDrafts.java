package woozlabs.echo.domain.gmail.dto.draft;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.api.services.gmail.model.Draft;
import lombok.Data;
import woozlabs.echo.domain.gmail.dto.thread.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class GmailDraftListDrafts {
    private String id;
    private String subject;
    private String snippet;
    private Long timestamp;
    private BigInteger historyId;
    private List<String> labelIds;
    private int threadSize;
    private List<GmailThreadGetMessagesFrom> from;
    private List<GmailThreadGetMessagesCc> cc;
    private List<GmailThreadGetMessagesBcc> bcc;
    private int attachmentSize;
    private Map<String, GmailThreadListAttachments> attachments;
    private GmailThreadGetMessagesResponse message;
}