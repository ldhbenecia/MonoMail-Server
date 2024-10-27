package woozlabs.echo.domain.contact.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import woozlabs.echo.domain.contact.dto.PinResponseDto;
import woozlabs.echo.domain.contact.entity.Pin;
import woozlabs.echo.domain.contact.repository.PinRepository;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PinService {

    private final PinRepository pinRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void createPin(String activeAccountUid, String pinnedEmail) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        Pin pin = pinRepository.findByAccount(account)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Pin newPin = new Pin();
                    newPin.setAccount(account);
                    return newPin;
                });

        pin.getPinnedEmails().add(pinnedEmail);
        pinRepository.save(pin);
    }

    public PinResponseDto getPin(String activeAccountUid) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        List<Pin> pins = pinRepository.findByAccount(account);

        List<String> pinnedEmails = pins.stream()
                .flatMap(pin -> pin.getPinnedEmails().stream())
                .collect(Collectors.toList());

        return PinResponseDto.builder()
                .uid(activeAccountUid)
                .pinnedEmails(pinnedEmails)
                .build();
    }

    @Transactional
    public void deletePin(String activeAccountUid, String pinnedEmail) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        List<Pin> pins = pinRepository.findByAccount(account);

        Pin pin = pins.stream()
                .filter(p -> p.getPinnedEmails().contains(pinnedEmail))
                .findFirst()
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_PIN));

        pin.getPinnedEmails().remove(pinnedEmail);
        pinRepository.save(pin);
    }
}
