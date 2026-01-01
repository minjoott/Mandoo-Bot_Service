package minjoott.mandooBot.domain.dto;

import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatResponse {
    private boolean hasReply;
    private String reply;   //답변을 전송하지 않는 경우(hasReply가 false인 경우) null

    public static ChatResponse noReply() {
        return new ChatResponse(false, null);
    }

    public static ChatResponse withReply(String reply) {
        return new ChatResponse(true, reply);
    }
}
