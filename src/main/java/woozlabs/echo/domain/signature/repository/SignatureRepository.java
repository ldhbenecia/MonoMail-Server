package woozlabs.echo.domain.signature.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import woozlabs.echo.domain.signature.entity.Signature;

public interface SignatureRepository extends JpaRepository<Signature, Long> {
}
