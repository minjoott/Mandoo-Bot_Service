package minjoott.mandooBot.service.impl.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.entity.Message;
import minjoott.mandooBot.repository.MessageRepository;
import minjoott.mandooBot.service.chat.ChatService;
import minjoott.mandooBot.service.embedding.EmbeddingService;
import minjoott.mandooBot.service.chat.reply.RagReplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final EmbeddingService embeddingService;
    private final RagReplyService ragReplyService;

    @Override
    @Transactional
    public Message saveMessageWithEmbedding(Message message) {
        // 1) 메시지 내용으로부터 임베딩 벡터 생성
        float[] embedding = embeddingService.createEmbedding(message.getMsg());

        // 2) 생성된 임베딩을 Message 엔티티에 설정
        message.setEmbedding(embedding);

        // 3) 임베딩이 포함된 메시지를 DB에 저장
        Message savedMessage = messageRepository.save(message);
        log.info("Saved message id={} with embedding[0]={}", savedMessage.getId(),
                savedMessage.getEmbedding()[0]);

        // 4) 저장된 메시지 반환
        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public String createReply(Message message) {
        return ragReplyService.createRagReply(message);
    }
}