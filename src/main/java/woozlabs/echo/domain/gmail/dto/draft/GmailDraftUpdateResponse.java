package woozlabs.echo.domain.gmail.dto.draft;

import lombok.Builder;
import lombok.Data;
import woozlabs.echo.global.dto.ResponseDto;

@Data
@Builder
public class GmailDraftUpdateResponse implements ResponseDto {
    private String id;
    private GmailDraftGetMessageResponse message;
}
