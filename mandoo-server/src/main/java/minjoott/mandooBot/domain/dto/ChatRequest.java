package minjoott.mandooBot.domain.dto;

import lombok.*;
import minjoott.mandooBot.domain.vo.RequestMessageVo;

@Data
@AllArgsConstructor
@Builder
public class ChatRequest {
    @NonNull private String room;
    @NonNull private String sender;
    @NonNull private String msg;
    @NonNull private Boolean isGroupChat;

    public RequestMessageVo toVo() {
        return RequestMessageVo.builder()
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .isGroupChat(this.isGroupChat)
                .build();
    }
}

