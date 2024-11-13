package woozlabs.echo.domain.waitList;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import java.util.Date;

public class WaitList {

    @DocumentId
    private String id;

    private String email;

    @ServerTimestamp
    private Date createdAt;

    public WaitList() {
    }

    WaitList(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
