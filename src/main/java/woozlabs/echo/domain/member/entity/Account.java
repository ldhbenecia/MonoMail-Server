package woozlabs.echo.domain.member.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import woozlabs.echo.domain.contactGroup.entity.AccountContactGroup;
import woozlabs.echo.domain.echo.entity.EmailTemplate;
import woozlabs.echo.domain.echo.entity.UserBookmark;
import woozlabs.echo.domain.signature.Signature;
import woozlabs.echo.global.common.entity.BaseEntity;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uid;
    private String provider;
    private String providerId;
    private String displayName;
    private String email;
    private String profileImageUrl;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessTokenFetchedAt;
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberAccount> memberAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<EmailTemplate> emailTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserBookmark> bookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<AccountContactGroup> accountContactGroups = new ArrayList<>();

    @OneToMany(mappedBy = "ownerId", cascade = CascadeType.ALL)
    private List<Signature> allSignatures = new ArrayList<>();

    public List<Signature> getSignatures() {
        return allSignatures.stream()
                .filter(signature -> signature.getType() == Signature.SignatureType.MEMBER)
                .collect(Collectors.toList());
    }
}
