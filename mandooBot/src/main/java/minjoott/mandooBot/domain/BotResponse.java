package minjoott.mandooBot.domain;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class BotResponse {
    private boolean hasReply;
    private String reply;   // 없으면 null
}
