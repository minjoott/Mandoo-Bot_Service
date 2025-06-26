package minjoott.mandooBot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.dto.ChatResponse;
import minjoott.mandooBot.domain.dto.ChatRequest;
import minjoott.mandooBot.service.ChatService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest chatRequest) {
        return chatService.saveMessageAndReplyIfNeeded(chatRequest.toVo());
    }
}
