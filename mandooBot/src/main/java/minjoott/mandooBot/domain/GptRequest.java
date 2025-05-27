package minjoott.mandooBot.domain;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class GptRequest {
    private String sender;
    private String query;
    private List<Message> context;
}
