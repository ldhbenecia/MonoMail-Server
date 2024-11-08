package woozlabs.echo.domain.preference.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import woozlabs.echo.domain.member.entity.Member;
import woozlabs.echo.domain.member.entity.MemberPreference;
import woozlabs.echo.domain.member.repository.MemberRepository;
import woozlabs.echo.domain.preference.dto.AppearanceDto;
import woozlabs.echo.domain.preference.dto.EmailDto;
import woozlabs.echo.domain.preference.dto.NotificationDto;
import woozlabs.echo.domain.preference.dto.PreferenceDto;
import woozlabs.echo.domain.preference.dto.UpdatePreferenceRequestDto;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final MemberRepository memberRepository;

    @Transactional
    public void updatePreference(String primaryUid, UpdatePreferenceRequestDto updatePreferenceRequest) {
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        MemberPreference memberPreference = member.getPreference();
        if (memberPreference == null) {
            memberPreference = new MemberPreference();
            member.setPreference(memberPreference);
        }

        PreferenceDto preferenceDto = updatePreferenceRequest.getPreference();
        if (preferenceDto != null) {
            if (preferenceDto.getLanguage() != null) {
                memberPreference.setLanguage(preferenceDto.getLanguage());
            }

            AppearanceDto appearanceDto = preferenceDto.getAppearance();
            if (appearanceDto != null) {
                if (appearanceDto.getTheme() != null) {
                    memberPreference.setTheme(appearanceDto.getTheme());
                }
                if (appearanceDto.getDensity() != null) {
                    memberPreference.setDensity(appearanceDto.getDensity());
                }
            }

            NotificationDto notificationDto = preferenceDto.getNotification();
            if (notificationDto != null) {
                if (notificationDto.getWatchNotification() != null) {
                    memberPreference.setWatchNotification(notificationDto.getWatchNotification());
                }
                if (notificationDto.getAlertSound() != null) {
                    memberPreference.setAlertSound(notificationDto.getAlertSound());
                }
                if (notificationDto.getMarketingEmails() != null) {
                    memberPreference.setMarketingEmails(notificationDto.getMarketingEmails());
                }
                if (notificationDto.getSecurityEmails() != null) {
                    memberPreference.setSecurityEmails(notificationDto.getSecurityEmails());
                }
            }


            // Update email preferences
            EmailDto emailDto = preferenceDto.getEmail(); // Get the email preference part
            if (emailDto != null) {
                if (emailDto.getCancelWindow() >= 0) { // Ensure valid cancel window
                    memberPreference.setCancelWindow(emailDto.getCancelWindow());
                }
                if (emailDto.getDefaultSignature() != null) {
                    memberPreference.setDefaultSignature(emailDto.getDefaultSignature());
                }
            }
        }
    }

    public PreferenceDto getPreference(String primaryUid) {
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        MemberPreference memberPreference = member.getPreference();

        return PreferenceDto.builder()
                .language(memberPreference.getLanguage())
                .appearance(AppearanceDto.builder()
                        .theme(memberPreference.getTheme())
                        .density(memberPreference.getDensity())
                        .build())
                .notification(NotificationDto.builder()
                        .watchNotification(memberPreference.getWatchNotification())
                        .alertSound(memberPreference.getAlertSound())
                        .build())
                .email(EmailDto.builder() // Add the email preferences
                        .defaultSignature(memberPreference.getDefaultSignature())
                        .cancelWindow(memberPreference.getCancelWindow())
                        .build())
                .build();
    }
}
