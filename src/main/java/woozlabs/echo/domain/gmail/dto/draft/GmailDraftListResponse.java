package woozlabs.echo.domain.gmail.dto.draft;

import lombok.Builder;
import lombok.Getter;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesResponse;
import woozlabs.echo.global.dto.ResponseDto;

import java.util.List;

@Getter
@Builder
public class GmailDraftListResponse implements ResponseDto {
    private List<GmailDraftDetailInList> drafts;
    private String nextPageToken;
}
