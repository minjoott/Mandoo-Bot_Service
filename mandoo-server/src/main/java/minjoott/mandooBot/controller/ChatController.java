package minjoott.mandooBot.controller;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import minjoott.mandooBot.domain.dto.ChatResponse;
import minjoott.mandooBot.domain.dto.ChatRequest;
import minjoott.mandooBot.service.ChatService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest chatRequest) {
        return chatService.handleChat(chatRequest.toVo());
    }
}
