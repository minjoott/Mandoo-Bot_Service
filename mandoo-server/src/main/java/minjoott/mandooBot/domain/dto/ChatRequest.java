package minjoott.mandooBot.domain.dto;

import lombok.*;
import minjoott.mandooBot.domain.vo.MessageVo;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class ChatRequest {

    @NotNull
    private String room;

    @NotNull
    private String sender;

    @NotNull
    private String msg;

    @NotNull
    private Boolean isGroupChat;

    public MessageVo toVo() {
        return MessageVo.builder()
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .isGroupChat(this.isGroupChat)
                .build();
    }
}

