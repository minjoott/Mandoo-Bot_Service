package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.decorator.BufferRedisDecorator;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.decorator.RecentChatRedisDecorator;
import minjoott.mandooBot.domain.dto.ChatResponse;
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
        if (merged.isBlank()) return new ChatResponse(false, null);

        // ✅ (A) buffer complete decision 단계
        Prompt completePrompt = promptService.buildBufferCompleteDecisionPrompt(merged);
        boolean complete = externalAiClientDecorator.getBufferCompleteDecision(completePrompt).isComplete();

        // 결정 프롬프트 (5개)
        List<ChatTurnVo> decisionRecentChats = recentChatRedisDecorator.loadRecentChatsForNeedsMandooDecision(message.getRoom());

        // complete = X → PASS
        if (!complete) return new ChatResponse(false, null);

        // complete = O → 버퍼 초기화
        bufferRedisDecorator.clearBuffer(message.getRoom(), message.getSender());

        // 완성된 통합본 MessageVo
        MessageVo mergedMessage = message.withCompleteMsg(merged);

        // 4) AI 호출 2: needsMandoo 판단 (최근대화 포함)
        Prompt needsPrompt = promptService.buildNeedsMandooDecisionPrompt(merged, decisionRecentChats);
        boolean hasReply = externalAiClientDecorator.getNeedsMandooDecision(needsPrompt).isNeedsMandoo();

        // 5) 완성본은 무조건 저장해야 하므로 embedding 1회 생성
        float[] embedding = ragRepositoryDecorator.generateEmbedding(mergedMessage.getMsg());

        // 6) needsMandoo = O 인 경우에만 답변 생성
        String reply = hasReply ? generateReply(mergedMessage, embedding) : null;

        // 7) 완성본 DB + 최근대화 Redis 저장
        saveChat(mergedMessage, embedding, reply);

        return new ChatResponse(hasReply, reply);
    }

    private String generateReply(MessageVo message, float[] embedding) {
        Prompt prompt = getPrompt(message, embedding);
        return externalAiClientDecorator.getReply(prompt);
    }

    private Prompt getPrompt(MessageVo message, float[] embedding) {
        // 실제 답변 생성 (30개)
        List<ChatTurnVo> replyRecentChats = recentChatRedisDecorator.loadRecentChats(message.getRoom());

        Prompt prompt;
        if (!needsRag(message.getMsg())) {
            prompt = promptService.buildReplyPrompt(message, replyRecentChats);
        }
        else {
            List<RagContextMessageVo> ragContextMessages = getRagContext(message, embedding);
            prompt = promptService.buildReplyPrompt(message, replyRecentChats, ragContextMessages);
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
