package minjoott.mandooBot.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import minjoott.mandooBot.domain.dto.RedisChatTurn;
import minjoott.mandooBot.domain.entity.Message;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MessageVo {

    private Long id;  //저장 이후에만 존재
    private String room;
    private String sender;
    private String msg;
    private boolean isGroupChat;

    @Builder.Default
    private LocalDateTime dateTime = LocalDateTime.now();

    public Message toEntity(float[] embedding) {
        return Message.builder()
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .isGroupChat(this.isGroupChat)
                .dateTime(this.dateTime)
                .embedding(embedding)
                .build();
    }

    public RedisChatTurn toRedisChatTurn(String reply) {
        return RedisChatTurn.builder()
                .dateTime(this.dateTime.toString())
                .user(this.sender)
                .query(this.msg)
                .reply(reply)
                .build();
    }
}