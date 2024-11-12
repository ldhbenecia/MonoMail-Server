package woozlabs.echo.domain.echo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import woozlabs.echo.domain.echo.dto.emailTemplate.EmailTemplateRequest;
import woozlabs.echo.domain.echo.dto.emailTemplate.EmailTemplateResponse;
import woozlabs.echo.domain.echo.entity.EmailTemplate;
import woozlabs.echo.domain.echo.repository.EmailTemplateRepository;
import woozlabs.echo.domain.member.entity.Account;
import woozlabs.echo.domain.member.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    // Mock 객체 생성
    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    // Mock 객체 생성
    @Mock
    private AccountRepository accountRepository;

    // 생성한 위의 Mock 객체들을 EmailTemplateService에 주입
    @InjectMocks
    private EmailTemplateService emailTemplateService;

    private Account account;
    private EmailTemplate emailTemplate;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUid("1234567891");

        emailTemplate = new EmailTemplate();
        emailTemplate.setId(1L);
        emailTemplate.setTemplateName("Template1");
        emailTemplate.setSubject("Subject1");
        emailTemplate.setBody("Body1");
        emailTemplate.setAccount(account);
    }

    @Test
    public void getAllTemplatesTest() throws Exception {
        // given
        List<EmailTemplate> templates = new ArrayList<>();
        templates.add(emailTemplate);

        // when
        when(accountRepository.findByUid("1234567891")).thenReturn(Optional.of(account));
        when(emailTemplateRepository.findByAccount(account)).thenReturn(templates);

        // then
        List<EmailTemplateResponse> responses = emailTemplateService.getAllTemplates("1234567891");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Template1", responses.get(0).getTemplateName());
        assertEquals("Subject1", responses.get(0).getSubject());
        assertEquals("Body1", responses.get(0).getBody());
        assertEquals(1L, responses.get(0).getKey());
    }

    @Test
    public void createTemplateTest() {
        // given
        EmailTemplateRequest request = new EmailTemplateRequest();
        request.setTemplateName("New Template");
        request.setSubject("New Subject");
        request.setBody("New Body");

        when(accountRepository.findByUid("1234567891")).thenReturn(Optional.of(account));

        // when
        emailTemplateService.createTemplate("1234567891", request);

        // then
        verify(emailTemplateRepository, times(1)).save(argThat(template ->
                template.getTemplateName().equals("New Template") &&
                        template.getSubject().equals("New Subject") &&
                        template.getBody().equals("New Body") &&
                        template.getAccount().equals(account)
        ));
    }

    @Test
    public void updateTemplateTest() throws Exception {
        // given
        EmailTemplateRequest request = new EmailTemplateRequest();
        request.setTemplateName("Updated Template");
        request.setSubject("Updated Subject");
        request.setBody("Updated Body");

        EmailTemplate existingTemplate = new EmailTemplate();
        existingTemplate.setId(3L);
        existingTemplate.setTemplateName("Old Template");
        existingTemplate.setSubject("Old Subject");
        existingTemplate.setBody("Old Body");
        existingTemplate.setAccount(account);

        // when
        when(accountRepository.findByUid("1234567891")).thenReturn(Optional.of(account));
        when(emailTemplateRepository.findById(3L)).thenReturn(Optional.of(existingTemplate));

        emailTemplateService.updateTemplate("1234567891", 3L, request);

        // then
        verify(emailTemplateRepository, times(1)).save(argThat(template ->
                template.getTemplateName().equals("Updated Template") &&
                        template.getSubject().equals("Updated Subject") &&
                        template.getBody().equals("Updated Body") &&
                        template.getAccount().equals(account)
        ));
    }

    @Test
    public void deleteTemplateTest() throws Exception {
        // given
        Long templateId = 4L;

        // 변경을 위한 이미 존재하는 emailTemplate 생성
        EmailTemplate existingTemplate = new EmailTemplate();
        existingTemplate.setId(templateId); // templateId와 동일하게 설정
        existingTemplate.setTemplateName("Old Template");
        existingTemplate.setSubject("Old Subject");
        existingTemplate.setBody("Old Body");
        existingTemplate.setAccount(account);

        when(accountRepository.findByUid("1234567891")).thenReturn(Optional.of(account));
        when(emailTemplateRepository.findById(templateId)).thenReturn(Optional.of(existingTemplate));

        // when
        emailTemplateService.deleteTemplate("1234567891", templateId);

        // then
        verify(emailTemplateRepository, times(1)).delete(existingTemplate);
    }
}