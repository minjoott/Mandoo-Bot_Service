package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class BufferRedisDecorator {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration BUFFER_TTL = Duration.ofMinutes(3);

    private String bufferKey(String roomId, String sender) {
        return "chat:buffer:list:" + roomId + ":" + sender;
    }

    public void appendToBuffer(String roomId, String sender, String msg) {
        String k = bufferKey(roomId, sender);
        redisTemplate.opsForList().rightPush(k, msg);
        redisTemplate.expire(k, BUFFER_TTL);
    }

    public String readMergedBuffer(String roomId, String sender) {
        String k = bufferKey(roomId, sender);
        List<String> parts = redisTemplate.opsForList().range(k, 0, -1);
        if (parts == null || parts.isEmpty()) return "";
        return String.join(" ", parts).trim();
    }

    public void clearBuffer(String roomId, String sender) {
        redisTemplate.delete(bufferKey(roomId, sender));
    }

}
