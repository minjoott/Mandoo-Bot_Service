package minjoott.mandooBot.service.impl.chat.reply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.vo.ChatHistory;
import minjoott.mandooBot.domain.entity.Message;
import minjoott.mandooBot.repository.MessageRepository;
import minjoott.mandooBot.service.chat.reply.ChatHistoryService;
import minjoott.mandooBot.service.chat.reply.PromptService;
import minjoott.mandooBot.service.chat.reply.RagReplyService;
import minjoott.mandooBot.service.embedding.EmbeddingService;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.openai.OpenAiChatModel;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagReplyServiceImpl implements RagReplyService {

    private final EmbeddingService embeddingService;
    private final OpenAiChatModel openAiChatModel;
    private final MessageRepository messageRepository;
    private final ChatHistoryService chatHistoryService;
    private final PromptService promptService;

    @Value("${chat.rag.distance.threshold}")
    private double distanceThreshold;

    /**
     * RAG 패턴에 따라 답변 생성
     * 1) 메시지 정보 파싱
     * 2) 임베딩 생성 및 유사도 기반 과거 메시지 조회
     * 3) 최근 대화 이력 조회
     * 4) 프롬프트 조립 및 OpenAI 호출
     * 5) 챗 히스토리 저장 후 응답 반환
     *
     * @param message 답변 대상을 담은 엔티티
     * @return 생성된 챗봇 답변 텍스트
     */
    @Override
    @Transactional(readOnly = true)
    public String createRagReply(Message message) {
        String time   = message.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String sender = message.getSender();
        String room   = message.getRoom();
        String query  = message.getMsg();

        // 1) 임베딩 생성 + RAG 컨텍스트 조회 (private 메서드 활용)
        List<Message> ragContextMessages = retrieveRagContext(message);

        // 2) 최근 대화 이력 조회
        List<ChatHistory> recentChatHistory = chatHistoryService.findRecentChatHistory(room);

        // 3) Prompt 생성
        Prompt prompt = promptService.buildRagPrompt(
                message, ragContextMessages, recentChatHistory);

        // 4) OpenAI 호출 및 답변 텍스트 추출
        ChatResponse response = openAiChatModel.call(prompt);
        String reply = response.getResult().getOutput().getText();
        log.info("Created reply: {}", reply);

        // 5) 요청된 메시지지와 생성된 답변을 최근 대화 이력에 저장
        chatHistoryService.saveChatHistory(room, new ChatHistory(time, sender, query, reply));

        return reply;
    }

    /**
     * 메시지 내용으로부터 임베딩 벡터를 생성하고,
     * 그룹 채팅 여부에 따라 적절한 DB 조회 메서드를 호출하여
     * 과거 메시지 리스트를 반환합니다.
     *
     * @param message 조회 대상 메시지 엔티티
     * @return 과거 메시지 리스트
     */
    private List<Message> retrieveRagContext(Message message) {
        // 사용자의 메시지를 벡터로 변환
        float[] embedding = embeddingService.createEmbedding(message.getMsg());

        // 그룹 채팅이면 방 단위로, 1:1 채팅이면 발신자 단위로 조회
        if (message.isGroupChat()) {
            return messageRepository.findContextMessagesByRoom(
                    message.getRoom(), embedding, distanceThreshold);
        } else {
            return messageRepository.findContextMessagesBySender(
                    message.getSender(), embedding, distanceThreshold);
        }
    }
}
