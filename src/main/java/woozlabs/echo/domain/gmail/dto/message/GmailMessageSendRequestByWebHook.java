package woozlabs.echo.domain.gmail.dto.message;

import lombok.Data;

@Data
public class GmailMessageSendRequestByWebHook {
    private String taskId;
}
