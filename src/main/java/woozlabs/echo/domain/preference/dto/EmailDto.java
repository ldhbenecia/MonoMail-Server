package woozlabs.echo.domain.preference.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {
    private Map<String, Long> defaultSignature;
    private Integer cancelWindow;
}