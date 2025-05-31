package minjoott.mandooBot.service.chat.reply;

import minjoott.mandooBot.domain.entity.Message;

public interface RagReplyService {

    /**
     * 프롬프트 생성부터 OpenAI 호출, 대화 이력 저장까지 포함한 최종 답변 생성
     *
     * @param message               답변 할 메시지
     * @return 챗봇이 생성한 답변 텍스트
     */
    String createRagReply(Message message);
}
