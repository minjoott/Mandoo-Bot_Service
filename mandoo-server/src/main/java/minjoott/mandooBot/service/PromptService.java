package minjoott.mandooBot.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import minjoott.mandooBot.config.OpenAiOptions;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;

@Slf4j
@RequiredArgsConstructor
@Service
public class PromptService {

    public Prompt buildBufferCompleteDecisionPrompt(String mergedBufferText) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.BUFFER_COMPLETE_DECISION.format(mergedBufferText));
        UserMessage userMessage = new UserMessage("# 버퍼 통합본: " + mergedBufferText);

        return new Prompt(List.of(systemMessage, userMessage), OpenAiOptions.BUFFER_COMPLETE_DECISION);
    }

    public Prompt buildNeedsMandooDecisionPrompt(MessageVo mergedMessage, List<ChatTurnVo> recentChatTurns) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.NEEDS_MANDOO_DECISION.format(mergedMessage.getSender(), mergedMessage.getMsg()));
        SystemMessage recentContextMessage = new SystemMessage(assembleRecentChatTurns(recentChatTurns));  //빈게들어가는 경우(노이즈발생)을 위해 코드 수정 필요
        UserMessage userMessage = new UserMessage("# 사용자 메시지\n" + mergedMessage.getSender() + ": " + mergedMessage.getMsg());

        return new Prompt(List.of(systemMessage, recentContextMessage, userMessage), OpenAiOptions.NEEDS_MANDOO_DECISION);
    }

    public Prompt buildRagContextDecisionPrompt(String query) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.RAG_CONTEXT_DECISION.format(query));
        UserMessage userMessage = new UserMessage("# 사용자 메시지: " + query);

        return new Prompt(List.of(systemMessage, userMessage), OpenAiOptions.RAG_CONTEXT_DECISION);
    }

    public Prompt buildRagContextFilterPrompt(MessageVo message, List<RagContextMessageVo> ragContextMessages) {
        SystemMessage systemMessage = new SystemMessage(PromptTemplate.RAG_CONTEXT_FILTER.format(message.getMsg(), message.getMsg(), message.getMsg()));
        UserMessage userMessage = new UserMessage("# 사용자 메시지\n" + message.getSender() + ": " + message.getMsg());
        UserMessage ragContextMessage = new UserMessage(assembleRagContextMessages(ragContextMessages));  //빈게들어가는 경우(노이즈발생)을 위해 코드 수정 필요

        return new Prompt(List.of(systemMessage, userMessage, ragContextMessage), OpenAiOptions.RAG_CONTEXT_FILTER);
    }

    public Prompt buildReplyPrompt(MessageVo message, List<ChatTurnVo> recentChatTurns) {
        SystemMessage baseSystemMessage = new SystemMessage(PromptTemplate.REPLY_WITHOUT_RAG_CONTEXT.format(message.getSender(), message.getMsg()));
        SystemMessage nowSystemMessage = new SystemMessage("# 현재 시각: "+ message.getDateTime());
        SystemMessage recentContextMessage = new SystemMessage(assembleRecentChatTurns(recentChatTurns));
        UserMessage userMessage = new UserMessage("# 사용자 메시지\n" + message.getSender() + ": " + message.getMsg());

        return new Prompt(List.of(baseSystemMessage, nowSystemMessage, recentContextMessage, userMessage), OpenAiOptions.REPLY);
    }

    public Prompt buildReplyPrompt(MessageVo message, List<ChatTurnVo> recentChatTurns, List<RagContextMessageVo> ragContextMessages) {
        SystemMessage baseSystemMessage = new SystemMessage(PromptTemplate.REPLY_WITH_RAG_CONTEXT.format(message.getSender(), message.getMsg()));
        SystemMessage nowSystemMessage = new SystemMessage("# 현재 시각: "+ message.getDateTime());
        SystemMessage recentContextMessage = new SystemMessage(assembleRecentChatTurns(recentChatTurns));
        SystemMessage ragContextMessage = new SystemMessage(assembleRagContextMessages(ragContextMessages));
        UserMessage userMessage = new UserMessage("# 사용자 메시지\n" + message.getSender() + ": " + message.getMsg());

        return new Prompt(List.of(baseSystemMessage, nowSystemMessage, recentContextMessage, ragContextMessage, userMessage), OpenAiOptions.REPLY);
    }

    private String assembleRecentChatTurns(List<ChatTurnVo> recentChatTurns) {
        if (recentChatTurns.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n[최근 채팅 이력 | 과거순]\n");

        for (ChatTurnVo t : recentChatTurns) {
            String user = t.getUser();
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
        if (ragContextMessages.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n[참고: 관련 과거 메시지 (RAG 컨텍스트) | 과거순]\n");

        for (RagContextMessageVo m : ragContextMessages) {
            String dateTime = m.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String sender = m.getSender();
            String msg = m.getMsg().replaceAll("\t", " ").replaceAll("\\r?\\n", " ").trim();

            sb.append("[").append(dateTime).append("] ").append(sender).append(": ").append(msg).append("\n");
        }

        return sb.toString();
    }

}
