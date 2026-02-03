package minjoott.mandooBot.decorator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import minjoott.mandooBot.domain.dto.RedisChatTurn;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RequiredArgsConstructor
@Component
public class RecentChatRedisDecorator {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 최근 컨텍스트 윈도우 */
    private static final Duration WINDOW = Duration.ofHours(24);

    /** 최대 보관 턴 수 */
    @Value("${chat.history.max-turns}") private int maxTurns;

    @Value("${chat.history.decision-max-turns}") private int decisionMaxTurns;

    /**
     * 방이 완전히 멈추면 키 통째로 정리되게 하는 TTL (WINDOW + α)
     * - push/load가 더 이상 안 오면 TTL만 줄어들어 결국 키가 사라짐
     * - WINDOW(24h)보다 살짝 크게(예: 25h) 잡아 안전 마진
     */
    private static final Duration KEY_TTL = Duration.ofHours(25);

    private String key(String roomId) {
        return "chat:turns:zset:" + roomId;
    }

    /** room의 최근 대화 로드: "최근 24시간" AND "최신 limit개" */
    public List<ChatTurnVo> loadRecentChats(String roomId, int limit) {
        String k = key(roomId);

        long now = System.currentTimeMillis();
        long min = now - WINDOW.toMillis();

        // 1) 24시간 지난 데이터 정리
        redisTemplate.opsForZSet().removeRangeByScore(k, 0, min - 1);

        // 2) 최신 limit개 조회 (최신 → 과거)
        Set<String> jsonSet =
                redisTemplate.opsForZSet().reverseRangeByScore(k, min, now, 0, limit);

        if (jsonSet == null || jsonSet.isEmpty()) return List.of();

        List<ChatTurnVo> list = new ArrayList<>(jsonSet.stream()
                .map(this::readTurn)
                .map(RedisChatTurn::toVo)
                .toList()
        );
        Collections.reverse(list);
        return list;
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
