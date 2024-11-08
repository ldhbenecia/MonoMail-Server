package woozlabs.echo.domain.member.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import woozlabs.echo.domain.member.dto.GetAccountResponseDto;
import woozlabs.echo.domain.member.dto.GetPrimaryAccountResponseDto;
import woozlabs.echo.domain.member.dto.profile.AccountProfileResponseDto;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.entity.Member;
import woozlabs.echo.domain.member.entity.MemberAccount;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.domain.member.repository.MemberAccountRepository;
import woozlabs.echo.domain.member.repository.MemberRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;
import woozlabs.echo.global.utils.GoogleOAuthUtils;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final GoogleOAuthUtils googleOAuthUtils;

    public Object getAccountInfo(String uid) {
        Account currentAccount = accountRepository.findByUid(uid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        List<MemberAccount> memberAccounts = memberAccountRepository.findByAccount(currentAccount);
        if (memberAccounts.isEmpty()) {
            throw new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER_ACCOUNT);
        }

        Optional<Member> primaryMember = memberAccounts.stream()
                .map(MemberAccount::getMember)
                .filter(member -> member.getPrimaryUid().equals(uid))
                .findFirst();

        boolean isPrimaryAccount = primaryMember.isPresent();
        Member firstMember = isPrimaryAccount ? primaryMember.get() : memberAccounts.get(0).getMember();

        if (isPrimaryAccount) {
            List<Account> accounts = memberAccountRepository.findAllAccountsByMember(firstMember);

            GetPrimaryAccountResponseDto.MemberDto memberDto = GetPrimaryAccountResponseDto.MemberDto.builder()
                    .displayName(firstMember.getDisplayName())
                    .memberName(firstMember.getMemberName())
                    .email(firstMember.getEmail())
                    .primaryUid(firstMember.getPrimaryUid())
                    .profileImageUrl(firstMember.getProfileImageUrl())
                    .createdAt(firstMember.getCreatedAt())
                    .updatedAt(firstMember.getUpdatedAt())
                    .build();

            List<GetPrimaryAccountResponseDto.AccountDto> accountDtos = accounts.stream()
                    .map(account -> {
                        List<String> grantedScopes = googleOAuthUtils.getGrantedScopes(account.getAccessToken());
                        return GetPrimaryAccountResponseDto.AccountDto.builder()
                                .uid(account.getUid())
                                .email(account.getEmail())
                                .displayName(account.getDisplayName())
                                .profileImageUrl(account.getProfileImageUrl())
                                .provider(account.getProvider())
                                .isExpired(account.getAccessToken() == null)
                                .scopes(grantedScopes)
                                .defaultSignatureId(account.getDefaultSignature().getId())
                                .build();
                    })
                    .collect(Collectors.toList());

            List<GetPrimaryAccountResponseDto.RelatedMemberDto> relatedMembers = memberAccounts.stream()
                    .map(MemberAccount::getMember)
                    .filter(member -> !member.getId().equals(firstMember.getId()))
                    .map(member -> GetPrimaryAccountResponseDto.RelatedMemberDto.builder()
                            .displayName(member.getDisplayName())
                            .memberName(member.getMemberName())
                            .email(member.getEmail())
                            .primaryUid(member.getPrimaryUid())
                            .profileImageUrl(member.getProfileImageUrl())
                            .createdAt(member.getCreatedAt())
                            .updatedAt(member.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());

            return GetPrimaryAccountResponseDto.builder()
                    .member(memberDto)
                    .accounts(accountDtos)
                    .relatedMembers(relatedMembers)
                    .build();
        } else {
            GetAccountResponseDto.AccountDto currentAccountDto = GetAccountResponseDto.AccountDto.builder()
                    .uid(currentAccount.getUid())
                    .email(currentAccount.getEmail())
                    .displayName(currentAccount.getDisplayName())
                    .profileImageUrl(currentAccount.getProfileImageUrl())
                    .provider(currentAccount.getProvider())
                    .isExpired(currentAccount.getAccessToken() == null)
                    .scopes(googleOAuthUtils.getGrantedScopes(currentAccount.getAccessToken()))
                    .defaultSignatureId(currentAccount.getDefaultSignature().getId())
                    .build();

            List<GetAccountResponseDto.RelatedMemberDto> relatedMembers = memberAccounts.stream()
                    .map(MemberAccount::getMember)
                    .map(member -> GetAccountResponseDto.RelatedMemberDto.builder()
                            .displayName(member.getDisplayName())
                            .memberName(member.getMemberName())
                            .email(member.getEmail())
                            .primaryUid(member.getPrimaryUid())
                            .profileImageUrl(member.getProfileImageUrl())
                            .createdAt(member.getCreatedAt())
                            .updatedAt(member.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());

            return GetAccountResponseDto.builder()
                    .accounts(Collections.singletonList(currentAccountDto))
                    .relatedMembers(relatedMembers)
                    .build();
        }
    }

    public AccountProfileResponseDto getProfileByField(String fieldType, String fieldValue) {
        Account account = fetchMemberByField(fieldType, fieldValue);

        return AccountProfileResponseDto.builder()
                .uid(account.getUid())
                .provider(account.getProvider())
                .displayName(account.getDisplayName())
                .profileImageUrl(account.getProfileImageUrl())
                .email(account.getEmail())
                .build();
    }

    private Account fetchMemberByField(String fieldType, String fieldValue) {
        if (fieldType.equals("email")) {
            return accountRepository.findByEmail(fieldValue)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));
        } else if (fieldType.equals("uid")) {
            return accountRepository.findByUid(fieldValue)
                    .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));
        } else {
            throw new CustomErrorException(ErrorCode.INVALID_FIELD_TYPE_ERROR_MESSAGE);
        }
    }

    @Transactional
    public void unlinkAccount(String primaryUid, String accountUid) {
        log.info("Unlinking accountUid: {} from primaryUid: {}", accountUid, primaryUid);
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        Account account = accountRepository.findByUid(accountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        MemberAccount memberAccount = memberAccountRepository.findByMemberAndAccount(member, account)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER_ACCOUNT));

        if (account.getUid().equals(member.getPrimaryUid())) {
            log.warn("Attempt to unlink primary account with UID: {}", accountUid);
            throw new CustomErrorException(ErrorCode.CANNOT_UNLINK_PRIMARY_ACCOUNT);
        }

        log.debug("Removing relation between member: {} and account: {}", primaryUid, accountUid);
        member.getMemberAccounts().remove(memberAccount);
        account.getMemberAccounts().remove(memberAccount);

        memberAccountRepository.delete(memberAccount);
    }

    @Transactional
    public void findAccountAndUpdateLastLogin(String aAUid) {
        Account account = accountRepository.findByUid(aAUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        account.setLastLoginAt(LocalDateTime.now());
    }
}
