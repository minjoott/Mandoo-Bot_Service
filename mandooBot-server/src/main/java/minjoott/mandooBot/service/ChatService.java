package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.decorator.ChatHistoryDecorator;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import minjoott.mandooBot.domain.dto.ChatResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final RagRepositoryDecorator ragRepositoryDecorator;
    private final PromptService promptService;
    private final ExternalAiClientDecorator externalAiClientDecorator;
    private final ChatHistoryDecorator chatHistoryDecorator;

    public ChatResponse saveAndReplyIfShould(MessageVo messageVo) {
        // 1) 답장이 필요한 메시지인지 여부를 판단
        boolean hasReply = shouldReply(messageVo);

        // 2) 답장이 필요한 경우, 답장 생성 후 메모리에 대화 내역 저장
        String reply = hasReply ? createReplyAndSaveChatHistory(messageVo) : null;

        // 3) 메시지를 임베딩과 함께 DB에 저장
        ragRepositoryDecorator.saveMessageWithEmbedding(messageVo);

        // 4) 답장 여부 및 내용을 포함해 응답 객체 생성 후 반환
        return new ChatResponse(hasReply, reply);
    }

    private boolean shouldReply(MessageVo messageVo) {
        return !messageVo.isGroupChat() || messageVo.getMsg().startsWith("만두야");
    }

    private String createReplyAndSaveChatHistory(MessageVo messageVo) {
        // ✅ 고민!!!!!! 책임 분리해야 하나? 일단 saveChatHistory는 비즈니스 로직에서 핵심 로직이 아니고,
        //    createReply가 선행되어야만 하는 작업이기 때문에 하나의 메서드로 처리했음.
        String room = messageVo.getRoom();
        String sender = messageVo.getSender();
        String query = messageVo.getMsg();
        String dateTime = messageVo.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);  // ✅ LocalDateTime 타입으로 변경

        // 1) 해당 채팅방(room)에서 봇과의 최근 대화 기록(메모리)을 조회
        List<ChatHistoryVo> recentChatHistoryVo = chatHistoryDecorator.findRecentChatHistory(room);

        // 2) 현재 사용자의 메시지(query)에 대해 RAG 컨텍스트가 필요한지 판단
        boolean isRagContextNeeded = needsRagContext(query);

        // 3) RAG 컨텍스트 필요 여부에 따라 다음의 Prompt 생성
        //    - 필요O: 최근 대화 + RAG 컨텍스트를 포함한 프롬프트
        //    - 필요X: 최근 대화만 포함한 프롬프트
        Prompt prompt = promptService.buildReplyPrompt(messageVo, query, recentChatHistoryVo, isRagContextNeeded);

        // 4) 생성된 Prompt를 바탕으로 GPT에게 답변 생성 요청
        String replyByGpt = externalAiClientDecorator.getReplyByGpt(prompt);

        // 5) 원본 메시지(query)와 생성된 답변(replyByGpt), 즉 대화 내역을 메모리에 저장
        ChatHistoryVo chatHistoryVo = new ChatHistoryVo(dateTime, sender, query, replyByGpt);
        chatHistoryDecorator.saveChatHistory(room, chatHistoryVo);

        // 6) 생성된 답변을 반환
        return replyByGpt;
    }

    private boolean needsRagContext(String query) {
        Prompt prompt = promptService.buildRagContextDecisionPrompt(query);
        String replyByGpt = externalAiClientDecorator.getRagContextDecisionByGpt(prompt);
        boolean isRagContextNeeded = replyByGpt.equals("O");
        return isRagContextNeeded;
    }
}
