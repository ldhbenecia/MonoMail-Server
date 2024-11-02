package woozlabs.echo.domain.subscription.service;

//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class SubscriptionService {
//
//    private final SubscriptionRepository subscriptionRepository;
//    private final MemberRepository memberRepository;
//    private final PaymentService paymentService;
//
//    @Transactional
//    public void activateSubscription(String primaryUid) {
//        Member member = memberRepository.findByPrimaryUid(primaryUid)
//                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));
//
//        paymentService.processPayment(member);
//
//        Subscription subscription = Subscription.builder()
//                .member(member)
//                .plan(Plan.PLUS)
//                .startDate(LocalDateTime.now())
//                .endDate(LocalDateTime.now().plusMonths(1))
//                .build();
//
//        subscriptionRepository.save(subscription);
//    }
//
//}
