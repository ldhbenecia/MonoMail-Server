package woozlabs.echo.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import jakarta.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.sdk.path}")
    private String firebaseSdkPath;

    @Value("${firebase.storage.bucket}")
    private String firebaseStorageBucket;

    @PostConstruct
    public void init() throws IOException {

        try {
            ClassPathResource resource = new ClassPathResource(firebaseSdkPath);
            InputStream serviceAccount = resource.getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket(firebaseStorageBucket)
                    .build();
            FirebaseApp.initializeApp(options);
        } catch (FileNotFoundException e) {
            throw new CustomErrorException(ErrorCode.NOT_FOUND_FIREBASE_SERVICE_ACCOUNT_KEY);
        }
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    @Bean
    public Bucket bucket() {
        return StorageClient.getInstance().bucket();
    }
}
