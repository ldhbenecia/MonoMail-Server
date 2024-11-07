package woozlabs.echo.domain.contact.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import woozlabs.echo.domain.contact.dto.PinRequestDto;
import woozlabs.echo.domain.contact.dto.PinResponseDto;
import woozlabs.echo.domain.contact.service.PinService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/echo")
public class PinController {

    private final PinService pinService;

    @GetMapping("/pin")
    public ResponseEntity<PinResponseDto> getPins(@RequestParam("aAUid") String activeAccountUid) {
        PinResponseDto pins = pinService.getPin(activeAccountUid);
        return ResponseEntity.ok(pins);
    }

    @PostMapping("/pin")
    public ResponseEntity<Void> createPin(@RequestParam("aAUid") String activeAccountUid,
                                          @RequestBody PinRequestDto requestDto) {
        pinService.createPin(activeAccountUid, requestDto.getPinnedEmail());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/pin/{pinnedEmail}")
    public ResponseEntity<Void> deletePin(@PathVariable("pinnedEmail") String pinnedEmail,
                                          @RequestParam("aAUid") String activeAccountUid) {
        pinService.deletePin(activeAccountUid, pinnedEmail);
        return ResponseEntity.noContent().build();
    }
}
