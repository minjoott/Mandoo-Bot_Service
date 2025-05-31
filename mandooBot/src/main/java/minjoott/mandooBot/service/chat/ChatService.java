package minjoott.mandooBot.service.chat;

import minjoott.mandooBot.domain.entity.Message;

public interface ChatService {

    /**
     * 메시지를 저장(persist)하기 전에 임베딩을 생성해서 함께 저장한다.
     *
     * @param message 저장할 Message 엔티티 (id, embedding은 null인 상태)
     * @return 저장된 Message (id, embedding 모두 세팅됨)
     */
    Message saveMessageWithEmbedding(Message message);

    /**
     * 저장된 메시지로부터 RAG 기반 답변을 생성한다.
     *
     * @param message 저장된 Message 엔티티
     * @return 생성된 답변 문자열
     */
    String createReply(Message message);
}