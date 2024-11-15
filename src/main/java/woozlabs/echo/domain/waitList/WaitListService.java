package woozlabs.echo.domain.waitList;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class WaitListService {

    private final Firestore firestore;

    @Value("${slack.webhook-url}")
    private String slackWebHookUrl;

    @Autowired
    public WaitListService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void addWaitList(String email) throws ExecutionException, InterruptedException {
        if (EmailValidator.isValidEmail(email)) {
            // 이미 대기자 명단에 해당 이메일이 있는지 확인
            ApiFuture<QuerySnapshot> future = firestore.collection("waitList")
                    .whereEqualTo("email", email)
                    .get();

            QuerySnapshot querySnapshot = future.get();
            if (!querySnapshot.isEmpty()) {
                log.error("Email already exists in waitList: " + email);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists in wait list");
            }

            WaitList waitList = new WaitList(email);
            waitList.setCreatedAt(new Date());
            ApiFuture<DocumentReference> addFuture = firestore.collection("waitList").add(waitList);
            DocumentReference docRef = addFuture.get();
            log.info("Email added to waitList with ID: " + docRef.getId());

            sendSlackNotification(email);
        } else {
            log.error("Invalid email address: " + email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address");
        }
    }

    public List<WaitList> getWaitList() {
        List<WaitList> waitList = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = firestore.collection("waitList").get();
        try {
            QuerySnapshot querySnapshot = future.get();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                WaitList waitListEntry = document.toObject(WaitList.class);
                waitList.add(waitListEntry);
            }
            return waitList;
        } catch (Exception e) {
            log.error("Error getting wait list: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting wait list");
        }
    }

    private void sendSlackNotification(String email) {
        Slack slack = Slack.getInstance();
        String message = String.format("*%s* joined the waitlist! :tada:", email);

        Payload payload = Payload.builder()
                .text(message)
                .build();

        try {
            WebhookResponse webhookResponse = slack.send(slackWebHookUrl, payload);

            if (webhookResponse.getCode() == 200) {
                log.info("Slack notification sent successfully.");
            } else {
                log.error("Failed to send Slack notification: " + webhookResponse.getMessage());
            }
        } catch (IOException e) {
            log.error("Error sending Slack notification: ", e);
        }
    }
}
