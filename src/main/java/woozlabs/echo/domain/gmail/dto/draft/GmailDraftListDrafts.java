package woozlabs.echo.domain.gmail.dto.draft;

import lombok.Data;
import woozlabs.echo.domain.gmail.dto.thread.*;

import java.math.BigInteger;
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
    private List<GmailDraftGetMessagesFrom> from;
    private List<GmailDraftGetMessagesCc> cc;
    private List<GmailDraftGetMessagesBcc> bcc;
    private int attachmentSize;
    private Map<String, GmailThreadListAttachments> attachments;
    private GmailDraftGetMessageResponse message;
}