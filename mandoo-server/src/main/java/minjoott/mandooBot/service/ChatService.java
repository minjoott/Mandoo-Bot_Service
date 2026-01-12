package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RecentChatRedisDecorator recentChatRedisDecorator;

    public ChatResponse handleChat(MessageVo message) {
        boolean hasReply = needsReply(message);
        float[] embedding = ragRepositoryDecorator.generateEmbedding(message.getMsg());
        String reply = needsReply(message) ? generateReply(message, embedding) : null;
        saveChat(message, embedding, reply);
        return new ChatResponse(hasReply, reply);
    }

    private boolean needsReply(MessageVo message) {
        return !message.isGroupChat() || message.getMsg().startsWith("만두야");
    }

    private String generateReply(MessageVo message, float[] embedding) {
        List<ChatTurnVo> recentChatTurns = recentChatRedisDecorator.loadRecentChats(message.getRoom());

        List<RagContextMessageVo> ragContext = getRagContextIfNeeded(message, embedding);

        Prompt prompt = ragContext.isEmpty()
                ? promptService.buildReplyPrompt(message, recentChatTurns)
                : promptService.buildReplyPrompt(message, recentChatTurns, ragContext);

        return externalAiClientDecorator.getReply(prompt);
    }

    private List<RagContextMessageVo> getRagContextIfNeeded(MessageVo message, float[] embedding) {
        if (!needsRag(message.getMsg())) return List.of();

        List<RagContextMessageVo> candidates = ragRepositoryDecorator.findMessagesWithEmbedding(message, embedding);
        if (candidates.isEmpty()) return List.of();

        Prompt filterPrompt = promptService.buildRagContextFilterPrompt(message.getSender(), message.getMsg(), candidates);
        return externalAiClientDecorator.getFilteredRagContext(filterPrompt);
    }

    private boolean needsRag(String query) {
        Prompt prompt = promptService.buildRagContextDecisionPrompt(query);
        boolean decision = externalAiClientDecorator.getRagContextDecision(prompt);
        log.info("\n🧠 RAG 컨텍스트 필요 유무 결정 ⮕ decision = {} | msg = \"{}\"", decision, query);
        return decision;
    }

    private void saveChat(MessageVo message, float[] embedding, String reply) {
        ragRepositoryDecorator.saveMessageWithEmbedding(message, embedding);
        recentChatRedisDecorator.pushTurn(message.getRoom(), message.toRedisChatTurn(reply));
        printLog(message, reply);
    }

    private void printLog(MessageVo message, String reply) {
        if (message.isGroupChat()) {
            log.info("\n📱 (n:1) [{}] {} 요청 메시지 = \"{}\"\n🤖 만두봇 답변 = \"{}\"",
                    message.getRoom(),
                    message.getSender(),
                    message.getMsg().replaceAll("\\r?\\n", " "),
                    reply.replaceAll("\\r?\\n", " "));
        } else {
            log.info("\n📱 (1:1) {}의 메시지 = \"{}\"\n🤖 만두봇 답변 = \"{}\"",
                    message.getSender(),
                    message.getMsg().replaceAll("\\r?\\n", " "),
                    reply.replaceAll("\\r?\\n", " "));
        }
    }
}
