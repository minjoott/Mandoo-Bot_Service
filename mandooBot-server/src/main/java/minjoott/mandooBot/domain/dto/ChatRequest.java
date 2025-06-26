package minjoott.mandooBot.domain.dto;

import lombok.*;
import minjoott.mandooBot.domain.vo.RequestMessageVo;

@Data
@AllArgsConstructor
@Builder
public class ChatRequest {
    private String room;
    private String sender;
    private String msg;
    private Boolean isGroupChat;

    /** DTO → VO 변환 */
    public RequestMessageVo toVo() {
        return RequestMessageVo.builder()
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .isGroupChat(this.isGroupChat)
                .build();
    }
}

