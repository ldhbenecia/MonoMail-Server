package woozlabs.echo.domain.gmail.dto.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import woozlabs.echo.domain.gmail.dto.pubsub.HistoryType;

@Getter
public enum SendType {
    @JsonProperty
    NORMAL("normal"),
    REPLY("reply"),
    FORWARD("forward");

    private final String value;

    SendType(String value) {
        this.value = value;
    }

    @JsonCreator
    public SendType deserializerSendType(String value){
        for(SendType sendType : SendType.values()){
            if(sendType.getValue().equals(value)){
                return sendType;
            }
        }
        return null;
    }
    @JsonValue
    public String serializerHistoryType(){
        return value;
    }
}
