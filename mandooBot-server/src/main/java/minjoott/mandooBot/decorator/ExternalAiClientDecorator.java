package minjoott.mandooBot.decorator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAiClientDecorator {

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final ObjectMapper objectMapper;

    public float[] getEmbedding(String query) {
        return openAiEmbeddingModel.embed(query);
    }

    public String getRagContextDecisionByGpt(Prompt prompt) {
        String reply = callGptApi(prompt);
        log.info("\nGPT가 판단한 RAG 컨텍스트 필요 유무 = {}", reply);
        return reply;
    }

    public String getReplyByGpt(Prompt prompt) {
        String reply = callGptApi(prompt);
        String cleanReply = reply.replaceAll("\\r?\\n", " ");
        log.info("\nGPT가 생성한 reply = {} with prompt = {}", cleanReply, prompt.getInstructions());
        return reply;
    }

    public List<RagContextMessageVo> getFilteredRagContextByGpt(Prompt prompt) {

        String rawResponse = callGptApi(prompt);

        //'[' ~ ']' 사이에 있는 JSON 배열 부분만 잘라내기
        int startIdx = rawResponse.indexOf('[');
        int endIdx = rawResponse.lastIndexOf(']');
        String justJson = rawResponse.substring(startIdx, endIdx + 1).trim();

        // 3) Jackson 객체 매퍼로 JSON 파싱하여 List<RagContextMessageVo>로 변환
        try {
            List<RagContextMessageVo> messageVos = objectMapper.readValue(
                    justJson, new TypeReference<List<RagContextMessageVo>>() {}
            );
            log.info("\nGPT로 필터링한 RAG 컨텍스트 메시지 개수 = {}", messageVos.size());
            return messageVos;
        } catch (Exception e) {
            log.error("\nJSON 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String callGptApi(Prompt prompt) {
        ChatResponse response = openAiChatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}

