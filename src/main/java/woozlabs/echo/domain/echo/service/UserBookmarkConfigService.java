package woozlabs.echo.domain.echo.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import woozlabs.echo.domain.echo.dto.UserBookmarkConfigDto;
import woozlabs.echo.domain.echo.entity.UserBookmarkConfig;
import woozlabs.echo.domain.echo.repository.UserBookmarkConfigRepository;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;


@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserBookmarkConfigService {

    private final UserBookmarkConfigRepository userBookmarkConfigRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public UserBookmarkConfigDto createOrUpdateBookmark(String activeAccountUid,
                                                        UserBookmarkConfigDto userBookmarkConfigDto) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        UserBookmarkConfig userBookmarkConfig = userBookmarkConfigRepository.findByAccount(account)
                .orElse(new UserBookmarkConfig());

        userBookmarkConfig.setAccount(account);
        userBookmarkConfig.setQuery(userBookmarkConfigDto.getQuery());
        userBookmarkConfig.setTitle(userBookmarkConfigDto.getTitle());

        userBookmarkConfig = userBookmarkConfigRepository.save(userBookmarkConfig);
        return UserBookmarkConfigDto.builder()
                .query(userBookmarkConfig.getQuery())
                .title(userBookmarkConfig.getTitle())
                .build();
    }

    public Optional<UserBookmarkConfigDto> getBookmarkByaAUid(String activeAccountUid) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        UserBookmarkConfig userBookmarkConfig = userBookmarkConfigRepository.findByAccount(account)
                .orElse(null);

        if (userBookmarkConfig != null) {
            UserBookmarkConfigDto dto = UserBookmarkConfigDto.builder()
                    .query(userBookmarkConfig.getQuery())
                    .title(userBookmarkConfig.getTitle())
                    .build();
            return Optional.of(dto);
        }

        return Optional.empty();
    }

    @Transactional
    public void deleteBookmark(String activeAccountUid) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        UserBookmarkConfig userBookmarkConfig = userBookmarkConfigRepository.findByAccount(account)
                .orElse(null);

        if (userBookmarkConfig != null) {
            userBookmarkConfigRepository.delete(userBookmarkConfig);
        }
    }
}
