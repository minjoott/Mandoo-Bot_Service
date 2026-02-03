package minjoott.mandooBot.domain.ai;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BufferCompleteDecision {
    private String complete;   // "O" | "X"

    public boolean isComplete() {
        return "O".equalsIgnoreCase(complete);
    }
}
