package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.domain.GptRequest;
import minjoott.mandooBot.domain.Message;
import minjoott.mandooBot.repository.MessageRepository;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final EmbeddingService embeddingService;
    private final OpenAiChatModel openAiChatModel;

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
        String sender = message.getSender();
        String msg = extractMsg(message.getMsg());
        float[] vector = embeddingService.embed(msg);

        /**
         * RAG 패턴으로 최종 답변 생성
         */
        List<Message> contextMessages;
        if (!message.isGroupChat()) {  // 만두와의 1:1 채팅방이면,
            contextMessages = messageRepository.findContextMessagesBySender(message.getSender(), vector, distanceThreshold);
        }
        else {
            contextMessages = messageRepository.findContextMessagesByRoom(message.getRoom(), vector, distanceThreshold);
        }

        return generateRagReply(new GptRequest(sender, msg, contextMessages));
    }

    /**
     * RAG 답변 생성
     */
    public String generateRagReply(GptRequest gptRequest) {
        String sender = gptRequest.getSender();
        String query = gptRequest.getQuery();
        List<Message> context = gptRequest.getContext();

        // 1) 프롬프트 텍스트 조립
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 친절한 AI 도우미입니다.\n")
                .append("아래 문맥(Context)를 참고하여 " + sender + "의 질문에 답변해주세요.\n\n")
                .append("문맥:\n");
        for (Message msg : context) {
            sb.append(String.format("- [%s] %s, %s\n", msg.getSender(), msg.getMsg(), msg.getTime().toString()));
        }
        sb.append("\n질문: ").append(query);

        String promptText = sb.toString();

        // 2) Prompt 객체 생성 (옵션은 application.yml 에서 제어)
        Prompt prompt = new Prompt(promptText);

        // 3) OpenAI ChatCompletion 호출
        ChatResponse response = openAiChatModel.call(prompt);

        // 4) 답변 반환
        return response.getResults().get(0).getOutput().getText();
    }

    /**
     * "만두야" 접두사를 제거한 msg 추출
     */
    private String extractMsg(String msg) {
        final String PREFIX = "만두야,";
        if (msg.startsWith(PREFIX)) {
            String trimmed = msg.substring(PREFIX.length());
            return trimmed;
        }
        return msg;
    }
}
