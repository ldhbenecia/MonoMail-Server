package woozlabs.echo.domain.echo.dto.emailTemplate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import woozlabs.echo.domain.echo.entity.EmailTemplate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateResponse {

    private Long key;
    private String templateName;
    private String subject;
    private String body;

    public EmailTemplateResponse(EmailTemplate emailTemplate) {
        this.key = emailTemplate.getId();
        this.templateName = emailTemplate.getTemplateName();
        this.subject = emailTemplate.getSubject();
        this.body = emailTemplate.getBody();
    }
}
