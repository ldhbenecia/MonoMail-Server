package woozlabs.echo.domain.gmail.dto.extract;

import lombok.Data;
import woozlabs.echo.global.dto.ResponseDto;

@Data
public class RecommendScheduleEmailTemplate implements ResponseDto {
    private String template;
    private Boolean isSchedule;
}