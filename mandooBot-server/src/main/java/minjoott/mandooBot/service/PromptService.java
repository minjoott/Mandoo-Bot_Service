package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptService {

    private static final String FILTER_SYSTEM_TEMPLATE =
                    "# 정체성\n" +
                    "당신은 RAG 기반 AI 챗봇을 구현하는 데 활용할 어시스턴스입니다.\n" +
                    "# 지침\n" +
                    "- 아래 주어진 '임베딩으로 조회한 과거 대화 목록' 중, 사용자 질의 \"%s\"에 답변하는 데 \n" +
                    "  반드시 필요한 메시지만 골라서, **유효한 JSON 배열** 형태로만 응답하세요.\n" +
                    "- 반드시 유효한 JSON 배열 형태로만 응답하세요.\n" +
                    "반드시 JSON 내부의 모든 문자열 값은 JSON 규격대로 이스케이프된 상태여야 합니다.\n" +
                    "  예를 들어, 메시지 안에 쌍따옴표(\")가 들어갈 경우 반드시 `\\\"` 로 바꿔주세요\n" +
                    "- 절대로 그 외의 한국어 설명, 주석, 코드 블록, 마크다운 등을 섞지 마십시오.\n" +
                    "반환할 JSON 배열의 요소(객체)에는 세 필드만 포함해야 합니다:\n" +
                    "  1. \"sender\": String\n" +
                    "  2. \"msg\": String (안의 쌍따옴표, 줄바꿈 등을 모두 \\\" 또는 \\n 같은 이스케이프 형태로 처리)\n" +
                    "  3. \"dateTime\": String (ISO_LOCAL_DATE_TIME 포맷)\n" +
                    "\n" +
                    "### 예시 응답 (유효한 JSON 배열, 반드시 이 형식을 따르세요):\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"sender\": \"userA\",\n" +
                    "    \"msg\": \"이 메시지 안의 \\\"쌍따옴표\\\"는 모두 이스케이프되었습니다.\",\n" +
                    "    \"dateTime\": \"2025-06-05T16:00:00\"\n" +
                    "  },\n" +
                    "]\n";

    private static final String RAG_SYSTEM_TEMPLATE =
                    "# 정체성\n" +
                    "당신은 '만두'라는 이름의 AI 챗봇이며, 아래 지침을 반드시 준수하여 %s의 메시지 \"%s\"에만 집중하여 답변해야 합니다.\n\n" +
                    "# 지침\n" +
                    "- 아래 제공된 '임베딩으로 조회한 과거 대화 내용'에서 필요한 사실을 참고하여 답변하세요.\n" +
                    "- 아래 제공된 '만두와의 최근 채팅 이력'을 활용하여, 중복된 답변 없이 대화를 이어가주세요.\n" +
                    "- 불필요한 인사말(안녕하세요, 반갑습니다 등)은 생략하고, 곧바로 질문에 답하세요.\n" +
                    "- 질문에 답할 수 있는 정보가 충분하면 최대한 회피하지 말고 구체적으로 답변하세요.\n" +
                    "- 마크다운 문법을 전혀 사용하지 말고, 오직 순수 텍스트만 사용하세요.\n" +
                    "- 읽기 편하게 문단을 적절히 나누어서 답변해주세요.\n" +
                    "- 이모지를 최대 2개 사용해서 답변해주세요.\n" +
                    "- 답변은 최대 7문장 이내로 간결하게 작성해 주세요.\n";

    public Prompt buildRagContextFilterPrompt(String query, List<RagContextMessageVo> ragContextMessageVos) {
        // 1) 시스템 템플릿에 유저 질의를 삽입하여 기본 지침 구성
        SystemMessage systemMessage = new SystemMessage(String.format(FILTER_SYSTEM_TEMPLATE, query));

        // 2) 실제 프롬프트에 “임베딩으로 조회한 과거 대화” 리스트를 추가
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("\n----- 임베딩으로 조회한 과거 대화 내용 (오래된순) -----\n");
        for (RagContextMessageVo rctx : ragContextMessageVos) {
            String cleanMsg = rctx.getMsg().replaceAll("\\r?\\n", " ");
            contextBuilder
                    .append(rctx.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append(" [").append(rctx.getSender()).append("] ")
                    .append(cleanMsg)
                    .append("\n");
        }
        contextBuilder.append("\n");

        // 3) Prompt 객체 생성 (Prompt 빌더/생성자 형태에 맞춰 수정 가능)
        UserMessage userMessage = new UserMessage(contextBuilder.toString());

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return prompt;
    }

    public Prompt buildRagPrompt(MessageVo messageVo, List<RagContextMessageVo> ragContextMessages, List<ChatHistoryVo> recentChatHistoryVo) {
        String sender = messageVo.getSender();
        String query = messageVo.getMsg();

        // 1) 시스템 메시지 생성: 봇 페르소나와 행동 지침 포함
        SystemMessage systemMessage = new SystemMessage(
                String.format(RAG_SYSTEM_TEMPLATE, sender, query)
        );

        // 2) 사용자 메시지 생성: 원본 요청 + 조합된 컨텍스트 문자열
        //    assembleFullContext() 내부에서 최근 대화 이력과 RAG 컨텍스트를 합침
        String contextBlock = assembleFullContext(ragContextMessages, recentChatHistoryVo);
        SystemMessage contextMessage = new SystemMessage(contextBlock);

        UserMessage userMessage = UserMessage.builder()
                .text(sender + "가 요청한 메시지(질문): " + query + "\n")
                .build();

        // 3) 완성된 Prompt 객체 반환
        return new Prompt(List.of(systemMessage, contextMessage, userMessage));
    }

    private String assembleFullContext(List<RagContextMessageVo> filteredRagContextMessageVos, List<ChatHistoryVo> recentChatHistoryVo) {
        StringBuilder contextBuilder = new StringBuilder();

        // 1) 최근 대화 이력 섹션
        contextBuilder.append("\n----- 최근 채팅 이력 (오래된순) -----\n");
        for (ChatHistoryVo h : recentChatHistoryVo) {
            String cleanQuery = h.getQuery().replaceAll("\\r?\\n", " ");
            String cleanReply = h.getReply().replaceAll("\\r?\\n", " ");
            contextBuilder.append(h.getDateTime())
                    .append(" [").append(h.getUser()).append("] ")
                    .append(cleanQuery)
                    .append(" ⇒ 만두 답변: ")
                    .append(cleanReply)
                    .append("\n");
        }

        // 2) RAG 컨텍스트 섹션
        contextBuilder.append("\n----- 임베딩으로 조회한 과거 대화 내용 (오래된순) -----\n");
        for (RagContextMessageVo m : filteredRagContextMessageVos) {
            String cleanMsg = m.getMsg().replaceAll("\\r?\\n", " ");
            contextBuilder.append(m.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append(" [").append(m.getSender()).append("] ")
                    .append(cleanMsg)
                    .append("\n");
        }

        return contextBuilder.toString();  // 완성된 문자열 블록 반환
    }
}
