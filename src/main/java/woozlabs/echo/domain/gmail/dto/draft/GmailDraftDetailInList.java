package woozlabs.echo.domain.gmail.dto.draft;

import lombok.Data;
import woozlabs.echo.domain.gmail.dto.thread.GmailThreadGetMessagesResponse;

import java.util.List;

@Data
public class GmailDraftDetailInList {
    private String draftId;
    private GmailDraftGetMessageResponse message;
}