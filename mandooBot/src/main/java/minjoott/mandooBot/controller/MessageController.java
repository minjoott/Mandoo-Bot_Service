package minjoott.mandooBot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.BotResponse;
import minjoott.mandooBot.domain.Message;
import org.springframework.web.bind.annotation.*;
import minjoott.mandooBot.service.MessageService;

@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public BotResponse message(@RequestBody Message message) {
        String msg = message.getMsg();

        log.info(message.toString());

        boolean needsReply = !message.isGroupChat() || msg.startsWith("만두");  // "만두"를 부른 경우 또는 만두와의 1:1 채팅방이면, true
        String reply = null;

        if (needsReply) {  // 메시지 응답 필수
            // 답변 생성
            double threshold = 0.2;
            reply = messageService.generateReply(message, threshold);
        }

        // 메시지는 항상 저장
        messageService.saveMessage(message);

        // 메시지봇r에 응답 보내기
        return new BotResponse(needsReply, reply);
    }
}
