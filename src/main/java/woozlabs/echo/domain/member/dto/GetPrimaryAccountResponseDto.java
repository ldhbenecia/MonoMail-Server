package woozlabs.echo.domain.member.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPrimaryAccountResponseDto {

    private MemberDto member;
    private List<AccountDto> accounts;
    private AccountDto primaryAccount;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private Long id;
        private String displayName;
        private String memberName;
        private String profileImageUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountDto {
        private Long id;
        private String uid;
        private String email;
        private String displayName;
        private String profileImageUrl;
        private String provider;
        private String providerId;
    }
}
