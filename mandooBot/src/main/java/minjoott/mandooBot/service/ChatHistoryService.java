package minjoott.mandooBot.service;

import minjoott.mandooBot.domain.ChatHistory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 채팅방(room)별 최근 대화 이력을 메모리에 보관
 */
@Service
public class ChatHistoryService {
    private final ConcurrentMap<String, List<ChatHistory>> historyMap = new ConcurrentHashMap<>();
    private static final int MAX_TURNS = 5;

    /**
     * 최근 MAX_TURNS 건의 ChatHistory 를 반환
     */
    public List<ChatHistory> getRecentHistory(String room, int maxTurns) {
        List<ChatHistory> list = historyMap.getOrDefault(room, new ArrayList<>());
        int size = list.size();
        if (size <= maxTurns) {
            return Collections.unmodifiableList(list);
        }
        return Collections.unmodifiableList(
                new ArrayList<>(list.subList(size - maxTurns, size))
        );
    }

    /**
     * 유저 and 봇 메시지를 기록
     */
    public void saveChatHistory(String room, ChatHistory entry) {
        List<ChatHistory> list = historyMap.computeIfAbsent(room, k -> new ArrayList<>());
        list.add(entry);
        // trim old entries
        if (list.size() > MAX_TURNS) {
            list.remove(0);
        }
    }
}
