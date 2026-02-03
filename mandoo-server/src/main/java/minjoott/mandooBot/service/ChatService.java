package minjoott.mandooBot.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import minjoott.mandooBot.decorator.BufferRedisDecorator;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.decorator.RecentChatRedisDecorator;
import minjoott.mandooBot.domain.dto.ChatResponse;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final PromptService promptService;
    private final RagRepositoryDecorator ragRepositoryDecorator;
    private final ExternalAiClientDecorator externalAiClientDecorator;
    private final BufferRedisDecorator bufferRedisDecorator;
    private final RecentChatRedisDecorator recentChatRedisDecorator;

    @Value("${chat.history.decision-max-turns}") private int decisionMaxTurns;
    @Value("${chat.history.max-turns}") private int maxTurns;

    public ChatResponse handleChat(MessageVo message) {

        // 1) 버퍼에 누적
        bufferRedisDecorator.appendToBuffer(message);

        // 2) 버퍼 메시지 병합
        String merged = bufferRedisDecorator.readMergedBuffer(message.getRoom(), message.getSender());

        // 3) 병합 메시지가 완성된 발화인지 판단
        Prompt completePrompt = promptService.buildBufferCompleteDecisionPrompt(merged);
        boolean complete = externalAiClientDecorator.getBufferCompleteDecision(completePrompt).isComplete();

        // 4) 완성되지 않은 발화라면, 즉시 return
        if (!complete) return new ChatResponse(false, null);

        // 5) 버퍼 초기화
        MessageVo mergedMessage = message.withCompleteMsg(merged);
        bufferRedisDecorator.clearBuffer(message.getRoom(), message.getSender());

        // 6) 만두 응답 필요 유무 판단
        List<ChatTurnVo> decisionRecentChats = recentChatRedisDecorator.loadRecentChats(message.getRoom(), decisionMaxTurns);
        Prompt needsMandooPrompt = promptService.buildNeedsMandooDecisionPrompt(mergedMessage, decisionRecentChats);
        boolean hasReply = externalAiClientDecorator.getNeedsMandooDecision(needsMandooPrompt).isNeedsMandoo();

        // 7) 완성된 발화 임베딩
        float[] embedding = externalAiClientDecorator.getEmbedding(merged);;

        // 8) 만두 응답 필요 유무에 따라 응답 생성
        String reply = null;
        if (hasReply) {
            // - 최신 대화 이력 조회
            List<ChatTurnVo> replyRecentChats = recentChatRedisDecorator.loadRecentChats(mergedMessage.getRoom(), maxTurns);
            // - 응답 생성
            Prompt replyPrompt = buildReplyPrompt(mergedMessage, embedding, replyRecentChats);
            reply = externalAiClientDecorator.getReply(replyPrompt);
        }

        // 9) Redis 최근 대화 이력 & DB 저장
        ragRepositoryDecorator.saveMessageWithEmbedding(mergedMessage, embedding);
        recentChatRedisDecorator.pushTurn(mergedMessage.getRoom(), mergedMessage.toRedisChatTurn(reply));

        // 10) 응답 return
        return new ChatResponse(hasReply, reply);
    }

    private Prompt buildReplyPrompt(MessageVo message, float[] embedding, List<ChatTurnVo> recentChats) {
        Prompt prompt;
        if (!needsRagContext(message.getMsg())) {
            prompt = promptService.buildReplyPrompt(message, recentChats);
        }
        else {
            List<RagContextMessageVo> ragContextMessages = loadRagContext(message, embedding);
            prompt = promptService.buildReplyPrompt(message, recentChats, ragContextMessages);
        }
        return prompt;
    }

    private List<RagContextMessageVo> loadRagContext(MessageVo message, float[] embedding) {
        // - 임베딩 기반 RAG 컨텍스트 조회
        List<RagContextMessageVo> candidates = ragRepositoryDecorator.findMessagesWithEmbedding(message, embedding);
        if (candidates.isEmpty()) return List.of();

        // - RAG 컨텍스트 필터링
        Prompt filterPrompt = promptService.buildRagContextFilterPrompt(message, candidates);
        return externalAiClientDecorator.getFilteredRagContext(filterPrompt);
    }

    private boolean needsRagContext(String query) {
        // - RAG 파이프라인 필요 유무 판단
        Prompt prompt = promptService.buildRagContextDecisionPrompt(query);
        return externalAiClientDecorator.getRagContextDecision(prompt).isNeedsRagContext();
    }

}
