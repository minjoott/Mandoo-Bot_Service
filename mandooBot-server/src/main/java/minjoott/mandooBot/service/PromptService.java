package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptService {

    private static final String SYSTEM_TEMPLATE =
                    "# 정체성\n" +
                    "당신은 '만두'라는 이름의 AI 챗봇이며, 아래 지침을 반드시 준수하여 %s의 메시지 \"%s\"에만 집중하여 답변해야 합니다.\n\n" +
                    "# 지침\n" +
                    "- 아래 제공된 'RAG로 조회한 과거 대화 내용'에서 필요한 사실을 참고하여 답변하세요.\n" +
                    "- 아래 제공된 '만두와의 최근 채팅 이력'을 활용하여, 중복된 답변 없이 대화를 이어가주세요.\n" +
                    "- 불필요한 인사말(안녕하세요, 반갑습니다 등)은 생략하고, 곧바로 질문에 답하세요.\n" +
                    "- 질문에 답할 수 있는 정보가 충분하면 최대한 회피하지 말고 구체적으로 답변하세요.\n" +
                    "- 마크다운 문법을 전혀 사용하지 말고, 오직 순수 텍스트만 사용하세요.\n" +
                    "- 읽기 편하게 문단을 적절히 나누어서 답변해주세요.\n" +
                    "- 이모지를 최대 2개 사용해서 답변해주세요.\n" +
                    "- 답변은 최대 7문장 이내로 간결하게 작성해 주세요.\n";

    public Prompt buildRagPrompt(MessageVo messageVo, List<MessageVo> ragContextMessages, List<ChatHistoryVo> recentChatHistoryVo) {
        String sender = messageVo.getSender();
        String query = messageVo.getMsg();

        // 1) 시스템 메시지 생성: 봇 페르소나와 행동 지침 포함
        SystemMessage systemText = new SystemMessage(
                String.format(SYSTEM_TEMPLATE, sender, query)
        );

        // 2) 사용자 메시지 생성: 원본 요청 + 조합된 컨텍스트 문자열
        //    assembleFullContext() 내부에서 최근 대화 이력과 RAG 컨텍스트를 합침
        String contextBlock = assembleFullContext(ragContextMessages, recentChatHistoryVo);
        SystemMessage contextText = new SystemMessage(contextBlock);

        UserMessage userText = UserMessage.builder()
                .text(sender + "가 요청한 메시지(질문): " + query + "\n")
                .build();

        // 3) 완성된 Prompt 객체 반환
        return new Prompt(List.of(systemText, contextText, userText));
    }

    private String assembleFullContext(List<MessageVo> ragContextMessages, List<ChatHistoryVo> recentChatHistoryVo) {
        StringBuilder sb = new StringBuilder();

        // 1) 최근 대화 이력 섹션
        sb.append("----- 최근 채팅 이력 (오래된순) -----\n");
        for (ChatHistoryVo h : recentChatHistoryVo) {
            String cleanQuery = h.getQuery().replaceAll("\\r?\\n", " ");
            String cleanReply = h.getReply().replaceAll("\\r?\\n", " ");
            sb.append(h.getDateTime())
                    .append(" [").append(h.getUser()).append("] ")
                    .append(cleanQuery)
                    .append(" ⇒ 만두 답변: ")
                    .append(cleanReply)
                    .append("\n");
        }

        // 2) RAG 컨텍스트 섹션
        sb.append("\n----- RAG로 조회한 과거 대화 내용 (오래된순) -----\n");
        for (MessageVo m : ragContextMessages) {
            String cleanMsg = m.getMsg().replaceAll("\\r?\\n", " ");
            sb.append(m.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append(" [").append(m.getSender()).append("] ")
                    .append(cleanMsg)
                    .append("\n");
        }

        return sb.toString();  // 완성된 문자열 블록 반환
    }
}
