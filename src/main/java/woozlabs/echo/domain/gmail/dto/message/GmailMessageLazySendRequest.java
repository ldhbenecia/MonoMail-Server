package woozlabs.echo.domain.gmail.dto.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailMessageLazySendRequest {
    private String toEmailAddresses;
    private String ccEmailAddresses;
    private String bccEmailAddresses;
    private String subject;
    private String body;
    @JsonIgnore
    private List<MultipartFile> files;
    private List<String> originalFileNames;
    private List<String> encodedFiles;
    private String type;
    private String messageId;
    public void encodeFiles() {// Check if files is not null
        this.encodedFiles = this.files.stream()
                .map(file -> {
                    try {
                        return Base64.getEncoder().encodeToString(file.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException("Error encoding file", e);
                    }
                })
                .collect(Collectors.toList());
    }

    // Base64로 인코딩된 파일을 디코딩하여 byte[] 배열로 변환하는 메서드
    public List<byte[]> decodeFiles() {
        return encodedFiles.stream()
                .map(encoded -> Base64.getDecoder().decode(encoded))
                .collect(Collectors.toList());
    }
}