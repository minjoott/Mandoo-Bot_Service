package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.config.OpenAiOptions;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import minjoott.mandooBot.domain.vo.SavedMessageVo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PromptService {

    private final RagRepositoryDecorator ragRepositoryDecorator;
    private final ExternalAiClientDecorator externalAiClientDecorator;

    public Prompt buildRagContextDecisionPrompt(String query) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.RAG_CONTEXT_DECISION_SYSTEM_TEMPLATE.format(query));
        UserMessage userMessage = new UserMessage("사용자 메시지: " + query);
        return new Prompt(List.of(systemMessage, userMessage), OpenAiOptions.RAG_CONTEXT_DECISION);
    }

    public Prompt buildRagReplyPrompt(SavedMessageVo messageVo, List<ChatHistoryVo> recentChatHistoryVos) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.RAG_REPLY_SYSTEM_TEMPLATE.format(messageVo.getSender(), messageVo.getMsg()));

        List<RagContextMessageVo> ragContextMessageVos = ragRepositoryDecorator.findMessagesWithEmbedding(messageVo);
        List<RagContextMessageVo> filteredRagContextMessageVos = ragContextMessageVos.isEmpty()
                ? Collections.emptyList()
                : externalAiClientDecorator.getFilteredRagContext(
                        buildRagContextFilterPrompt(messageVo.getSender(), messageVo.getMsg(), ragContextMessageVos)
                );
        String ragContextSection = assembleRagContextSection(filteredRagContextMessageVos);
        String chatHistorySection = assembleChatHistorySection(recentChatHistoryVos);
        SystemMessage contextMessage = new SystemMessage(ragContextSection + "\n" + chatHistorySection);

        UserMessage userMessage = new UserMessage(messageVo.getSender() + "가 요청한 메시지: " + messageVo.getMsg() + "\n");

        return new Prompt(List.of(systemMessage, contextMessage, userMessage), OpenAiOptions.REPLY);
    }

    public Prompt buildSimpleReplyPrompt(SavedMessageVo messageVo, List<ChatHistoryVo> recentChatHistoryVos) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.SIMPLE_REPLY_SYSTEM_TEMPLATE.format(messageVo.getSender(), messageVo.getMsg()));

        String chatHistorySection = assembleChatHistorySection(recentChatHistoryVos);
        SystemMessage contextMessage = new SystemMessage(chatHistorySection);

        UserMessage userMessage = new UserMessage(messageVo.getSender() + "가 요청한 메시지: " + messageVo.getMsg() + "\n");

        return new Prompt(List.of(systemMessage, contextMessage, userMessage), OpenAiOptions.REPLY);
    }

    private Prompt buildRagContextFilterPrompt(String sender, String query, List<RagContextMessageVo> ragContextMessageVos) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.RAG_CONTEXT_FILTER_SYSTEM_TEMPLATE.format(query, query, query));

        String ragContextSection = assembleRagContextSection(ragContextMessageVos);
        UserMessage userMessage = new UserMessage("\n" + sender + "의 메시지: " + query + "\n" + ragContextSection);

        return new Prompt(List.of(systemMessage, userMessage), OpenAiOptions.RAG_CONTEXT_FILTER);
    }

    private String assembleRagContextSection(List<RagContextMessageVo> ragContextMessageVos) {
        StringBuilder section = new StringBuilder();

        section.append("\n----- 임베딩으로 조회한 과거 대화 내용 (오래된순) -----\n");
        for (RagContextMessageVo m : ragContextMessageVos) {
            String cleanMsg = m.getMsg().replaceAll("\\r?\\n", " ");
            section.append(m.getDateTime())
                    .append(" [").append(m.getSender()).append("] ")
                    .append(cleanMsg)
                    .append("\n");
        }

        return section.toString();
    }

    private String assembleChatHistorySection(List<ChatHistoryVo> recentChatHistoryVos) {
        StringBuilder section = new StringBuilder();

        section.append("\n----- 최근 채팅 이력 (오래된순) -----\n");
        for (ChatHistoryVo h : recentChatHistoryVos) {
            String cleanQuery = h.getQuery().replaceAll("\\r?\\n", " ");
            String cleanReply = h.getReply().replaceAll("\\r?\\n", " ");
            section.append(h.getDateTime())
                    .append(" [").append(h.getUser()).append("] ")
                    .append(cleanQuery)
                    .append(" ⇒ [만두] 답변: ")
                    .append(cleanReply)
                    .append("\n");
        }

        return section.toString();
    }
}
