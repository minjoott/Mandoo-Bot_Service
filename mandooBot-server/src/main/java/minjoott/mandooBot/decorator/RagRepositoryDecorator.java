package minjoott.mandooBot.decorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.entity.Message;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagRepositoryDecorator {

    private final MessageRepository messageRepository;
    private final ExternalAiClientDecorator externalAiClientDecorator;

    @Value("${chat.rag.distance.threshold}")
    private double distanceThreshold;

    @Transactional
    public MessageVo saveMessageWithEmbedding(MessageVo messageVo) {
        float[] embedding = externalAiClientDecorator.createEmbedding(messageVo.getMsg());

        Message message = messageVo.toEntity(embedding);

        Message savedMessage = messageRepository.save(message);
        log.info("Saved message id={} with embedding[0]={}", savedMessage.getMsg(), savedMessage.getEmbedding()[0]);

        return savedMessage.toVo();
    }

    public List<MessageVo> findMessagesWithEmbedding(MessageVo messageVo) {
        float[] embedding = externalAiClientDecorator.createEmbedding(messageVo.getMsg());

        List<Message> messages = messageVo.isGroupChat()
                ? messageRepository.getMessagesWithEmbeddingByRoom(messageVo.getRoom(), embedding, distanceThreshold)
                : messageRepository.getMessagesWithEmbeddingBySender(messageVo.getSender(), embedding, distanceThreshold);

        return messages.stream()
                .map(Message::toVo)
                .collect(Collectors.toList());
    }
}