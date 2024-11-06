package woozlabs.echo.domain.signature.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.entity.Member;
import woozlabs.echo.domain.member.entity.MemberAccount;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.domain.member.repository.MemberRepository;
import woozlabs.echo.domain.signature.dto.SignatureResponseDto;
import woozlabs.echo.domain.signature.entity.Signature;
import woozlabs.echo.domain.signature.repository.SignatureRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SignatureService {

    private final SignatureRepository signatureRepository;
    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;

    public SignatureResponseDto getSignatures(final String uid, final boolean isDirectAccountRequest) {
        final Map<String, List<String>> signaturesMap = new HashMap<>();

        if (isDirectAccountRequest) {
            // aAUid가 들어온 경우 - 특정 Account의 시그니처만 반환
            final Account account = accountRepository.findByUid(uid)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

            final List<String> signatures = account.getSignatures().stream()
                    .map(Signature::getContent)
                    .collect(Collectors.toList());
            signaturesMap.put(account.getUid(), signatures);
        } else {
            // aAUid가 없는 경우 - 해당 uid를 primaryUid로 가지는 Member의 모든 Account 시그니처 반환
            final Member member = memberRepository.findByPrimaryUid(uid)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

            final List<Account> memberAccounts = member.getMemberAccounts().stream()
                    .map(MemberAccount::getAccount)
                    .collect(Collectors.toList());

            for (final Account memberAccount : memberAccounts) {
                final List<String> signatures = memberAccount.getSignatures().stream()
                        .map(Signature::getContent)
                        .collect(Collectors.toList());
                signaturesMap.put(memberAccount.getUid(), signatures);
            }
        }

        return new SignatureResponseDto(signaturesMap);
    }

    @Transactional
    public void deleteSignature(final String uid, final Long signatureId, final boolean isDirectAccountRequest) {
        final Account account = accountRepository.findByUid(uid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        final Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_SIGNATURE));

        if (isDirectAccountRequest) {
            // aAUid가 들어온 경우 - 해당 Account의 시그니처만 삭제 예외처리
            if (!signature.getAccount().equals(account)) {
                throw new CustomErrorException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
        } else {
            // aAUid가 없는 경우 - 해당 uid를 primaryUid로 가지는 Member의 accounts들만 삭제 가능 처리
            final Member member = memberRepository.findByPrimaryUid(uid)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

            final boolean hasAccess = member.getMemberAccounts().stream()
                    .anyMatch(memberAccount -> memberAccount.getAccount().equals(signature.getAccount()));

            if (!hasAccess) {
                throw new CustomErrorException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
        }

        signatureRepository.delete(signature);
    }
}
