package woozlabs.echo.domain.preference.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import woozlabs.echo.domain.member.entity.Watch;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Map<String, Watch> watchNotification;
    private String alertSound;
    private Boolean marketingEmails;
    private Boolean securityEmails;
}
