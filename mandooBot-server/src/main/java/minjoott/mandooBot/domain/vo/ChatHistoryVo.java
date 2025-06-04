package minjoott.mandooBot.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ChatHistoryVo {

    private String dateTime;
    private String user;
    private String query;
    private String reply;
}
