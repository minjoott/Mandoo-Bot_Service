package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.decorator.ChatHistoryDecorator;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.domain.dto.ChatResponse;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import minjoott.mandooBot.domain.vo.RequestMessageVo;
import minjoott.mandooBot.domain.vo.SavedMessageVo;
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
    private final ChatHistoryDecorator chatHistoryDecorator;

    public ChatResponse handleChat(RequestMessageVo messageVo) {
        SavedMessageVo savedMessageVo = ragRepositoryDecorator.saveMessageWithEmbedding(messageVo);

        if (!needsReply(messageVo)) {
            return ChatResponse.noReply();
        }

        String reply = generateReplyAndSaveChatHistory(savedMessageVo);
        return ChatResponse.withReply(reply);
    }

    private boolean needsReply(RequestMessageVo messageVo) {
        return !messageVo.isGroupChat() || messageVo.getMsg().startsWith("만두야");
    }

    private String generateReplyAndSaveChatHistory(SavedMessageVo messageVo) {
        List<ChatHistoryVo> recentChatHistoryVos = chatHistoryDecorator.findRecentChatHistory(messageVo.getRoom());
        Prompt prompt = needsRagContext(messageVo.getMsg())
                ? promptService.buildRagReplyPrompt(messageVo, recentChatHistoryVos)
                : promptService.buildSimpleReplyPrompt(messageVo, recentChatHistoryVos);
        String reply = externalAiClientDecorator.getReply(prompt);
        chatHistoryDecorator.saveChatHistory(messageVo.getRoom(), messageVo.toChatHistory(reply));
        printLog(messageVo, reply);
        return reply;
    }

    private void printLog(SavedMessageVo messageVo, String reply) {
        if (messageVo.isGroupChat()) {
            log.info("\n📱 (n:1) [{}] {} 요청 메시지 = \"{}\"\n🤖 만두봇 답변 = \"{}\"",
                    messageVo.getRoom(),
                    messageVo.getSender(),
                    messageVo.getMsg().replaceAll("\\r?\\n", " "),
                    reply.replaceAll("\\r?\\n", " "));
        } else {
            log.info("\n📱 (1:1) {}의 메시지 = \"{}\"\n🤖 만두봇 답변 = \"{}\"",
                    messageVo.getSender(),
                    messageVo.getMsg().replaceAll("\\r?\\n", " "),
                    reply.replaceAll("\\r?\\n", " "));
        }
    }

    private boolean needsRagContext(String query) {
        Prompt prompt = promptService.buildRagContextDecisionPrompt(query);
        boolean decision = externalAiClientDecorator.getRagContextDecision(prompt);
        log.info("\n🧠 RAG 컨텍스트 필요 유무 결정 ⮕ decision = {} | msg = \"{}\"", decision, query);
        return decision;
    }
}
