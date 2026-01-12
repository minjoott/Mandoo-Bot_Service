package minjoott.mandooBot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import minjoott.mandooBot.domain.dto.RedisChatTurn;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, RedisChatTurn> chatTurnRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, RedisChatTurn> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key: String
        template.setKeySerializer(new StringRedisSerializer());

        // Value: JSON
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<RedisChatTurn> valueSerializer =
                new Jackson2JsonRedisSerializer<>(RedisChatTurn.class);
        valueSerializer.setObjectMapper(mapper);

        template.setValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}