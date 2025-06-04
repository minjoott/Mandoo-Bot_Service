package minjoott.mandooBot.decorator;

import lombok.extern.slf4j.Slf4j;
import minjoott.mandooBot.domain.vo.ChatHistoryVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class ChatHistoryDecorator {

    private final ConcurrentMap<String, List<ChatHistoryVo>> historyMap = new ConcurrentHashMap<>();

    @Value("${chat.history.max-turns}")
    private int historyMaxTurns;

    public void saveChatHistory(String room, ChatHistoryVo entry) {
        List<ChatHistoryVo> list = historyMap.computeIfAbsent(room, k -> new ArrayList<>());
        list.add(entry);
        if (list.size() > historyMaxTurns) {
            list.remove(0);
        }
    }

    public List<ChatHistoryVo> findRecentChatHistory(String room) {
        List<ChatHistoryVo> list = historyMap.get(room);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }
}
