package minjoott.mandooBot.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import minjoott.mandooBot.domain.vo.MessageVo;

@Data
@AllArgsConstructor
@Builder
public class ChatRequest {
    private String room;
    private String sender;
    private String msg;
    private Boolean isGroupChat;

    /** DTO → VO 변환 */
    public MessageVo toVo() {
        return MessageVo.builder()
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .isGroupChat(this.isGroupChat)
                .build();
    }
}

