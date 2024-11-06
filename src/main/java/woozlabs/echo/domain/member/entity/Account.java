package woozlabs.echo.domain.member.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import woozlabs.echo.domain.echo.entity.EmailTemplate;
import woozlabs.echo.domain.echo.entity.UserBookmark;
import woozlabs.echo.domain.signature.entity.Signature;
import woozlabs.echo.global.common.entity.BaseEntity;

@Entity
@Table(indexes = {
        @Index(name = "idx_uid", columnList = "uid", unique = true),
})
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
    private Boolean hasBookmark = false;

    @Builder.Default
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberAccount> memberAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<EmailTemplate> emailTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserBookmark> bookmarks = new ArrayList<>();

    // alpha not used
    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    // private List<AccountContactGroup> accountContactGroups = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Signature> signatures = new ArrayList<>();
}
