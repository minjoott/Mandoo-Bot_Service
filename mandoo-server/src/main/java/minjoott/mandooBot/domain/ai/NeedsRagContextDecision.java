package minjoott.mandooBot.domain.ai;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NeedsRagContextDecision {
    private String needsRagContext;  // "O" | "X"

    public boolean isNeedsRagContext() {
        return "O".equalsIgnoreCase(needsRagContext);
    }
}
