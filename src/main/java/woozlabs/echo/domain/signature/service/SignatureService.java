package woozlabs.echo.domain.signature.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.entity.Member;
import woozlabs.echo.domain.member.entity.MemberAccount;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.domain.member.repository.MemberAccountRepository;
import woozlabs.echo.domain.member.repository.MemberRepository;
import woozlabs.echo.domain.signature.dto.SignatureResponseDto;
import woozlabs.echo.domain.signature.entity.Signature;
import woozlabs.echo.domain.signature.repository.SignatureRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final SignatureRepository signatureRepository;
    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final MemberAccountRepository memberAccountRepository;

    public SignatureResponseDto getSignatures(String uid, boolean isDirectAccountRequest) {
        Map<String, List<String>> signaturesMap = new HashMap<>();

        if (isDirectAccountRequest) {
            // aAUid가 들어온 경우 - 특정 Account의 시그니처만 반환
            Account account = accountRepository.findByUid(uid)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

            List<String> signatures = account.getSignatures().stream()
                    .map(Signature::getContent)
                    .collect(Collectors.toList());
            signaturesMap.put(account.getUid(), signatures);
        } else {
            // aAUid가 없는 경우 - 해당 uid를 primaryUid로 가지는 Member의 모든 Account 시그니처 반환
            Member member = memberRepository.findByPrimaryUid(uid)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

            List<Account> memberAccounts = member.getMemberAccounts().stream()
                    .map(MemberAccount::getAccount)
                    .collect(Collectors.toList());

            for (Account memberAccount : memberAccounts) {
                List<String> signatures = memberAccount.getSignatures().stream()
                        .map(Signature::getContent)
                        .collect(Collectors.toList());
                signaturesMap.put(memberAccount.getUid(), signatures);
            }
        }

        return new SignatureResponseDto(signaturesMap);
    }
}
