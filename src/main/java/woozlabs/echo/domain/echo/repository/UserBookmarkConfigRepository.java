package woozlabs.echo.domain.echo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import woozlabs.echo.domain.echo.entity.UserBookmarkConfig;
import woozlabs.echo.domain.member.entity.Account;

public interface UserBookmarkConfigRepository extends JpaRepository<UserBookmarkConfig, Long> {

    Optional<UserBookmarkConfig> findByAccount(Account account);
}
