package woozlabs.echo.domain.feedback.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import woozlabs.echo.domain.feedback.Feedback;
import woozlabs.echo.domain.feedback.dto.FeedbackRequestDto;
import woozlabs.echo.domain.member.entity.Member;
import woozlabs.echo.domain.member.repository.MemberRepository;
import woozlabs.echo.global.exception.CustomErrorException;
import woozlabs.echo.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final MemberRepository memberRepository;
    private final Firestore firestore;
    private final Bucket bucket;

    public void createFeedback(String primaryUid, FeedbackRequestDto feedbackRequestDto, MultipartFile attachment) {
        Member member = memberRepository.findByPrimaryUid(primaryUid)
                .orElseThrow(() -> new CustomErrorException(ErrorCode.NOT_FOUND_MEMBER));

        try {
            String attachmentUrl = null;
            if (attachment != null) {
                String fileName = "feedback/" + System.currentTimeMillis() + "-" + attachment.getOriginalFilename();
                BlobInfo blobInfo = bucket.create(fileName, attachment.getBytes(), attachment.getContentType());
                attachmentUrl = blobInfo.getMediaLink(); // 업로드된 파일 URL
            }

            Feedback feedback = new Feedback();
            feedback.setCategory(feedbackRequestDto.getCategory());
            feedback.setContent(feedbackRequestDto.getContent());
            feedback.setAttachmentUrl(attachmentUrl);  // 첨부파일 URL 설정
            feedback.setAuthor(member.getMemberName());
            feedback.setCreatedAt(new Date());
            feedback.setResolved(false);

            ApiFuture<DocumentReference> future = firestore.collection("feedbacks").add(feedback);
            DocumentReference documentReference = future.get();
            feedback.setId(documentReference.getId()); // Firestore ID 저장
        } catch (Exception e) {
            throw new CustomErrorException(ErrorCode.FIREBASE_CREATE_FEEDBACK_ERROR, e.getMessage());
        }
    }
}
