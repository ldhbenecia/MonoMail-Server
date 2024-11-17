package woozlabs.echo.domain.feedback.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import woozlabs.echo.domain.feedback.dto.FeedbackRequestDto;
import woozlabs.echo.domain.feedback.service.FeedbackService;
import woozlabs.echo.global.constant.GlobalConstant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/echo/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping(value = "/create", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> createFeedback(HttpServletRequest httpServletRequest,
                                               @RequestPart("feedback") FeedbackRequestDto feedbackRequestDto,
                                               @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
        feedbackService.createFeedback(uid, feedbackRequestDto, attachment);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{feedbackId}/resolve")
    public ResponseEntity<Void> resolveFeedback(@PathVariable("feedbackId") String feedbackId) {
        feedbackService.resolvedFeedback(feedbackId);
        return ResponseEntity.ok().build();
    }
}
