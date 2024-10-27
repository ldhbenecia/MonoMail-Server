package woozlabs.echo.domain.contact.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import woozlabs.echo.domain.member.entity.Account;

@Entity
@Getter
@Setter
public class Pin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ElementCollection
    @CollectionTable(name = "pin_email_addresses", joinColumns = @JoinColumn(name = "pin_id"))
    @Column(name = "email_address")
    private Set<String> pinnedEmails = new HashSet<>();
}
