package woozlabs.echo.domain.gmail.dto.message;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.Data;
import woozlabs.echo.domain.gmail.dto.thread.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static woozlabs.echo.global.constant.GlobalConstant.*;
import static woozlabs.echo.global.constant.GlobalConstant.MESSAGE_PAYLOAD_HEADER_MESSAGE_ID_KEY;
import static woozlabs.echo.global.utils.GlobalUtility.splitCcAndBcc;
import static woozlabs.echo.global.utils.GlobalUtility.splitSenderData;

@Data
public class GmailReferenceExtraction {
    private String messageId;
    private List<String> references;

    public void setGmailReferenceExtraction(Message message, Map<String, String> messageIdMapping) {
        MessagePart payload = message.getPayload();
        List<MessagePartHeader> headers = payload.getHeaders(); // parsing header
        for(MessagePartHeader header: headers) {
            switch (header.getName().toUpperCase()) {
                case MESSAGE_PAYLOAD_HEADER_REFERENCE_KEY -> {
                    String references = header.getValue();
                    this.references = Arrays.asList(references.split(" "));
                }case MESSAGE_PAYLOAD_HEADER_MESSAGE_ID_KEY -> {
                    String messageId = header.getValue();
                    messageIdMapping.put(messageId, message.getId());
                }
            }
        }
        this.messageId = message.getId();
    }

    public void convertMessageIdInReference(Map<String, String> messageIdMapping){
        for(int idx = 0;idx < references.size();idx++){
            String originMessageId = references.get(idx);
            this.references.set(idx, messageIdMapping.get(originMessageId));
        }
    }
}