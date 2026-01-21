package minjoott.mandooBot.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import minjoott.mandooBot.domain.vo.ChatTurnVo;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisChatTurn {

    @NotNull
    private String dateTime;

    @NotNull
    private String user;

    @NotNull
    private String query;

    private String reply;

    public static List<ChatTurnVo> toVoList(List<RedisChatTurn> redisTurns) {
        return redisTurns.stream()
                .map(RedisChatTurn::toVo)
                .toList();
    }

    public ChatTurnVo toVo() {
        return ChatTurnVo.builder()
                .dateTime(this.getDateTime())
                .user(this.getUser())
                .query(this.getQuery())
                .reply(this.getReply())
                .build();
    }
}
