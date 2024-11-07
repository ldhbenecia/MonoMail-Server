package woozlabs.echo.domain.gmail.dto.message;

import lombok.Builder;
import lombok.Getter;
import woozlabs.echo.global.dto.ResponseDto;

@Getter
@Builder
public class GmailMessageLazySendResponse implements ResponseDto {
    private String taskId;
}