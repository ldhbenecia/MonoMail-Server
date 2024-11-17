package woozlabs.echo.domain.feedback;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.Date;
import lombok.Data;

@Data
public class Feedback {

    @DocumentId
    private String id;
    
    private String category;
    private String content;
    private String attachmentUrl;
    private String author;
    private Date createdAt;
    private boolean resolved;
    private Date resolvedTime;
}
