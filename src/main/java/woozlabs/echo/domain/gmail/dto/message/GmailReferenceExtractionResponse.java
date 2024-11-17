package woozlabs.echo.domain.gmail.dto.message;

import lombok.Data;

import java.util.List;

@Data
public class GmailReferenceExtractionResponse {
    private List<String> references;
}
