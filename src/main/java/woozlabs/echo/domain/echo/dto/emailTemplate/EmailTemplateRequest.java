package woozlabs.echo.domain.echo.dto.emailTemplate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplateRequest {

    private String templateName;
    private String subject;
    private String body;
}
