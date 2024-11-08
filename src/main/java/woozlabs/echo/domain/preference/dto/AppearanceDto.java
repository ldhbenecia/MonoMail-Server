package woozlabs.echo.domain.preference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import woozlabs.echo.domain.member.entity.Density;
import woozlabs.echo.domain.member.entity.Theme;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppearanceDto {

    private Theme theme;
    private Density density;
}

