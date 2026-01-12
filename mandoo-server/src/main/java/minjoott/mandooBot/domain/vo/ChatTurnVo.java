package minjoott.mandooBot.domain.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import minjoott.mandooBot.domain.dto.RedisChatTurn;

@Getter
@Builder
public class ChatTurnVo {

    @NonNull
    private final String dateTime;

    @NonNull
    private final String user;

    @NonNull
    private final String query;

    private final String reply;

    public RedisChatTurn toRedis() {
        return RedisChatTurn.builder()
                .dateTime(this.getDateTime())
                .user(this.getUser())
                .query(this.getQuery())
                .reply(this.getReply())
                .build();
    }

}

