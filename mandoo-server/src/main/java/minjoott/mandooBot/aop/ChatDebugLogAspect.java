package minjoott.mandooBot.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.dto.RedisBufferDecision;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ChatDebugLogAspect {

    private final ObjectMapper objectMapper;

    // ✅ (0) 버퍼 통합본 조회 로그
    @AfterReturning(
            pointcut = "execution(* minjoott.mandooBot.decorator.BufferRedisDecorator.readMergedBuffer(..)) && args(roomId, sender)",
            returning = "merged"
    )
    public void logReadMergedBuffer(String roomId, String sender, String merged) {
        int len = (merged == null) ? 0 : merged.length();
        int parts = (merged == null || merged.isBlank()) ? 0 : merged.split("\\s+", -1).length;

        log.info("🧩[trace={}] bufferMerged room={} sender={} parts~={} len={} preview=\"{}\"",
                trace(), roomId, sender, parts, len, oneLine(merged).replaceAll("\\r?\\n", " ")
        );
    }

    // ✅ (0) 버퍼 통합본 확정 여부 및 만두 답변 필요 여부 확인 로그
    @AfterReturning(
            pointcut = "execution(minjoott.mandooBot.domain.dto.RedisBufferDecision minjoott.mandooBot.decorator.ExternalAiClientDecorator.getBufferDecision(..))",
            returning = "decision"
    )
    public void logBufferDecision(RedisBufferDecision decision) {
        log.info("🧠[trace={}] bufferDecision complete={} needsMandoo={}",
                trace(), decision.getComplete(), decision.getNeedsMandoo());
    }

    // ✅ (1) RAG 필요 여부 결정 결과
    @AfterReturning(
            pointcut = "execution(boolean minjoott.mandooBot.decorator.ExternalAiClientDecorator.getRagContextDecision(..))",
            returning = "decision"
    )
    public void logRagDecision(boolean decision) {
        log.info("🧠[trace={}] ragNeeded = {}", trace(), decision);
    }

    // ✅ (2) 최근 대화 이력 "전체" 출력
    @AfterReturning(
            pointcut = "execution(java.util.List minjoott.mandooBot.decorator.RecentChatRedisDecorator.loadRecentChats(..))",
            returning = "turns"
    )
    public void logRecentChats(List<ChatTurnVo> turns) {
        if (turns == null || turns.isEmpty()) {
            log.info("📜[trace={}] recentChatTurns | size=0", trace());
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n📜[trace=").append(trace()).append(" | recentChatTurns size=").append(turns.size());

        for (int i = 0; i < turns.size(); i++) {
            ChatTurnVo t = turns.get(i);
            String reply = (t.getReply() == null) ? "" : t.getReply().replaceAll("\\r?\\n", " ");
            sb.append("\n  ├─ [").append(i).append("] ")
                    .append(t.getUser()).append(": \"").append(t.getQuery().replaceAll("\\r?\\n", " ")).append("\"")
                    .append(" | 만두: \"").append(reply).append("\"");
        }

        log.info(sb.toString());
    }

    // ✅ (3) RAG 후보(벡터검색 결과)
    @AfterReturning(
            pointcut = "execution(java.util.List minjoott.mandooBot.decorator.RagRepositoryDecorator.findMessagesWithEmbedding(..))",
            returning = "candidates"
    )
    public void logRagCandidates(List<RagContextMessageVo> candidates) {
        int count = (candidates == null) ? 0 : candidates.size();
        log.info("🗂️[trace={}] ragCandidates count = {}", trace(), count);
    }

    // ✅ (4) 필터링된 RAG 컨텍스트 "전체" 출력
    @AfterReturning(
            pointcut = "execution(java.util.List minjoott.mandooBot.decorator.ExternalAiClientDecorator.getFilteredRagContext(..))",
            returning = "filtered"
    )
    public void logFilteredRagContext(List<RagContextMessageVo> filtered) {

        if (filtered == null || filtered.isEmpty()) {
            log.info("🔎[trace={}] filteredRagContext size=0", trace());
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n🔎[trace=").append(trace())
                .append("] filteredRagContext size=").append(filtered.size());

        for (int i = 0; i < filtered.size(); i++) {
            RagContextMessageVo c = filtered.get(i);
            sb.append("\n  ├─ [").append(i).append("] ")
                    .append(c.getDateTime()).append(" ")
                    .append(c.getSender()).append(": \"")
                    .append(c.getMsg().replaceAll("\\r?\\n", " ")).append("\"");
        }

        log.info(sb.toString());
    }

    // ✅ (5) 최종 답변(LLM 응답) 내용
    @AfterReturning(
            pointcut = "execution(String minjoott.mandooBot.decorator.ExternalAiClientDecorator.getReply(..)) && args(prompt)",
            returning = "reply"
    )
    public void logFinalReply(Prompt prompt, String reply) {
        log.info("🤖[trace={}] finalReply = \"{}\"", trace(), oneLine(reply));
    }

    private String trace() {
        String t = MDC.get("traceId");
        return (t == null) ? "-" : t;
    }

    private String oneLine(String s) {
        if (s == null) return "null";
        return s.replaceAll("\\r?\\n", " ");
    }
}
