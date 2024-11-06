package woozlabs.echo.domain.signature.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import woozlabs.echo.domain.signature.dto.SignatureRequestDto;
import woozlabs.echo.domain.signature.dto.SignatureResponseDto;
import woozlabs.echo.domain.signature.service.SignatureService;

@RestController
@RequestMapping("/api/v1/echo")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    @GetMapping("/signature")
    public ResponseEntity<SignatureResponseDto> getSignatures(HttpServletRequest httpServletRequest,
                                                              @RequestParam(value = "aAUid", required = false) final String activeAccountUid) {
        final boolean isDirectAccountRequest = (activeAccountUid != null);
        final String uid = isDirectAccountRequest ? activeAccountUid : (String) httpServletRequest.getAttribute("uid");

        final SignatureResponseDto response = signatureService.getSignatures(uid, isDirectAccountRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/signature/{signatureId}")
    public ResponseEntity<Void> deleteSignature(HttpServletRequest httpServletRequest,
                                                @PathVariable("signatureId") final Long signatureId) {
        final String uid = (String) httpServletRequest.getAttribute("uid");
        signatureService.deleteSignature(uid, signatureId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signature")
    public ResponseEntity<Void> createSignature(@RequestParam("aAUid") final String activeAccountUid,
                                                @RequestBody final SignatureRequestDto signatureRequestDto) {
        signatureService.createSignature(activeAccountUid, signatureRequestDto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/signature/{signatureId}")
    public ResponseEntity<Void> updateSignature(HttpServletRequest httpServletRequest,
                                                @PathVariable("signatureId") Long signatureId,
                                                @RequestBody SignatureRequestDto signatureRequestDto) {
        final String uid = (String) httpServletRequest.getAttribute("uid");
        signatureService.updateSignature(uid, signatureId, signatureRequestDto);
        return ResponseEntity.ok().build();
    }
}
