package woozlabs.echo.domain.gmail.dto.message;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Builder
public class GmailMessageLazySendRequest {
    private String toEmailAddresses;
    private String ccEmailAddresses;
    private String bccEmailAddresses;
    private String subject;
    private String body;
    private List<MultipartFile> files;
    private String aAUid;
    private String type;
    private String messageId;
}
