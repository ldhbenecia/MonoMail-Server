package woozlabs.echo.domain.gmail.dto.message;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.List;

@Data
@Builder
public class GmailMessageSendRequestWithAtt {
    private List<String> toEmailAddresses;
    private List<String> ccEmailAddresses;
    private List<String> bccEmailAddresses;
    private String fromEmailAddress;
    private String subject;
    private String bodyText;
    private List<byte[]> files;
    private List<String> fileNames;
    private String sendType;
    private String messageId;
}
