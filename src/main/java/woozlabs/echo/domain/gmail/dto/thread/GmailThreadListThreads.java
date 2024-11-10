package woozlabs.echo.domain.gmail.dto.thread;

import lombok.Data;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Data
public class GmailThreadListThreads {
    private String id;
    private String subject;
    private String snippet;
    private Long timestamp;
    private BigInteger historyId;
    private int threadSize;
    private List<GmailThreadGetMessagesResponse> messages;
}