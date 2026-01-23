package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.decorator.BufferRedisDecorator;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.decorator.RecentChatRedisDecorator;
import minjoott.mandooBot.domain.dto.ChatResponse;
import minjoott.mandooBot.domain.dto.RedisBufferDecision;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final PromptService promptService;
    private final RagRepositoryDecorator ragRepositoryDecorator;
    private final ExternalAiClientDecorator externalAiClientDecorator;
    private final BufferRedisDecorator bufferRedisDecorator;
    private final RecentChatRedisDecorator recentChatRedisDecorator;

    public ChatResponse handleChat(MessageVo message) {
        // 1) 매 메시지마다 버퍼에 추가
        bufferRedisDecorator.appendToBuffer(message.getRoom(), message.getSender(), message.getMsg());

        // 2) 버퍼 통합본 생성
        String merged = bufferRedisDecorator.readMergedBuffer(message.getRoom(), message.getSender());
        if (merged.isBlank()) {
            return new ChatResponse(false, null);
        }

        // 3) *1 완성 여부 + *2 만두 필요 여부 판단 (LLM 1회)
        Prompt decisionPrompt = promptService.buildBufferDecisionPrompt(merged);
        RedisBufferDecision decision = externalAiClientDecorator.getBufferDecision(decisionPrompt);

        // complete = X → PASS (응답/저장 없음)
        if (!decision.isComplete()) return new ChatResponse(false, null);

        // complete = O → 버퍼 초기화
        bufferRedisDecorator.clearBuffer(message.getRoom(), message.getSender());

        // 이제부터는 "완성된 통합본"만 다룸
        MessageVo mergedMessage = message.withCompleteMsg(merged);

        // 4) 완성본은 무조건 저장해야 하므로 embedding 1회 생성
        float[] embedding = ragRepositoryDecorator.generateEmbedding(mergedMessage.getMsg());

        // 5) needsMandoo = O 인 경우에만 답변 생성
        boolean hasReply = decision.isNeedsMandoo();
        String reply = hasReply ? generateReply(mergedMessage, embedding) : null;

        // 6) 완성본 DB + 최근대화 Redis 저장
        saveChat(mergedMessage, embedding, reply);

        return new ChatResponse(hasReply, reply);
    }

    private String generateReply(MessageVo message, float[] embedding) {
        Prompt prompt = getPrompt(message, embedding);
        return externalAiClientDecorator.getReply(prompt);
    }

    private Prompt getPrompt(MessageVo message, float[] embedding) {
        List<ChatTurnVo> recentChatTurns = recentChatRedisDecorator.loadRecentChats(message.getRoom());

        Prompt prompt;
        if (!needsRag(message.getMsg())) {
            prompt = promptService.buildReplyPrompt(message, recentChatTurns);
        }
        else {
            List<RagContextMessageVo> ragContextMessages = getRagContext(message, embedding);
            prompt = promptService.buildReplyPrompt(message, recentChatTurns, ragContextMessages);
        }
        return prompt;
    }

    private List<RagContextMessageVo> getRagContext(MessageVo message, float[] embedding) {
        List<RagContextMessageVo> candidates = ragRepositoryDecorator.findMessagesWithEmbedding(message, embedding);
        if (candidates.isEmpty()) return List.of();

        Prompt filterPrompt = promptService.buildRagContextFilterPrompt(message.getSender(), message.getMsg(), candidates);
        return externalAiClientDecorator.getFilteredRagContext(filterPrompt);
    }

    private boolean needsRag(String query) {
        Prompt prompt = promptService.buildRagContextDecisionPrompt(query);
        return externalAiClientDecorator.getRagContextDecision(prompt);
    }

    private void saveChat(MessageVo message, float[] embedding, String reply) {
        ragRepositoryDecorator.saveMessageWithEmbedding(message, embedding);
        recentChatRedisDecorator.pushTurn(message.getRoom(), message.toRedisChatTurn(reply));
    }
}
