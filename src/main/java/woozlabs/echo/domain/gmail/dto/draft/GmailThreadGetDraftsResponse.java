package woozlabs.echo.domain.gmail.dto.draft;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import woozlabs.echo.domain.gmail.dto.thread.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class GmailThreadGetDraftsResponse {
    private String id; // draft id
    private String subject;
    private Long timestamp;
    private String timezone = ""; // timezone
    private GmailThreadGetMessagesFrom from;
    private List<GmailThreadGetMessagesCc> cc = new ArrayList<>();
    private List<GmailThreadGetMessagesBcc> bcc = new ArrayList<>();
    private List<GmailThreadGetMessagesTo> to = new ArrayList<>();
    private String threadId; // thread id
    private List<String> labelIds;
    private String snippet;
    private BigInteger historyId;
    private GmailThreadGetPayload payload;
    private Map<String, GmailThreadListAttachments> attachments;
    private Map<String, GmailThreadListInlineImages> inlineImages;
}
