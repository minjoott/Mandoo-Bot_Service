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
