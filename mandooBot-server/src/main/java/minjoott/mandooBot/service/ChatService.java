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

    public ChatResponse saveAndReplyIfNeeded(MessageVo messageVo) {
        // 일단 DB에 메시지 무조건 저장 - 데코레이터에서 요청응답/예외처리 다 해주고 서비스에선 저장 시 리턴받아서 처리할 게 없으니 리턴값 저장 안함
        ragRepositoryDecorator.saveMessageWithEmbedding(messageVo);

        // 필요 시 답장 (RagReply로) - 여기선 답장만 받음
        boolean hasReply = hasReplyChecker(messageVo);
        String reply = hasReply ? createReplyAndSaveChatHistory(messageVo) : null;

        return ChatResponse.builder()
                .hasReply(hasReply)
                .reply(reply)
                .build();
    }

    /** 1:1 채팅이거나, “만두”를 부른 메시지면 true */
    private boolean hasReplyChecker(MessageVo messageVo) {
        return !messageVo.isGroupChat() || messageVo.getMsg().startsWith("만두");
    }

    /** 답장 만들어서 리턴 */
    private String createReplyAndSaveChatHistory(MessageVo messageVo) {  // 하나의 메서드에서... 기능 두개 수행?
        String time = messageVo.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String sender = messageVo.getSender();
        String room = messageVo.getRoom();
        String query = messageVo.getMsg();

        // 1) 임베딩 생성 + RAG 컨텍스트 조회 (private 메서드 활용)
        List<MessageVo> ragContextMessages = ragRepositoryDecorator.findMessagesWithEmbedding(messageVo);

        // 2) 최근 대화 이력 조회
        List<ChatHistoryVo> recentChatHistoryVo = chatHistoryDecorator.findRecentChatHistory(room);

        // 3) Prompt 생성
        Prompt prompt = promptService.buildRagPrompt(messageVo, ragContextMessages, recentChatHistoryVo);

        // 4) OpenAI 호출 및 답변 텍스트 추출
        String reply = externalAiClientDecorator.createGptReply(prompt);

        // 5) 요청된 메시지지와 생성된 답변을 최근 대화 이력에 저장 - 구현위치 변경필요한듯. 서비스계층에서 표현에 들어날 필요 X
        chatHistoryDecorator.saveChatHistory(room, new ChatHistoryVo(time, sender, query, reply));

        return reply;
    }
}
