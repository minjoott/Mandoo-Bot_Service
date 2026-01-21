package minjoott.mandooBot.decorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.entity.Message;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import minjoott.mandooBot.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class RagRepositoryDecorator {

    private final MessageRepository messageRepository;
    private final ExternalAiClientDecorator externalAiClientDecorator;

    @Value("${chat.rag.distance.max}")
    private double maxDistance;
    @Value("${chat.rag.distance.limit}")
    private int distanceLimit;

    public float[] generateEmbedding(String text) {
        return externalAiClientDecorator.getEmbedding(text);
    }

    public List<RagContextMessageVo> findMessagesWithEmbedding(MessageVo message, float[] embedding) {
        List<Message> ragContextMessages = message.isGroupChat()
                ? messageRepository.findMessagesWithEmbeddingByRoom(message.getRoom(), embedding, maxDistance, distanceLimit)
                : messageRepository.findMessagesWithEmbeddingBySender(message.getSender(), embedding, maxDistance, distanceLimit);
        return Message.toVoList(ragContextMessages);
    }

    public MessageVo saveMessageWithEmbedding(MessageVo message, float[] embedding) {
        Message savedMessage = messageRepository.save(message.toEntity(embedding));
        return savedMessage.toMessageVo();
    }

}