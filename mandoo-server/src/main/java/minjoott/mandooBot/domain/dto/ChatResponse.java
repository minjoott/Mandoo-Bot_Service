package minjoott.mandooBot.domain.dto;

import lombok.*;

@AllArgsConstructor
@Getter
public class ChatResponse {

    private boolean hasReply;

    private String reply;

}
