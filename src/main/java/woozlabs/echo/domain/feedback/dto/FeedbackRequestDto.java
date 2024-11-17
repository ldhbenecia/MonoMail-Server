package woozlabs.echo.domain.feedback.dto;

import lombok.Getter;

@Getter
public class FeedbackRequestDto {

    private String category;
    private String content;
}
