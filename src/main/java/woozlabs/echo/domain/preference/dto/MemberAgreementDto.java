package woozlabs.echo.domain.preference.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAgreementDto {

    private boolean termAgreement;  // 약관 동의 여부
    private LocalDateTime termAgreementTimestamp;  // 약관 동의 타임스탬프

    private boolean marketingAgreement;  // 마케팅 동의 여부
    private LocalDateTime marketingAgreementTimestamp;  // 마케팅 동의 타임스탬프
}
