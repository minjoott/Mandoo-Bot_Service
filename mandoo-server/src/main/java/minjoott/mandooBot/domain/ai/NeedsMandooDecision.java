package minjoott.mandooBot.domain.ai;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NeedsMandooDecision {
    private String needsMandoo;  // "O" | "X"

    public boolean isNeedsMandoo() {
        return "O".equalsIgnoreCase(needsMandoo);
    }
}
