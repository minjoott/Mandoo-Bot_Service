package minjoott.mandooBot.domain.dto;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class ChatResponse {
    private boolean hasReply;
    private String reply;   // 없으면 null
}
