package woozlabs.echo.domain.signature.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignatureResponseDto {

    Map<String, Map<Long, String>> signatures;
}
