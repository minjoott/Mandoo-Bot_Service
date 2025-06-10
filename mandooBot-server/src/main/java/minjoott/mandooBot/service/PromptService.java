package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final RagRepositoryDecorator ragRepositoryDecorator;
    private final ExternalAiClientDecorator externalAiClientDecorator;

    public Prompt buildRagContextDecisionPrompt(String query) {
        String systemText = PromptTemplate.RAG_CONTEXT_DECISION_SYSTEM_TEMPLATE.format(query);
        SystemMessage systemMessage = new SystemMessage(systemText);
        UserMessage userMessage = new UserMessage("사용자 메시지: " + query);
        return new Prompt(systemMessage, userMessage);
    }

    public Prompt buildReplyPrompt(MessageVo messageVo, String query, List<ChatHistoryVo> recentChatHistoryVo, boolean isRagContextNeeded) {
        Prompt prompt;
        if (isRagContextNeeded) {  // CASE1: RAG 컨텍스트 필요 O
            List<RagContextMessageVo> ragContextMessages = ragRepositoryDecorator.findMessagesWithEmbedding(messageVo);

            boolean hasRagContext = !ragContextMessages.isEmpty();
            List<RagContextMessageVo> filteredRagContextMessageVos;
            if (hasRagContext) {  // 조회된 RAG 컨텍스트를 GTP를 통해 필터링하기
                Prompt filterPrompt = buildRagContextFilterPrompt(query, ragContextMessages);
                filteredRagContextMessageVos = externalAiClientDecorator.getFilteredRagContextByGpt(filterPrompt);
            }
            else {  // 조회된 RAG 컨텍스트가 없다면 빈 리스트 저장
                filteredRagContextMessageVos = Collections.emptyList();
            }

            prompt = buildRagReplyPrompt(messageVo, filteredRagContextMessageVos, recentChatHistoryVo);
        }
        else {  // CASE2: RAG 컨텍스트 필요 X
            prompt = buildSimpleReplyPrompt(messageVo, recentChatHistoryVo);
        }
        return prompt;
    }

    private Prompt buildRagContextFilterPrompt(String query, List<RagContextMessageVo> ragContextMessageVos) {
        String systemText = PromptTemplate.RAG_CONTEXT_FILTER_SYSTEM_TEMPLATE.format(query);
        SystemMessage systemMessage = new SystemMessage(systemText);

        String ragContextSection = assembleRagContextSection(ragContextMessageVos);
        UserMessage userMessage = new UserMessage(ragContextSection + "\n사용자가 요청한 메시지(질문): " + query + "\n");

        return new Prompt(systemMessage, userMessage);
    }

    private Prompt buildRagReplyPrompt(MessageVo messageVo, List<RagContextMessageVo> ragContextMessageVos, List<ChatHistoryVo> recentChatHistoryVos) {
        String sender = messageVo.getSender();
        String query = messageVo.getMsg();

        String systemText = PromptTemplate.RAG_REPLY_SYSTEM_TEMPLATE.format(sender, query);
        SystemMessage systemMessage = new SystemMessage(systemText);

        String ragContextSection = assembleRagContextSection(ragContextMessageVos);
        String chatHistorySection = assembleChatHistorySection(recentChatHistoryVos);
        SystemMessage contextMessage = new SystemMessage(ragContextSection + "\n" + chatHistorySection);

        UserMessage userMessage = new UserMessage(sender + "가 요청한 메시지(질문): " + query + "\n");

        return new Prompt(systemMessage, contextMessage, userMessage);
    }

    private Prompt buildSimpleReplyPrompt(MessageVo messageVo, List<ChatHistoryVo> recentChatHistoryVos) {
        String sender = messageVo.getSender();
        String query = messageVo.getMsg();

        String systemText = PromptTemplate.SIMPLE_REPLY_SYSTEM_TEMPLATE.format(sender, query);
        SystemMessage systemMessage = new SystemMessage(systemText);

        String contextBlock = assembleChatHistorySection(recentChatHistoryVos);
        SystemMessage contextMessage = new SystemMessage(contextBlock);

        UserMessage userMessage = new UserMessage(sender + "가 요청한 메시지(질문): " + query + "\n");

        return new Prompt(systemMessage, contextMessage, userMessage);
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
