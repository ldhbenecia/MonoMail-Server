package woozlabs.echo.domain.echo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import woozlabs.echo.domain.echo.entity.UserBookmark;
import woozlabs.echo.domain.member.entity.Account;

public interface UserBookmarkRepository extends JpaRepository<UserBookmark, Long> {

    List<UserBookmark> findByAccount(Account account);

    Optional<UserBookmark> findByAccountAndQuery(Account account, String query);
}
