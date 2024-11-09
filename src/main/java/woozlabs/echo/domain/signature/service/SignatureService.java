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
import woozlabs.echo.domain.member.entity.MemberPreference;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.domain.member.repository.MemberAccountRepository;
import woozlabs.echo.domain.member.repository.MemberRepository;
import woozlabs.echo.domain.signature.dto.SignatureRequestDto;
import woozlabs.echo.domain.signature.dto.SignatureResponseDto;
import woozlabs.echo.domain.signature.dto.SignatureResponseDto.SignatureInfo;
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
    private final MemberAccountRepository memberAccountRepository;

    public SignatureResponseDto getSignatures(final String uid, final boolean isDirectAccountRequest) {
        final Map<String, Map<Long, SignatureInfo>> signaturesMap = new HashMap<>();

        if (isDirectAccountRequest) {
            // aAUid가 들어온 경우 - 특정 Account의 시그니처만 반환
            final Account account = accountRepository.findByUid(uid)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

            final Map<Long, SignatureInfo> signatures = account.getSignatures().stream()
                    .collect(Collectors.toMap(
                            Signature::getId,
                            signature -> new SignatureInfo(signature.getTitle(), signature.getContent())
                    ));

            signaturesMap.put(account.getUid(), signatures);
        } else {
            // aAUid가 없는 경우 - 해당 uid를 primaryUid로 가지는 Member의 모든 Account 시그니처 반환
            final Member member = memberRepository.findByPrimaryUid(uid)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

            final List<Account> memberAccounts = member.getMemberAccounts().stream()
                    .map(MemberAccount::getAccount)
                    .collect(Collectors.toList());

            for (final Account memberAccount : memberAccounts) {
                final Map<Long, SignatureInfo> signatures = memberAccount.getSignatures().stream()
                        .collect(Collectors.toMap(
                                Signature::getId,
                                signature -> new SignatureInfo(signature.getTitle(), signature.getContent())
                        ));
                signaturesMap.put(memberAccount.getUid(), signatures);
            }
        }

        return new SignatureResponseDto(signaturesMap);
    }

    @Transactional
    public void deleteSignature(final String uid, final Long signatureId) {
        final Member member = memberRepository.findByPrimaryUid(uid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        final Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_SIGNATURE));

        boolean hasAccess = member.getMemberAccounts().stream()
                .anyMatch(memberAccount -> memberAccount.getAccount().equals(signature.getAccount()));

        if (!hasAccess) {
            throw new CustomErrorException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        signatureRepository.delete(signature);
    }

    @Transactional
    public void createSignature(final String uid, final SignatureRequestDto signatureRequestDto) {
        final Account account = accountRepository.findByUid(uid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        final Signature signature = Signature.of(signatureRequestDto.getTitle(), signatureRequestDto.getContent(),
                account);

        signatureRepository.save(signature);
    }

    @Transactional
    public void updateSignature(String uid, Long signatureId, SignatureRequestDto signatureRequestDto) {
        final Member member = memberRepository.findByPrimaryUid(uid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        final Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_SIGNATURE));

        boolean hasAccess = member.getMemberAccounts().stream()
                .anyMatch(memberAccount -> memberAccount.getAccount().equals(signature.getAccount()));

        if (!hasAccess) {
            throw new CustomErrorException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        signature.update(signatureRequestDto);
    }

    @Transactional
    public void setDefaultSignature(final String primaryUid, final String accountUid, final Long signatureId) {
        final Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        final MemberPreference memberPreference = member.getPreference();
        memberPreference.setDefaultSignature(accountUid, signatureId);
    }
}
