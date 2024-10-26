package woozlabs.echo.domain.echo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookmarkDto {

    private String query;
    private String title;
    private String icon;
}
