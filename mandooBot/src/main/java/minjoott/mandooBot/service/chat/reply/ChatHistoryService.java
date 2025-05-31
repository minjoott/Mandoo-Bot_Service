package minjoott.mandooBot.service.chat.reply;

import minjoott.mandooBot.domain.vo.ChatHistory;

import java.util.List;

public interface ChatHistoryService {

    /**
     * 유저와 봇 메시지를 기록
     *
     * @param room  채팅방 식별자
     * @param entry 저장할 대화 기록 엔트리
     */
    void saveChatHistory(String room, ChatHistory entry);

    /**
     * 최근 최대 maxTurns 건의 ChatHistory를 반환
     *
     * @param room     채팅방 식별자
     * @return 조회된 대화 기록 리스트 (불변)
     */
    List<ChatHistory> findRecentChatHistory(String room);
}