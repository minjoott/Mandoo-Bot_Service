package minjoott.mandooBot.service.impl.chat.reply;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.domain.vo.ChatHistory;
import minjoott.mandooBot.service.chat.reply.ChatHistoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ConcurrentMap<String, List<ChatHistory>> historyMap = new ConcurrentHashMap<>();

    @Value("${chat.history.max-turns}")
    private int historyMaxTurns;

    /**
     * 지정된 방(room)에 대해 새로운 대화 이력(ChatHistory)을 저장
     * - 저장 후, 전체 이력 수가 최대 보관 건수를 초과하면 가장 오래된 한 건을 제거
     *
     * @param room  채팅방 식별자
     * @param entry 저장할 대화 이력 엔트리
     */
    @Override
    public void saveChatHistory(String room, ChatHistory entry) {
        // 방별 리스트를 가져오거나, 없으면 새 ArrayList 생성
        List<ChatHistory> list = historyMap.computeIfAbsent(room, k -> new ArrayList<>());
        list.add(entry);
        // 최대 보관 건수 초과 시 가장 오래된 기록 제거
        if (list.size() > historyMaxTurns) {
            list.remove(0);
        }
    }

    /**
     * 지정된 방(room)의 최근 대화 이력을 조회
     * - 저장된 이력이 최대 보관 건수 이하이면 전체 반환
     * - 초과 시, 가장 최근 historyMaxTurns 건만 잘라서 반환
     * - 반환된 리스트는 읽기 전용(unmodifiable)
     *
     * @param room 채팅방 식별자
     * @return 최근 대화 이력 리스트 (읽기 전용)
     */
    @Override
    public List<ChatHistory> findRecentChatHistory(String room) {
        List<ChatHistory> list = historyMap.get(room);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }
}
