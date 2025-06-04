package minjoott.mandooBot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.vo.MessageVo;
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
        // 1) 요청 DTO → VO 변환
        MessageVo messageVo = chatRequest.toVo();

        // 메시지를 저장하고 필요 시 답장
        ChatResponse chatResponse = chatService.saveAndReplyIfNeeded(messageVo);

        // 5) VO -> DTO 변환 후 응답 반환
        return chatResponse;
    }
}
