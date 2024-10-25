package woozlabs.echo.domain.echo.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import woozlabs.echo.domain.echo.dto.UserBookmarkDto;
import woozlabs.echo.domain.echo.entity.UserBookmark;
import woozlabs.echo.domain.echo.repository.UserBookmarkRepository;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.repository.AccountRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;


@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserBookmarkService {

    private final UserBookmarkRepository userBookmarkRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void createBookmark(String activeAccountUid,
                               UserBookmarkDto userBookmarkConfigDto) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        UserBookmark userBookmark = new UserBookmark();
        userBookmark.setAccount(account);
        userBookmark.setQuery(userBookmarkConfigDto.getQuery());
        userBookmark.setTitle(userBookmarkConfigDto.getTitle());
        userBookmark.setIcon(userBookmarkConfigDto.getIcon());

        userBookmarkRepository.save(userBookmark);
    }

    public List<UserBookmarkDto> getBookmarkByaAUid(String activeAccountUid) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        List<UserBookmark> userBookmarks = userBookmarkRepository.findByAccount(account);
        return userBookmarks.stream()
                .map(bookmark -> UserBookmarkDto.builder()
                        .query(bookmark.getQuery())
                        .title(bookmark.getTitle())
                        .icon(bookmark.getIcon())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBookmark(String activeAccountUid, String targetQuery, UserBookmarkDto updateDto) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        UserBookmark userBookmark = userBookmarkRepository.findByAccountAndQuery(account, targetQuery)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_BOOKMARK_ERROR_MESSAGE));

        if (updateDto.getQuery() != null) {
            userBookmark.setQuery(updateDto.getQuery());
        }
        if (updateDto.getTitle() != null) {
            userBookmark.setTitle(updateDto.getTitle());
        }
        if (updateDto.getIcon() != null) {
            userBookmark.setIcon(updateDto.getIcon());
        }

        userBookmarkRepository.save(userBookmark);
    }

    @Transactional
    public void deleteBookmark(String activeAccountUid, String targetQuery) {
        Account account = accountRepository.findByUid(activeAccountUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));

        UserBookmark userBookmark = userBookmarkRepository.findByAccountAndQuery(account, targetQuery)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_BOOKMARK_ERROR_MESSAGE));

        userBookmarkRepository.delete(userBookmark);
    }
}
