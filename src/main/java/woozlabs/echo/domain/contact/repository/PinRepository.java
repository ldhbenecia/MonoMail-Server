package woozlabs.echo.domain.contact.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import woozlabs.echo.domain.contact.entity.Pin;
import woozlabs.echo.domain.member.entity.Account;

public interface PinRepository extends JpaRepository<Pin, Long> {

    List<Pin> findByAccount(Account account);
}
