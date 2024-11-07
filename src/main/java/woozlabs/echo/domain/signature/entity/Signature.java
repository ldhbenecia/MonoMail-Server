package woozlabs.echo.domain.signature.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.signature.dto.SignatureRequestDto;
import woozlabs.echo.global.common.entity.BaseEntity;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Signature extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    public static Signature of(final String title, final String content, final Account account) {
        return new Signature(null, title, content, account);
    }

    public void update(SignatureRequestDto signatureRequestDto) {
        if (signatureRequestDto.getTitle() != null) {
            this.title = signatureRequestDto.getTitle();
        }
        if (signatureRequestDto.getContent() != null) {
            this.content = signatureRequestDto.getContent();
        }
    }
}
