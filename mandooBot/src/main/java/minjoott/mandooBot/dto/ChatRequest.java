package minjoott.mandooBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import minjoott.mandooBot.domain.entity.Message;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    private String room;

    private String sender;

    private String msg;

    @JsonProperty("isGroupChat")
    private boolean isGroupChat;

    /** DTO → Entity 변환 */
    public Message toEntity() {
        return Message.builder()
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .isGroupChat(this.isGroupChat)
                .build();
    }
}

