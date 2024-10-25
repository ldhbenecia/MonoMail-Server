package woozlabs.echo.domain.echo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import woozlabs.echo.domain.echo.dto.UserBookmarkConfigDto;
import woozlabs.echo.domain.echo.service.UserBookmarkConfigService;

@RestController
@RequestMapping("/api/v1/echo")
@RequiredArgsConstructor
public class UserBookmarkConfigController {

    private final UserBookmarkConfigService userBookmarkConfigService;

    @GetMapping("/bookmark")
    public ResponseEntity<UserBookmarkConfigDto> getBookmark(@RequestParam("aAUid") String activeAccountUid) {
        return userBookmarkConfigService.getBookmarkByaAUid(activeAccountUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/bookmark")
    public ResponseEntity<Void> saveBookmark(@RequestParam("aAUid") String activeAccountUid,
                                             @RequestBody UserBookmarkConfigDto dto) {
        userBookmarkConfigService.createOrUpdateBookmark(activeAccountUid, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bookmark")
    public ResponseEntity<Void> deleteBookmark(@RequestParam("aAUid") String activeAccountUid) {
        userBookmarkConfigService.deleteBookmark(activeAccountUid);
        return ResponseEntity.ok().build();
    }
}
