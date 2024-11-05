package woozlabs.echo.domain.signature.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import woozlabs.echo.domain.signature.dto.SignatureResponseDto;
import woozlabs.echo.domain.signature.service.SignatureService;

@RestController
@RequestMapping("/api/v1/echo")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    @GetMapping("/signature")
    public ResponseEntity<SignatureResponseDto> getSignatures(HttpServletRequest httpServletRequest,
                                                              @RequestParam(value = "aAUid", required = false) String activeAccountUid) {
        boolean isDirectAccountRequest = (activeAccountUid != null);
        String uid = isDirectAccountRequest ? activeAccountUid : (String) httpServletRequest.getAttribute("uid");

        SignatureResponseDto response = signatureService.getSignatures(uid, isDirectAccountRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/signature/{signatureId}")
    public ResponseEntity<Void> deleteSignature(HttpServletRequest httpServletRequest,
                                                @PathVariable("signatureId") Long signatureId,
                                                @RequestParam(value = "aAUid", required = false) String activeAccountUid) {

        boolean isDirectAccountRequest = (activeAccountUid != null);
        String uid = isDirectAccountRequest ? activeAccountUid : (String) httpServletRequest.getAttribute("uid");

        signatureService.deleteSignature(uid, signatureId, isDirectAccountRequest);
        return ResponseEntity.noContent().build();
    }
}
