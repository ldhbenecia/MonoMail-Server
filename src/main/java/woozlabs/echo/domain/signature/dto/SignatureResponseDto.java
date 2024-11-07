package woozlabs.echo.domain.signature.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignatureResponseDto {

    Map<String, Map<Long, SignatureInfo>> signatures;

    @Getter
    @AllArgsConstructor
    public static class SignatureInfo {
        private String title;
        private String content;
    }
}
