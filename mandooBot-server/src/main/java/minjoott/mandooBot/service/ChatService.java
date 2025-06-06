package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.decorator.ChatHistoryDecorator;
import minjoott.mandooBot.decorator.ExternalAiClientDecorator;
import minjoott.mandooBot.decorator.RagRepositoryDecorator;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import minjoott.mandooBot.domain.dto.ChatResponse;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final RagRepositoryDecorator ragRepositoryDecorator;
    private final PromptService promptService;
    private final ExternalAiClientDecorator externalAiClientDecorator;
    private final ChatHistoryDecorator chatHistoryDecorator;

    public ChatResponse saveAndReplyIfNeeded(MessageVo messageVo) {
        // 필요 시 답장 - 여기선 답장만 받음
        boolean hasReply = hasReplyChecker(messageVo);
        String reply = hasReply ? createReplyAndSaveChatHistory(messageVo) : null;

        // 일단 DB에 메시지 무조건 저장 - 데코레이터에서 요청응답/예외처리 다 해주고 서비스에선 저장 시 리턴받아서 처리할 게 없으니 리턴값 저장 안함
        ragRepositoryDecorator.saveMessageWithEmbedding(messageVo);

        return ChatResponse.builder()
                .hasReply(hasReply)
                .reply(reply)
                .build();
    }

    /** 1:1 채팅이거나, “만두”를 부른 메시지면 true */
    private boolean hasReplyChecker(MessageVo messageVo) {
        return !messageVo.isGroupChat() || messageVo.getMsg().startsWith("만두야");
    }

    /** 답장 만들어서 리턴 */
    private String createReplyAndSaveChatHistory(MessageVo messageVo) {  // 하나의 메서드에서... 기능 두개 수행?
        String room = messageVo.getRoom();
        String sender = messageVo.getSender();
        String query = messageVo.getMsg();
        String time = messageVo.getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // 2) 최근 대화 이력 조회
        List<ChatHistoryVo> recentChatHistoryVo = chatHistoryDecorator.findRecentChatHistory(room);

        // RAG Context 필요한 메시지인지 판단
        boolean isRagContextNeeded = needsRag(query);

        // isRagContextNeeded 값에 따라 context 보충 결정
        Prompt prompt = getPrompt(messageVo, query, recentChatHistoryVo, isRagContextNeeded);

        // 4) OpenAI 호출 및 답변 텍스트 추출
        String reply = externalAiClientDecorator.createReplyByGpt(prompt);

        // 5) 요청된 메시지지와 생성된 답변을 최근 대화 이력에 저장 - 구현위치 변경필요한듯. 서비스계층에서 표현에 들어날 필요 X
        chatHistoryDecorator.saveChatHistory(room, new ChatHistoryVo(time, sender, query, reply));

        return reply;
    }

    private boolean needsRag(String query) {
        Prompt prompt = promptService.buildRagCheckerPrompt(query);
        String replyByGpt = externalAiClientDecorator.createReplyByGpt(prompt);
        return replyByGpt.equals("O");
    }

    private Prompt getPrompt(MessageVo messageVo, String query, List<ChatHistoryVo> recentChatHistoryVo, boolean isRagContextNeeded) {
        Prompt prompt;
        if (isRagContextNeeded) {
            // CASE1: RAG Context 필요 O
            List<RagContextMessageVo> ragContextMessages = ragRepositoryDecorator.findMessagesWithEmbedding(messageVo);

            // 조회된 RAG Context가 있다면 필터링, 없다면 empty 저장
            boolean hasRagContext = !ragContextMessages.isEmpty();
            List<RagContextMessageVo> filteredRagContextMessageVos;
            if (hasRagContext) {
                Prompt filterPrompt = promptService.buildRagContextFilterPrompt(query, ragContextMessages);
                filteredRagContextMessageVos = externalAiClientDecorator.getFilteredRagContextByGpt(filterPrompt);
            }
            else {
                filteredRagContextMessageVos = Collections.emptyList();
            }

            prompt = promptService.buildRagPrompt(messageVo, filteredRagContextMessageVos, recentChatHistoryVo);
        }
        else {
            // CASE2: RAG Context 필요 X
            prompt = promptService.buildSimplePrompt(messageVo, recentChatHistoryVo);
        }
        return prompt;
    }
}
