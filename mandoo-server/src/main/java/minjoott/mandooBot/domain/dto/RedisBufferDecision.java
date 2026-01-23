package minjoott.mandooBot.domain.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisBufferDecision {
    private String complete;     // "O" | "X"
    private String needsMandoo;  // "O" | "X" | "N"

    public boolean isComplete() {
        return "O".equalsIgnoreCase(complete);
    }
    public boolean isNeedsMandoo() {
        return "O".equalsIgnoreCase(needsMandoo);
    }
}
