package woozlabs.echo.domain.subscription.entity;

//@Getter
//@NoArgsConstructor
//public class Subscription {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id")
//    private Member member;
//
//    @Enumerated(EnumType.STRING)
//    private Plan plan;
//
//    private LocalDateTime startDate;
//    private LocalDateTime endDate;
//
//    @Builder
//    public Subscription(Member member, Plan plan, LocalDateTime startDate, LocalDateTime endDate) {
//        this.member = member;
//        this.plan = plan;
//        this.startDate = startDate;
//        this.endDate = endDate;
//    }
//
//    public boolean isActive() {
//        return endDate == null || LocalDateTime.now().isBefore(endDate);
//    }
//}
