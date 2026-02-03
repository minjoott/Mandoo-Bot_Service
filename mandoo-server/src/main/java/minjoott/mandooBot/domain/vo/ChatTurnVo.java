package minjoott.mandooBot.domain.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class ChatTurnVo {

    @NonNull private final String dateTime;

    @NonNull private final String user;

    @NonNull private final String query;

    private final String reply;
}

