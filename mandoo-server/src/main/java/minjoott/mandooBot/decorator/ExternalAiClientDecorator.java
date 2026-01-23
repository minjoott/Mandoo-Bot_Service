package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.config.OpenAiOptions;
import minjoott.mandooBot.domain.dto.RedisBufferDecision;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExternalAiClientDecorator {

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final ObjectMapper objectMapper;

    public RedisBufferDecision getBufferDecision(Prompt prompt) {
        String raw = callChatApi(prompt);

        // '{' ~ '}' 사이 JSON만 잘라서 파싱 (모델이 앞뒤 텍스트를 섞어도 방어)
        int s = raw.indexOf('{');
        int e = raw.lastIndexOf('}');
        if (s < 0 || e < 0 || e <= s) {
            return RedisBufferDecision.builder().complete("X").needsMandoo("N").build();
        }

        String json = raw.substring(s, e + 1).trim();
        try {
            return objectMapper.readValue(json, RedisBufferDecision.class);
        } catch (Exception ex) {
            log.error("⛔️ BufferDecision JSON 파싱 실패: {}", ex.getMessage());
            return RedisBufferDecision.builder().complete("X").needsMandoo("N").build();
        }
    }

    public float[] getEmbedding(String query) {
        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(query), OpenAiOptions.EMBEDDING);
        EmbeddingResponse response = openAiEmbeddingModel.call(embeddingRequest);
        return response.getResult().getOutput();
    }

    public boolean getRagContextDecision(Prompt prompt) {
        String decision = callChatApi(prompt);
        return decision.equals("O");
    }

    public List<RagContextMessageVo> getFilteredRagContext(Prompt prompt) {
        String rawResponse = callChatApi(prompt);

        // '[' ~ ']' 사이에 있는 JSON 배열 부분만 잘라내기
        int startIdx = rawResponse.indexOf('[');
        int endIdx = rawResponse.lastIndexOf(']');
        String justJson = rawResponse.substring(startIdx, endIdx + 1).trim();

        // Jackson 객체 매퍼로 JSON 파싱하여 List<RagContextMessageVo>로 변환
        try {
            return objectMapper.readValue(
                    justJson, new TypeReference<List<RagContextMessageVo>>() {}
            );
        } catch (Exception e) {
            log.error("\n⛔️ RagContextMessages JSON 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public String getReply(Prompt prompt) {
        return callChatApi(prompt);
    }

    private String callChatApi(Prompt prompt) {
        ChatResponse response = openAiChatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}

