package minjoott.mandooBot.aop;

import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.dto.ChatResponse;
import minjoott.mandooBot.domain.vo.MessageVo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class ChatTraceAspect {

    @Around("execution(* minjoott.mandooBot.service.ChatService.handleChat(..))")
    public Object traceHandleChat(ProceedingJoinPoint pjp) throws Throwable {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        MessageVo message = (MessageVo) pjp.getArgs()[0];

        log.info("🧩[trace={}] START handleChat | room={} | sender={} | group={} | msg=\"{}\"",
                traceId, message.getRoom(), message.getSender(), message.isGroupChat(), oneLine(message.getMsg()));

        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long took = System.currentTimeMillis() - start;

        log.info("✅[trace={}] END handleChat | took={}ms", traceId, took);

        MDC.remove("traceId");
        return result;
    }

    private String oneLine(String s) {
        if (s == null) return "null";
        return s.replaceAll("\\r?\\n", " ");
    }
}
