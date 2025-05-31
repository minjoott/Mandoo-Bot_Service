package minjoott.mandooBot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.dto.ChatResponse;
import minjoott.mandooBot.domain.entity.Message;
import minjoott.mandooBot.dto.ChatRequest;
import org.springframework.web.bind.annotation.*;
import minjoott.mandooBot.service.chat.ChatService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * 클라이언트로부터 들어온 채팅을 저장한 뒤,
     * 필요 시 AI 응답을 생성하여 반환한다.
     *
     * @param chatRequest 클라이언트 요청 DTO
     * @return 응답 여부 및 봇의 답변
     */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest chatRequest) {
        // 1) 요청 DTO → Entity 변환
        Message message = chatRequest.toEntity();

        // 2) 데이터베이스에 메시지 저장
        Message savedMessage = chatService.saveMessageWithEmbedding(message);

        // 3) 응답 필요 여부 판단
        boolean hasReply = hasReplyChecker(savedMessage);

        // 4) 필요 시 AI 응답 생성
        String reply = hasReply ? chatService.createReply(savedMessage) : null;

        // 5) 응답 반환
        ChatResponse chatResponse = new ChatResponse(hasReply, reply);
        return chatResponse;
    }

    /** 1:1 채팅이거나, “만두”를 부른 메시지면 true */
    private boolean hasReplyChecker(Message message) {
        return !message.isGroupChat() || message.getMsg().startsWith("만두");
    }
}
