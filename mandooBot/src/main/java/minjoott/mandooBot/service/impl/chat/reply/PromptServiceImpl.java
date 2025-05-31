package minjoott.mandooBot.service.impl.chat.reply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.vo.ChatHistory;
import minjoott.mandooBot.domain.entity.Message;
import minjoott.mandooBot.service.chat.reply.PromptService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private static final String SYSTEM_TEMPLATE =
            "너는 '만두'라는 이름의 귀엽고 똑부러지는, 1인칭으로 답변해주는 AI 챗봇이야.\n" +
                    "이 채팅방에서 RAG로 조회한 과거 대화 내용들과 최근 채팅 이력들을 보내줄 테니까, 필요한 내용만 보강해서 %s의 메시지 \"%s\"에 필요한 답변만 깔끔하게 해줘.\n" +
                    "최대한 답변을 회피하지 말고, 주어진 정보들을 활용해서 최적의 답변을 해줘. 그리고 가끔 귀엽게 이모지나 이모티콘도 함께 사용해서 답변해줘.\n" +
                    "마크다운 문법을 절대 사용하지 않고, 순수 텍스트만으로, 읽기 편하게 문단을 꼭 나누어서! 필요한 답변만 꼭 간결하게 작성해!\n";

    /**
     * RAG 컨텍스트 + 최근 대화 이력을 조합해 최종 프롬프트를 생성
     *
     * @param message            사용자가 요청한 메시지 정보
     * @param ragContextMessages RAG 검색을 통해 조회한 과거 Message 리스트
     * @param recentChatHistory  최근 ChatHistory 리스트
     * @return 생성된 Prompt 객체
     */
    @Override
    public Prompt buildRagPrompt(Message message, List<Message> ragContextMessages, List<ChatHistory> recentChatHistory) {
        String room   = message.getRoom();
        String sender = message.getSender();
        String query  = message.getMsg();

        // 1) 시스템 메시지 생성: 봇 페르소나와 행동 지침 포함
        SystemMessage system = new SystemMessage(
                String.format(SYSTEM_TEMPLATE, sender, query)
        );

        // 2) 사용자 메시지 생성: 원본 요청 + 조합된 컨텍스트 문자열
        //    assembleFullContext() 내부에서 최근 대화 이력과 RAG 컨텍스트를 합침
        String contextBlock = assembleFullContext(room, ragContextMessages, recentChatHistory);

        UserMessage user = UserMessage.builder()
                .text(sender + "가 요청한 메시지: " + query + "\n")
                .text(contextBlock)
                .build();

        // 3) 완성된 Prompt 객체 반환
        return new Prompt(system, user);
    }

    /**
     * 최근 대화 이력 + RAG 컨텍스트 메시지를 조합해 하나의 문자열 블록으로 반환
     *
     * @param room                채팅방 식별자
     * @param ragContextMessages  RAG 검색 결과 메시지 리스트
     * @param recentChatHistory   최근 대화 이력 리스트
     * @return 텍스트 형태의 컨텍스트 블록
     */
    private String assembleFullContext(String room, List<Message> ragContextMessages, List<ChatHistory> recentChatHistory) {
        StringBuilder sb = new StringBuilder();

        // 1) 최근 대화 이력 섹션
        sb.append("----- 최근 채팅 이력 (오래된순) -----\n");
        for (ChatHistory h : recentChatHistory) {
            sb.append(h.getDateTime())
                    .append(" [").append(h.getUser()).append("] ")
                    .append(h.getQuery())
                    .append(" ⇒ 만두 답변: ")
                    .append(h.getReply())
                    .append("\n");
        }

        // 2) RAG 컨텍스트 섹션
        sb.append("\n----- RAG로 조회한 과거 대화 내용 (오래된순) -----\n");
        for (Message m : ragContextMessages) {
            sb.append(m.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append(" [").append(m.getSender()).append("] ")
                    .append(m.getMsg())
                    .append("\n");
        }

        return sb.toString();  // 완성된 문자열 블록 반환
    }
}
