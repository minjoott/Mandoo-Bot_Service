package minjoott.mandooBot.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class SavedMessageVo {
    private Long id;
    private LocalDateTime dateTime;
    private boolean isGroupChat;
    private String room;
    private String sender;
    private String msg;

    public ChatHistoryVo toChatHistory(String reply) {
        return ChatHistoryVo.builder()
                .dateTime(this.dateTime.toString())
                .user(this.sender)
                .query(this.msg)
                .reply(reply)
                .build();
    }
}
