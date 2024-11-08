package woozlabs.echo.domain.member.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Member member;

    private String language = "en";

    @Enumerated(EnumType.STRING)
    private Theme theme = Theme.LIGHT;

    @Enumerated(EnumType.STRING)
    private Density density = Density.COMPACT;

    private Boolean marketingEmails;
    private Boolean securityEmails;
    private Integer closeWindow;
    private String alertSound;

    @ElementCollection
    @CollectionTable(name = "accounts_watch_notifications", joinColumns = @JoinColumn(name = "member_id"))
    @MapKeyColumn(name = "account_uid")
    @Column(name = "notification_setting")
    @Enumerated(EnumType.STRING)
    private Map<String, Watch> watchNotification = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "account_marketing_email_timestamps", joinColumns = @JoinColumn(name = "account_id"))
    @MapKeyColumn(name = "marketing_email")
    @Column(name = "timestamp")
    private Map<Boolean, LocalDateTime> marketingEmailTimestamps = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "account_security_email_timestamps", joinColumns = @JoinColumn(name = "account_id"))
    @MapKeyColumn(name = "security_email")
    @Column(name = "timestamp")
    private Map<Boolean, LocalDateTime> securityEmailTimestamps = new HashMap<>();



    @ElementCollection
    @CollectionTable(name = "account_default_signatures", joinColumns = @JoinColumn(name = "member_id"))
    @MapKeyColumn(name = "account_uid")
    @Column(name = "signature_id")
    private Map<String, Long> defaultSignature = new HashMap<>();

    private Integer cancelWindow;

}
