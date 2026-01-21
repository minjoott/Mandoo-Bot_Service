package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.config.OpenAiOptions;
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
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExternalAiClientDecorator {

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final ObjectMapper objectMapper;

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
            List<RagContextMessageVo> messageVos = objectMapper.readValue(
                    justJson, new TypeReference<List<RagContextMessageVo>>() {}
            );
            String assembledMessageVos = messageVos.stream()
                    .map(vo -> String.format("%s [%s] \"%s\"",
                            vo.getDateTime(), vo.getSender(), vo.getMsg().replaceAll("\\r?\\n", " ")
                    ))
                    .collect(Collectors.joining("\n"));
            return messageVos;
        } catch (Exception e) {
            log.error("\n⛔️ JSON 파싱 실패: {}", e.getMessage());
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

