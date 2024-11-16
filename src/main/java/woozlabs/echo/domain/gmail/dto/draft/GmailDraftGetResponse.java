package woozlabs.echo.domain.gmail.dto.draft;

import lombok.Builder;
import lombok.Getter;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesResponse;
import woozlabs.echo.global.dto.ResponseDto;

@Getter
@Builder
public class GmailDraftGetResponse {
    private String draftId;
    private GmailDraftGetMessageResponse message;
}