package minjoott.mandooBot.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class RagContextMessageVo {
    private String sender;
    private String msg;
    private LocalDateTime dateTime;
}
