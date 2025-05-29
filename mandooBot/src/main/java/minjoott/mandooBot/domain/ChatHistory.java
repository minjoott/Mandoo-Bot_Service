package minjoott.mandooBot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ChatHistory {
    private String time;
    private String user;
    private String query;
    private String reply;
}
