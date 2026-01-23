package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.config.OpenAiOptions;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PromptService {

    public Prompt buildBufferDecisionPrompt(String mergedBufferText) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.MESSAGE_BUFFER_DECISION.format(mergedBufferText));
        UserMessage userMessage = new UserMessage("버퍼 통합본: " + mergedBufferText);
        return new Prompt(List.of(systemMessage, userMessage), OpenAiOptions.MESSAGE_BUFFER_DECISION);
    }

    public Prompt buildRagContextDecisionPrompt(String query) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.RAG_CONTEXT_DECISION.format(query));
        UserMessage userMessage = new UserMessage("사용자 메시지: " + query);
        return new Prompt(List.of(systemMessage, userMessage), OpenAiOptions.RAG_CONTEXT_DECISION);
    }

    public Prompt buildRagContextFilterPrompt(String sender, String query, List<RagContextMessageVo> ragContextMessages) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.RAG_CONTEXT_FILTER.format(query, query, query));
        String ragContextSection = assembleRagContextMessages(ragContextMessages);
        UserMessage userMessage = new UserMessage("\n" + sender + ": " + query + "\n" + ragContextSection);
        return new Prompt(List.of(systemMessage, userMessage), OpenAiOptions.RAG_CONTEXT_FILTER);
    }

    public Prompt buildReplyPrompt(MessageVo message, List<ChatTurnVo> recentChatTurns) {
        SystemMessage baseSystemMessage = new SystemMessage(PromptTemplate.REPLY_WITHOUT_RAG_CONTEXT.format(message.getSender(), message.getMsg()));
        SystemMessage nowSystemMessage = new SystemMessage("#현재 시각: "+ message.getDateTime());
        SystemMessage recentContextMessage = toRecentContextMessage(recentChatTurns);
        UserMessage userQueryMessage = new UserMessage(message.getSender() + ": " + message.getMsg() + "\n");
        return new Prompt(List.of(baseSystemMessage, nowSystemMessage, recentContextMessage, userQueryMessage), OpenAiOptions.REPLY);
    }

    public Prompt buildReplyPrompt(MessageVo message, List<ChatTurnVo> recentChatTurns, List<RagContextMessageVo> ragContextMessages) {
        SystemMessage baseSystemMessage = new SystemMessage(PromptTemplate.REPLY_WITH_RAG_CONTEXT.format(message.getSender(), message.getMsg()));
        SystemMessage nowSystemMessage = new SystemMessage("#현재 시각: "+ message.getDateTime());
        SystemMessage recentContextMessage = toRecentContextMessage(recentChatTurns);
        SystemMessage ragContextMessage = toSimilarContextMessage(ragContextMessages);
        UserMessage userQueryMessage = new UserMessage(message.getSender() + ": " + message.getMsg() + "\n");
        return new Prompt(List.of(baseSystemMessage, nowSystemMessage, recentContextMessage, ragContextMessage, userQueryMessage), OpenAiOptions.REPLY);
    }

    private SystemMessage toRecentContextMessage(List<ChatTurnVo> recentChatTurns) {
        String recentChatTurnSection = assembleRecentChatTurns(recentChatTurns);
        return new SystemMessage(recentChatTurnSection);
    }

    private SystemMessage toSimilarContextMessage(List<RagContextMessageVo> ragContextMessages) {
        String ragContextMessageSection = assembleRagContextMessages(ragContextMessages);
        return new SystemMessage(ragContextMessageSection);
    }

    private String assembleRecentChatTurns(List<ChatTurnVo> recentChatTurns) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[최근 채팅 이력 | 오래된순]\n");

        for (ChatTurnVo t : recentChatTurns) {
            String user = t.getUser().replaceAll("\\r?\\n", " ").trim();
            String query = t.getQuery().replaceAll("\t", " ").replaceAll("\\r?\\n", " ").trim();

            sb.append(user).append(": ").append(query);
            String reply = t.getReply();
            if (reply != null) {
                sb.append(" | 만두: ").append(reply.replaceAll("\\r?\\n", " ").trim());
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String assembleRagContextMessages(List<RagContextMessageVo> ragContextMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[참고: 관련 과거 메시지 (RAG 컨텍스트) | 오래된순]\n");

        for (RagContextMessageVo m : ragContextMessages) {
            String sender = m.getSender().replaceAll("\\r?\\n", " ").trim();
            String msg = m.getMsg().replaceAll("\t", " ").replaceAll("\\r?\\n", " ").trim();
            String dateTime = m.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            sb.append(sender).append(" (").append(dateTime).append("): ").append(msg).append("\n");
        }

        return sb.toString();
    }

}
