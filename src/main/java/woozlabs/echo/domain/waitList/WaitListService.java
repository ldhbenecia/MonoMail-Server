package woozlabs.echo.domain.waitList;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class WaitListService {

    private final Firestore firestore;

    @Autowired
    public WaitListService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void addWaitList(String email) {
        if (EmailValidator.isValidEmail(email)) {
            WaitList waitList = new WaitList(email);
            waitList.setCreatedAt(new Date());

            ApiFuture<DocumentReference> future = firestore.collection("waitList").add(waitList);

            try {
                DocumentReference docRef = future.get();
                log.info("Email added to waitList with ID: " + docRef.getId());
            } catch (Exception e) {
                log.error("Error adding email to waitList: " + e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding email to waitList");
            }
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

}
