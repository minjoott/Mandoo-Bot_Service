package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.ChatHistory;
import minjoott.mandooBot.domain.Message;
import minjoott.mandooBot.repository.MessageRepository;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final EmbeddingService embeddingService;
    private final OpenAiChatModel openAiChatModel;
    private final ChatHistoryService chatHistoryService;

    private static final int MAX_HISTORY_TURNS = 10;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 1) 메시지를 받아 임베딩 생성
     * 2) Message 엔티티에 담아 DB에 저장
     */
    @Transactional
    public Message saveMessage(Message message) {
        float[] vector = embeddingService.embed(message.getMsg());
        message.setEmbedding(vector);
        return messageRepository.save(message);
    }

    /**
     *
     */
    @Transactional(readOnly = true)
    public String generateReply(Message message,
                                double distanceThreshold) {
        String time = message.getTime().format(TIMESTAMP_FORMAT);
        String room = message.getRoom();
        String sender = message.getSender();
        String msg = message.getMsg();
        float[] vector = embeddingService.embed(msg);

        /**
         * RAG 패턴으로 답변 생성
         */
        List<Message> ragContextMessages;
        if (!message.isGroupChat()) {  // 만두와의 1:1 채팅방이면,
            ragContextMessages = messageRepository.findContextMessagesBySender(message.getSender(), vector, distanceThreshold);
        } else {
            ragContextMessages = messageRepository.findContextMessagesByRoom(message.getRoom(), vector, distanceThreshold);
        }

        return generateRagReply(room, sender, msg, time, ragContextMessages);
    }

    /**
     * 대화 기억해서 최종 답변 생성
     */
    public String generateRagReply(String room, String sender, String query, String time, List<Message> ragContextMessages) {

        // 1) 프롬프트 텍스트 조립
        SystemMessage systemMessage = new SystemMessage(
                "너는 '만두'라는 이름의 귀엽고 똑부러지는, 1인칭으로 답변해주는 AI 챗봇이야.\n" +
                "이 채팅방에서 RAG로 조회한 과거 대화 내용들과 최근 채팅 이력들을 보내줄 테니까, 필요한 내용만 보강해서" + sender + "의 메시지 " + query +"에 필요한 답변만 깔끔하게 해줘.\n" +
                        "최대한 답변을 회피하지 말고, 주어진 정보들을 활용해서 최적의 답변을 해줘. 그리고 가끔 귀엽게 이모지나 이모티콘도 함께 사용해서 답변해줘.\n" +
                        "마크다운 문법을 절대 사용하지 않고, 순수 텍스트만으로, 읽기 편하게 문단을 꼭 나누어서! 필요한 답변만 꼭 간결하게 작성해!" +
                        "너가 내리는 답변에 대한 근거를 먼저 물어보지 않는 이상, 서술하지 마.");

        StringBuilder contextBuilder = new StringBuilder();

        // – 만두와의 최근 채팅 이력
        contextBuilder.append("----- 최근 채팅 이력 (오래된순) -----\n");
        List<ChatHistory> recentChaiHistory = chatHistoryService.getRecentHistory(room, MAX_HISTORY_TURNS);
        for (ChatHistory h : recentChaiHistory) {
            contextBuilder
                    .append(h.getTime()).append(" [").append(h.getUser()).append("] ")
                    .append(h.getQuery()).append(" ⇒ 만두 답변: ").append(h.getReply())
                    .append("\n");
        }

        // – RAG 컨텍스트
        contextBuilder.append("\n----- RAG로 조회한 과거 대화 내용 (오래된순) -----\n");
        for (Message m : ragContextMessages) {
            contextBuilder
                    .append(m.getTime().format(TIMESTAMP_FORMAT)).append(" [")
                    .append(m.getSender()).append("] ")
                    .append(m.getMsg()).append("\n");
        }

        UserMessage userMessage = UserMessage.builder()
                .text(sender + "가 요청한 메시지: " + query + "\n")
                .text(contextBuilder.toString())
                .build();

        // 3) OpenAI ChatCompletion 호출 및 반환
        log.info(contextBuilder.toString());

        Prompt prompt = new Prompt(systemMessage, userMessage);

        ChatResponse chatResponse = openAiChatModel.call(prompt);
        System.out.println("systemMessage: " + prompt.getSystemMessage());
//        System.out.println(openAiChatModel.getDefaultOptions().toString());

        String reply = chatResponse.getResult().getOutput().getText();

        // 최근 대화 이력 저장
        chatHistoryService.saveChatHistory(room, new ChatHistory(time, sender, query, reply));

        return reply;
    }
}
