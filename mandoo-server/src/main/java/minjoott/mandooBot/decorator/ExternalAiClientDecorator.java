package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.config.OpenAiOptions;
import minjoott.mandooBot.domain.ai.BufferCompleteDecision;
import minjoott.mandooBot.domain.ai.NeedsMandooDecision;
import minjoott.mandooBot.domain.ai.NeedsRagContextDecision;
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

    public BufferCompleteDecision getBufferCompleteDecision(Prompt prompt) {
        String raw = callChatApi(prompt);
        return parseJsonSafely(raw, BufferCompleteDecision.class,
                BufferCompleteDecision.builder().complete("X").build()
        );
    }

    public NeedsMandooDecision getNeedsMandooDecision(Prompt prompt) {
        String raw = callChatApi(prompt);
        return parseJsonSafely(raw, NeedsMandooDecision.class,
                NeedsMandooDecision.builder().needsMandoo("X").build()
        );
    }

    public NeedsRagContextDecision getRagContextDecision(Prompt prompt) {
        String raw = callChatApi(prompt);

        return parseJsonSafely(raw, NeedsRagContextDecision.class,
                NeedsRagContextDecision.builder().needsRagContext("X").build()
        );
    }

    public float[] getEmbedding(String query) {
        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(query), OpenAiOptions.EMBEDDING);
        return callEmbeddingApi(embeddingRequest);
    }

    public List<RagContextMessageVo> getFilteredRagContext(Prompt prompt) {
        String raw = callChatApi(prompt);
        return parseJsonSafely(raw, new TypeReference<List<RagContextMessageVo>>() {});
    }

    public String getReply(Prompt prompt) {
        return callChatApi(prompt);
    }

    private float[] callEmbeddingApi(EmbeddingRequest embeddingRequest) {
        EmbeddingResponse response = openAiEmbeddingModel.call(embeddingRequest);
        return response.getResult().getOutput();
    }

    private String callChatApi(Prompt prompt) {
        ChatResponse response = openAiChatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    private <T> T parseJsonSafely(String raw, Class<T> clazz, T fallback) {
        int s = raw.indexOf('{');
        int e = raw.lastIndexOf('}');
        if (s < 0 || e < 0 || e <= s) return fallback;
        String json = raw.substring(s, e + 1).trim();

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception ex) {
            log.error("⛔️ Decision JSON 파싱 실패: {}", ex.getMessage());
            return fallback;
        }
    }

    private <T> List<T> parseJsonSafely(String raw, TypeReference<List<T>> typeRef) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        int startIdx = raw.indexOf('[');
        int endIdx = raw.lastIndexOf(']');
        if (startIdx < 0 || endIdx < 0 || endIdx <= startIdx) {
            return Collections.emptyList();
        }
        String json = raw.substring(startIdx, endIdx + 1).trim();

        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception ex) {
            log.error("⛔️ JSON Array 파싱 실패: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}

