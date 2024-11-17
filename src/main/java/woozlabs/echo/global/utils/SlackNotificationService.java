package woozlabs.echo.global.utils;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SlackNotificationService {

    @Value("${slack.webhook-waitlist-url}")
    private String slackWebHookWaitListUrl;

    @Value("${slack.webhook-feedback-url}")
    private String slackWebHookFeedbackUrl;

    public void sendSlackNotification(String message, String channel) {
        Slack slack = Slack.getInstance();

        String webhookUrl = getWebhookUrlForChannel(channel);

        Payload payload = Payload.builder()
                .text(message)
                .build();

        try {
            WebhookResponse webhookResponse = slack.send(webhookUrl, payload);

            if (webhookResponse.getCode() == 200) {
                log.info("Slack notification sent successfully.");
            } else {
                log.error("Failed to send Slack notification: " + webhookResponse.getMessage());
            }
        } catch (IOException e) {
            log.error("Error sending Slack notification: ", e);
        }
    }

    private String getWebhookUrlForChannel(String channel) {
        switch (channel) {
            case "mono-waitlist-alert":
                return slackWebHookWaitListUrl;
            case "mono-feedback-alert":
                return slackWebHookFeedbackUrl;
            default:
                throw new IllegalArgumentException("Unknown channel: " + channel);
        }
    }
}
