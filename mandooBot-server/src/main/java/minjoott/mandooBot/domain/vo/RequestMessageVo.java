package minjoott.mandooBot.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import minjoott.mandooBot.domain.entity.Message;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class RequestMessageVo {
    private String room;
    private String sender;
    private String msg;
    private boolean isGroupChat;

    @Builder.Default
    private LocalDateTime dateTime = LocalDateTime.now();

    /** VO → Entity 변환 메서드 */
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
}
