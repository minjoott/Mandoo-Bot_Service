package minjoott.mandooBot.domain.dto;

import lombok.*;

@Getter
@AllArgsConstructor
public class ChatResponse {

    private boolean hasReply;

    private String reply;

}
