package woozlabs.echo.domain.echo.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import woozlabs.echo.domain.echo.dto.emailTemplate.EmailTemplateRequest;
import woozlabs.echo.domain.echo.dto.emailTemplate.EmailTemplateResponse;
import woozlabs.echo.domain.echo.service.EmailTemplateService;
import woozlabs.echo.global.constant.GlobalConstant;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/echo")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @GetMapping("/email-templates")
    public ResponseEntity<List<EmailTemplateResponse>> getAllTemplates(HttpServletRequest httpServletRequest) {
        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
        try {
            List<EmailTemplateResponse> emailTemplates = emailTemplateService.getAllTemplates(uid);
            return ResponseEntity.ok(emailTemplates);
        } catch (Exception e) {
            log.error("Error occurred while fetching email templates for user with UID: {}", uid, e);
            throw new CustomErrorException(ErrorCode.FAILED_TO_FETCHING_EMAIL_TEMPLATE, e.getMessage());
        }
    }

    @PostMapping("/email-templates")
    public ResponseEntity<Void> createTemplate(HttpServletRequest httpServletRequest,
                                               @RequestBody EmailTemplateRequest emailTemplateRequest) {
        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
        try {
            emailTemplateService.createTemplate(uid, emailTemplateRequest);
            return ResponseEntity.status(201).build();
        } catch (Exception e) {
            log.error("Error occurred while creating email template for user with UID: {}", uid, e);
            throw new CustomErrorException(ErrorCode.FAILED_TO_CREATE_EMAIL_TEMPLATE, e.getMessage());
        }
    }

    @PutMapping("/email-templates/{templateId}")
    public ResponseEntity<Void> updateTemplate(HttpServletRequest httpServletRequest,
                                               @PathVariable("templateId") Long templateId,
                                               @RequestBody EmailTemplateRequest emailTemplateRequest) {
        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
        try {
            emailTemplateService.updateTemplate(uid, templateId, emailTemplateRequest);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error occurred while updating email template for user with UID: {}", uid, e);
            throw new CustomErrorException(ErrorCode.FAILED_TO_UPDATE_EMAIL_TEMPLATE, e.getMessage());
        }
    }

    @DeleteMapping("/email-templates/{templateId}")
    public ResponseEntity<Void> deleteTemplate(HttpServletRequest httpServletRequest,
                                               @PathVariable("templateId") Long templateId) {
        String uid = (String) httpServletRequest.getAttribute(GlobalConstant.FIREBASE_UID_KEY);
        try {
            emailTemplateService.deleteTemplate(uid, templateId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error occurred while deleting email template for user with UID: {}", uid, e);
            throw new CustomErrorException(ErrorCode.FAILED_TO_DELETE_EMAIL_TEMPLATE, e.getMessage());
        }
    }
}
