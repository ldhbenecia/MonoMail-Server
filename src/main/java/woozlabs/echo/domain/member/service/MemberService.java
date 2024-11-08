package woozlabs.echo.domain.member.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import woozlabs.echo.domain.auth.utils.AuthUtils;
import woozlabs.echo.domain.auth.utils.FirebaseUtils;
import woozlabs.echo.domain.member.dto.ChangePrimaryAccountResponseDto;
import woozlabs.echo.domain.member.dto.CheckPrimaryAccountEligibilityRequestDto;
import woozlabs.echo.domain.member.dto.GetPrimaryAccountResponseDto;
import woozlabs.echo.domain.member.dto.profile.ChangeProfileRequestDto;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.entity.Member;
import woozlabs.echo.domain.member.entity.MemberAccount;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.domain.member.repository.MemberAccountRepository;
import woozlabs.echo.domain.member.repository.MemberRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final FirebaseUtils firebaseUtils;

    @Transactional
    public void softDeleteMember(String primaryUid) {
        log.info("Soft deleting member with UID: {}", primaryUid);
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        member.setDeletedAt(LocalDateTime.now());
        log.info("Successfully soft deleted member with UID: {}", primaryUid);
    }

    @Transactional
    public void superHardDeleteMember(String primaryUid) {
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        List<Account> accountsToDelete = new ArrayList<>();
        List<MemberAccount> memberAccountsToDelete = new ArrayList<>();

        for (MemberAccount memberAccount : member.getMemberAccounts()) {
            Account account = memberAccount.getAccount();
            account.getMemberAccounts().remove(memberAccount);
            memberAccountsToDelete.add(memberAccount);

            if (account.getMemberAccounts().isEmpty()) {
                accountsToDelete.add(account);
            }
        }

        member.getMemberAccounts().clear();
        memberAccountRepository.deleteAll(memberAccountsToDelete);
        memberRepository.delete(member);

        for (Account account : accountsToDelete) {
            try {
                FirebaseAuth.getInstance().deleteUser(account.getUid());
            } catch (FirebaseAuthException e) {
                throw new CustomErrorException(ErrorCode.FIREBASE_ACCOUNT_DELETION_ERROR, e.getMessage());
            }
        }

        accountRepository.deleteAll(accountsToDelete);

        log.info("Successfully deleted member with UID: {}", primaryUid);
    }

    @Transactional
    public GetPrimaryAccountResponseDto createMember(String uid) {
        Account account = accountRepository.findByUid(uid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        List<MemberAccount> memberAccounts = memberAccountRepository.findByAccount(account);

        String memberName = account.getDisplayName() + "-" + AuthUtils.generateRandomString();

        Member newMember = Member.builder()
                .displayName(account.getDisplayName())
                .memberName(memberName)
                .email(account.getEmail())
                .primaryUid(account.getUid())
                .profileImageUrl(account.getProfileImageUrl())
                .primaryUid(account.getUid())
                .build();

        newMember.setMemberAccounts(new ArrayList<>());

        memberRepository.save(newMember);

        MemberAccount newMemberAccount = new MemberAccount(newMember, account);
        newMember.addMemberAccount(newMemberAccount);
        account.getMemberAccounts().add(newMemberAccount);

        memberAccountRepository.save(newMemberAccount);

        List<Member> relatedMembers = memberAccounts.stream()
                .map(MemberAccount::getMember)
                .filter(member -> !member.getId().equals(newMember.getId()))
                .collect(Collectors.toList());

        List<GetPrimaryAccountResponseDto.RelatedMemberDto> relatedMemberDtos = relatedMembers.stream()
                .map(relatedMember -> GetPrimaryAccountResponseDto.RelatedMemberDto.builder()
                        .displayName(relatedMember.getDisplayName())
                        .memberName(relatedMember.getMemberName())
                        .email(relatedMember.getEmail())
                        .primaryUid(relatedMember.getPrimaryUid())
                        .profileImageUrl(relatedMember.getProfileImageUrl())
                        .createdAt(relatedMember.getCreatedAt())
                        .updatedAt(relatedMember.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        GetPrimaryAccountResponseDto.MemberDto memberDto = GetPrimaryAccountResponseDto.MemberDto.builder()
                .displayName(newMember.getDisplayName())
                .memberName(newMember.getMemberName())
                .email(newMember.getEmail())
                .primaryUid(newMember.getPrimaryUid())
                .profileImageUrl(newMember.getProfileImageUrl())
                .createdAt(newMember.getCreatedAt())
                .updatedAt(newMember.getUpdatedAt())
                .build();

        return GetPrimaryAccountResponseDto.builder()
                .member(memberDto)
                .accounts(Collections.singletonList(GetPrimaryAccountResponseDto.AccountDto.builder()
                        .uid(account.getUid())
                        .email(account.getEmail())
                        .displayName(account.getDisplayName())
                        .profileImageUrl(account.getProfileImageUrl())
                        .provider(account.getProvider())
                        .build()))
                .relatedMembers(relatedMemberDtos)
                .build();
    }

    @Transactional
    public void changeProfile(String primaryUid, ChangeProfileRequestDto changeProfileRequestDto) {
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        String displayName = changeProfileRequestDto.getDisplayName();
        String profileImageUrl = changeProfileRequestDto.getProfileImageUrl();
        String language = changeProfileRequestDto.getLanguage();

        if (displayName != null) {
            member.setDisplayName(displayName);
        }
        if (profileImageUrl != null) {
            member.setProfileImageUrl(profileImageUrl);
        }
        if (language != null) {
            member.setLanguage(language);
        }
    }

    @Transactional
    public ChangePrimaryAccountResponseDto changePrimaryAccount(String primaryUid, String newPrimaryUid)
            throws FirebaseAuthException {
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        Account account = accountRepository.findByUid(newPrimaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        if (!isValidAccountToChangePrimary(member, newPrimaryUid)) {
            throw new CustomErrorException(ErrorCode.INVALID_ACCOUNT_CHANGE);
        }

        member.setPrimaryUid(newPrimaryUid);
        member.setEmail(account.getEmail());
        memberRepository.save(member);

        String primaryToken;
        try {
            primaryToken = firebaseUtils.createCustomToken(newPrimaryUid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to create Firebase custom token", e);
            throw new CustomErrorException(ErrorCode.FIREBASE_AUTH_ERROR, e.getMessage());
        }

        log.info("Primary account changed for member: {}", member.getId());
        return new ChangePrimaryAccountResponseDto(primaryToken);
    }

    public Map<String, Boolean> checkPrimaryAccountEligibility(String primaryUid,
                                                               CheckPrimaryAccountEligibilityRequestDto requestDto) {
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        Map<String, Boolean> eligibilityMap = new HashMap<>();

        for (String uid : requestDto.getUids()) {
            boolean isEligible = isValidAccountToChangePrimary(member, uid);
            eligibilityMap.put(uid, isEligible);
        }

        return eligibilityMap;
    }

    private boolean isValidAccountToChangePrimary(Member member, String newPrimaryUid) {
        boolean isValidAccount = member.getMemberAccounts().stream()
                .anyMatch(ma -> ma.getAccount().getUid().equals(newPrimaryUid));

        boolean isNotPrimaryAccount = !memberRepository.findByPrimaryUid(newPrimaryUid).isPresent();

        return isValidAccount && isNotPrimaryAccount;
    }
}
