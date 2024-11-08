package woozlabs.echo.domain.preference.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import woozlabs.echo.domain.preference.dto.PreferenceDto;
import woozlabs.echo.domain.preference.dto.UpdatePreferenceRequestDto;
import woozlabs.echo.domain.preference.service.PreferenceService;
import woozlabs.echo.global.constant.GlobalConstant;

@RestController
@RequestMapping("/api/v1/echo")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping("/preference")
    public ResponseEntity<PreferenceDto> getPreferences(HttpServletRequest httpServletRequest) {
        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
        PreferenceDto preference = preferenceService.getPreference(uid);
        return ResponseEntity.ok(preference);
    }

    @PatchMapping("/preference")
    public ResponseEntity<Void> updatePreferences(HttpServletRequest httpServletRequest,
                                                  @RequestBody UpdatePreferenceRequestDto updatePreferenceRequest) {
        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
        preferenceService.updatePreference(uid, updatePreferenceRequest);
        return ResponseEntity.ok().build();
    }
}
