package minjoott.mandooBot.decorator;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.domain.dto.RedisChatTurn;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RecentChatRedisDecorator {

    @Value("${chat.history.max-turns}")
    private int maxTurns;

    private final RedisTemplate<String, RedisChatTurn> redisTemplate;

    private static final String KEY_PREFIX = "chat:room:";

    private String key(String roomId) {
        return KEY_PREFIX + roomId;
    }

    /** 최근 N개 유지하면서 턴 1개 추가 */
    public void pushTurn(String roomId, RedisChatTurn turn) {
        String k = key(roomId);

        redisTemplate.opsForList().rightPush(k, turn);
        redisTemplate.opsForList().trim(k, -maxTurns, -1);
    }

    /** room의 최근 대화 전체(=현재 Redis에 남아있는 범위) 로드 */
    public List<ChatTurnVo> loadRecentChats(String roomId) {
        String k = key(roomId);

        List<RedisChatTurn> turns = redisTemplate.opsForList().range(k, 0, -1);
        log.info("🔍 최근 대화 이력 ⮕ room={} | count={}", roomId, turns.size());

        return (turns == null) ? List.of() : RedisChatTurn.toVoList(turns);
    }

    /** (선택) 방의 대화 삭제 */
    public void clearRoom(String roomId) {
        redisTemplate.delete(key(roomId));
    }
}