package woozlabs.echo.domain.contactGroup.service;

//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class ContactGroupService {
//
//    private final ContactGroupRepository contactGroupRepository;
//    private final AccountRepository accountRepository;
//
//    @Transactional
//    public void createContactGroup(String ownerUid, String contactGroupName) {
//        Account owner = accountRepository.findByUid(ownerUid)
//                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));
//
//        ContactGroup contactGroup = new ContactGroup();
//        contactGroup.setName(contactGroupName);
//        contactGroup.setOwner(owner);
//
//        contactGroup.addAccount(owner);
//        contactGroupRepository.save(contactGroup);
//    }
//
//    @Transactional
//    public void addMembersToContactGroup(Long contactGroupId, List<String> memberEmails) {
//        ContactGroup contactGroup = contactGroupRepository.findById(contactGroupId)
//                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_CONTACT_GROUP));
//
//        for (String memberEmail : memberEmails) {
//            contactGroup.addEmail(memberEmail);
//        }
//        contactGroupRepository.save(contactGroup);
//    }
//
//    public List<ContactGroupResponse> getContactGroupsByOwner(String ownerUid) {
//        Account owner = accountRepository.findByUid(ownerUid)
//                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_ACCOUNT_ERROR_MESSAGE));
//
//        List<ContactGroup> contactGroups = contactGroupRepository.findByOwner(owner);
//        return contactGroups.stream()
//                .map(ContactGroupResponse::new)
//                .collect(Collectors.toList());
//    }
//}
