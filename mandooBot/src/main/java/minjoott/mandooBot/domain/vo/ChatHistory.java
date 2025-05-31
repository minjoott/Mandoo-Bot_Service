package minjoott.mandooBot.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ChatHistory {

    private String dateTime;

    private String user;

    private String query;

    private String reply;
}
