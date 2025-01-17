package woozlabs.echo.domain.preference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceDto {

    private String language;
    private AppearanceDto appearance;
    private NotificationDto notification;
    private EmailDto email;
}
