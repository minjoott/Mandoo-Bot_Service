package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.vo.MessageVo;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${redis.buffer.ttl.minutes}")
    private Duration bufferTtlMinutes;

    public void appendToBuffer(MessageVo message) {
        String k = bufferKey(message.getRoom(), message.getSender());
        redisTemplate.opsForList().rightPush(k, message.getMsg());
        redisTemplate.expire(k, bufferTtlMinutes);
    }

    public String readMergedBuffer(String room, String sender) {
        String k = bufferKey(room, sender);
        List<String> parts = redisTemplate.opsForList().range(k, 0, -1);
        return String.join(" ", parts).trim();
    }

    public void clearBuffer(String roomId, String sender) {
        redisTemplate.delete(bufferKey(roomId, sender));
    }

    private String bufferKey(String roomId, String sender) {
        return "chat:buffer:list:" + roomId + ":" + sender;
    }

}
