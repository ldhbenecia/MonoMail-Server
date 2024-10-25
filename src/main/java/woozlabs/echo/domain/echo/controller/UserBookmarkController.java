package woozlabs.echo.domain.echo.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import woozlabs.echo.domain.echo.dto.UserBookmarkDto;
import woozlabs.echo.domain.echo.service.UserBookmarkService;

@RestController
@RequestMapping("/api/v1/echo")
@RequiredArgsConstructor
public class UserBookmarkController {

    private final UserBookmarkService userBookmarkService;

    @GetMapping("/bookmark")
    public ResponseEntity<List<UserBookmarkDto>> getBookmarks(@RequestParam("aAUid") String activeAccountUid) {
        List<UserBookmarkDto> bookmarks = userBookmarkService.getBookmarkByaAUid(activeAccountUid);
        return ResponseEntity.ok(bookmarks);
    }

    @PostMapping("/bookmark")
    public ResponseEntity<Void> saveBookmark(@RequestParam("aAUid") String activeAccountUid,
                                             @RequestBody UserBookmarkDto dto) {
        userBookmarkService.createBookmark(activeAccountUid, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/bookmark")
    public ResponseEntity<Void> updateBookmark(
            @RequestParam("aAUid") String activeAccountUid,
            @RequestParam("query") String targetQuery,
            @RequestBody UserBookmarkDto updateDto) {
        userBookmarkService.updateBookmark(activeAccountUid, targetQuery, updateDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bookmark")
    public ResponseEntity<Void> deleteBookmark(@RequestParam("aAUid") String activeAccountUid,
                                               @RequestParam("query") String targetQuery) {
        userBookmarkService.deleteBookmark(activeAccountUid, targetQuery);
        return ResponseEntity.noContent().build();
    }
}
