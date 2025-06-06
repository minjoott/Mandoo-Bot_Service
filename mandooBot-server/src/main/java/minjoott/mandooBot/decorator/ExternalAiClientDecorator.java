package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAiClientDecorator {

    private final OpenAiChatModel openAiChatModel;
    private final EmbeddingModel embeddingModel;

    private final ObjectMapper objectMapper;

    public float[] createEmbedding(String query) {
        return embeddingModel.embed(query);
    }

    public String createReplyByGpt(Prompt prompt) {
        String reply = callGptApi(prompt);
        log.info("\nGPT가 생성한 reply={} with prompt={}", reply, prompt.getInstructions());
        return reply;
    }

    public List<RagContextMessageVo> getFilteredRagContextByGpt(Prompt prompt) {
        // 4) GPT 호출 → JSON 배열 형태(메시지 텍스트 목록)로 응답받음

        String rawResponse = callGptApi(prompt);

        // 5) '[' ~ ']' 사이 JSON 배열 내용만 잘라내기
        int startIdx = rawResponse.indexOf('[');
        int endIdx   = rawResponse.lastIndexOf(']');
        if (startIdx < 0 || endIdx < 0 || endIdx <= startIdx) {
            // JSON 구간을 찾지 못하면 빈 리스트 반환
            return Collections.emptyList();
        }
        String justJson = rawResponse.substring(startIdx, endIdx + 1).trim();

        // 6) Jackson으로 파싱
        try {
            List<RagContextMessageVo> messageVos = objectMapper.readValue(
                    justJson,
                    new TypeReference<List<RagContextMessageVo>>() {}
            );
            log.info("GPT로 필터링한 RAG Context 메시지 개수={}", messageVos.size());
            return messageVos;
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String callGptApi(Prompt prompt) {
        ChatResponse response = openAiChatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}

