package minjoott.mandooBot.service.chat.reply;

import minjoott.mandooBot.domain.vo.ChatHistory;
import minjoott.mandooBot.domain.entity.Message;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

public interface PromptService {

    /**
     * RAG 컨텍스트와 최근 대화 이력을 모두 포함한 프롬프트를 생성
     *
     * @param message            응답을 요청한 메시지
     * @param ragContextMessages RAG 문맥 메시지 목록
     * @param recentHistory      최근 대화 이력 목록
     * @return OpenAI 호출용 Prompt 객체
     */
    Prompt buildRagPrompt(Message message,
            List<Message> ragContextMessages,
            List<ChatHistory> recentHistory
    );
}
