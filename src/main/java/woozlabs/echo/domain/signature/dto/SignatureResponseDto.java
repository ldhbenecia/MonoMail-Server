package woozlabs.echo.domain.signature.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignatureResponseDto {

    Map<String, List<String>> signatures;
}
