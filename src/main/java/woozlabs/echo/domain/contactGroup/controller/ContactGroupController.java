package woozlabs.echo.domain.contactGroup.controller;

//@RestController
//@RequestMapping("/api/v1/echo")
//@RequiredArgsConstructor
//public class ContactGroupController {
//
//    private final ContactGroupService contactGroupService;
//
//    @GetMapping("/contactGroups")
//    public ResponseEntity<List<ContactGroupResponse>> getContactGroupsByOwner(HttpServletRequest httpServletRequest) {
//        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
//        List<ContactGroupResponse> contactGroups = contactGroupService.getContactGroupsByOwner(uid);
//        return ResponseEntity.ok(contactGroups);
//    }
//
//    @PostMapping("/contactGroups")
//    public ResponseEntity<Void> createContactGroup(HttpServletRequest httpServletRequest,
//                                                   @RequestBody CreateContactGroupRequest createContactGroupRequest) {
//        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
//        String contactGroupName = createContactGroupRequest.getName();
//        contactGroupService.createContactGroup(uid, contactGroupName);
//        return ResponseEntity.status(201).build();
//    }
//
//    @PostMapping("/contactGroups/{contactGroupId}/members")
//    public ResponseEntity<Void> addMembersToContactGroup(
//            @PathVariable("contactGroupId") Long contactGroupId,
//            @RequestBody List<String> memberEmails) {
//        contactGroupService.addMembersToContactGroup(contactGroupId, memberEmails);
//        return ResponseEntity.ok().build();
//    }
//}
