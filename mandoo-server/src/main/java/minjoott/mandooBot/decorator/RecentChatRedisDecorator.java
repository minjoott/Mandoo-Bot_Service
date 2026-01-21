package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.dto.RedisChatTurn;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class RecentChatRedisDecorator {

    /** 최근 컨텍스트 윈도우 */
    private static final Duration WINDOW = Duration.ofHours(24);

    /** 최대 보관 턴 수 */
    @Value("${chat.history.max-turns}")
    private int maxTurns;

    /**
     * 방이 완전히 멈추면 키 통째로 정리되게 하는 TTL (WINDOW + α)
     * - push/load가 더 이상 안 오면 TTL만 줄어들어 결국 키가 사라짐
     * - WINDOW(24h)보다 살짝 크게(예: 25h) 잡아 안전 마진
     */
    private static final Duration KEY_TTL = Duration.ofHours(25);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private String key(String roomId) {
        return "chat:turns:zset:" + roomId;
    }

    /** room의 최근 대화 로드: "최근 24시간" AND "최신 MAX_TURNS개" */
    public List<ChatTurnVo> loadRecentChats(String roomId) {
        String k = key(roomId);

        long now = System.currentTimeMillis();
        long min = now - WINDOW.toMillis();

        // 1) 24시간 지난 데이터 정리 (읽을 때도 한 번 정리해두면 안정적)
        redisTemplate.opsForZSet().removeRangeByScore(k, 0, min - 1);

        // 2) 24시간 범위 중 최신 MAX_TURNS개만 조회 (오래된 것부터)
        Set<String> jsonSet = redisTemplate.opsForZSet().rangeByScore(k, min, now, 0, maxTurns);

        if (jsonSet.isEmpty()) return List.of();

        return jsonSet.stream()
                .map(this::readTurn)
                .map(RedisChatTurn::toVo)
                .toList();
    }

    /** room에 대화 턴 추가 + 24시간/개수 기준으로 정리 */
    public void pushTurn(String roomId, RedisChatTurn turn) {
        String k = key(roomId);

        long now = System.currentTimeMillis();
        long min = now - WINDOW.toMillis();

        // 0) 직렬화
        String json = writeTurn(turn);

        // 1) ZSET 추가 (score=시간)
        redisTemplate.opsForZSet().add(k, json, now);

        // 2) 24시간 지난 데이터 삭제
        redisTemplate.opsForZSet().removeRangeByScore(k, 0, min - 1);

        // 3) 개수 제한: 최신 MAX_TURNS개 초과분은 "오래된 것부터" 제거
        long size = redisTemplate.opsForZSet().zCard(k);
        if (size > maxTurns) {
            long endIndexToRemove = size - maxTurns - 1; // 0..endIndexToRemove 제거
            redisTemplate.opsForZSet().removeRange(k, 0, endIndexToRemove);
        }

        // 4) 방이 멈추면 키 통째로 정리되게 TTL 갱신
        redisTemplate.expire(k, KEY_TTL);
    }

    private String writeTurn(RedisChatTurn turn) {
        try {
            return objectMapper.writeValueAsString(turn);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize RedisChatTurn", e);
        }
    }

    private RedisChatTurn readTurn(String json) {
        try {
            return objectMapper.readValue(json, RedisChatTurn.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize RedisChatTurn", e);
        }
    }
}
