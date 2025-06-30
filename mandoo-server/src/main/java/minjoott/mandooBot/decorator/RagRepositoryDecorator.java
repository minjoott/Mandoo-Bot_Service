package minjoott.mandooBot.decorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.entity.Message;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import minjoott.mandooBot.domain.vo.RequestMessageVo;
import minjoott.mandooBot.domain.vo.SavedMessageVo;
import minjoott.mandooBot.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional
    public SavedMessageVo saveMessageWithEmbedding(RequestMessageVo messageVo) {
        float[] embedding = externalAiClientDecorator.getEmbedding(messageVo.getMsg());
        Message savedMessage = messageRepository.save(messageVo.toEntity(embedding));
        log.info("\n💾 벡터 DB에 메시지 저장 성공 ⮕ id = {} | msg = \"{}\" | embedding[0] = {}", savedMessage.getId(), savedMessage.getMsg(), savedMessage.getEmbedding()[0]);
        return savedMessage.toMessageVo();
    }

    public List<RagContextMessageVo> findMessagesWithEmbedding(SavedMessageVo messageVo) {
        float[] embedding = externalAiClientDecorator.getEmbedding(messageVo.getMsg());

        List<Message> ragContextMessages = messageVo.isGroupChat()
                ? messageRepository.findMessagesWithEmbeddingByRoom(messageVo.getRoom(), embedding, maxDistance, distanceLimit)
                : messageRepository.findMessagesWithEmbeddingBySender(messageVo.getSender(), embedding, maxDistance, distanceLimit);
        log.info("\n🔍 RAG 컨텍스트 조회 ⮕ count = {} | query = \"{}\"", ragContextMessages.size(), messageVo.getMsg());

        return ragContextMessages.stream()
                .map(Message::toRagContextMessageVo)
                .collect(Collectors.toList());
    }
}