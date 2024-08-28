package woozlabs.echo.domain.member.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberAgreementDto {

    private boolean termAgreement;
    private LocalDateTime termAgreementTimestamp;

    private boolean marketingAgreement;
    private LocalDateTime marketingAgreementTimestamp;
}
