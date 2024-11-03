package woozlabs.echo.domain.contactGroup.entity;

//@Getter
//@Setter
//@NoArgsConstructor
//public class ContactGroup extends BaseEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String name;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "owner_id")
//    private Account owner;
//
//    @OneToMany(mappedBy = "contactGroup", cascade = CascadeType.ALL)
//    private List<AccountContactGroup> accountContactGroups = new ArrayList<>();
//
//    @ElementCollection
//    @CollectionTable(name = "contactGroup_emails", joinColumns = @JoinColumn(name = "contactGroup_id"))
//    @Column(name = "email")
//    private List<String> emails = new ArrayList<>();
//
//    public void addAccount(Account account) {
//        AccountContactGroup accountContactGroup = new AccountContactGroup();
//        accountContactGroup.setContactGroup(this);
//        accountContactGroup.setAccount(account);
//        this.accountContactGroups.add(accountContactGroup);
//        account.getAccountContactGroups().add(accountContactGroup);
//    }
//
//    public void addEmail(String email) {
//        if (!this.emails.contains(email)) {
//            this.emails.add(email);
//        }
//    }
//}
