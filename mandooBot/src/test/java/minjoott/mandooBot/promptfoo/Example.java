package minjoott.mandooBot.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import minjoott.mandooBot.domain.Message;
import minjoott.mandooBot.repository.MessageRepository;

@SpringBootTest
class Example {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void testFindSimilarMessagesBySender() {
        // given
        Message message = Message.builder()
                .room("민주")
                .sender("민주")
                .msg("테스트 관련 질문들을 내가 뭐 했지?")
                .isGroupChat(false)
                .build();

        double threshold = 0.25;

        // when
        String results = messageService.generateReply(message, threshold);

        // then: 결과 출력
        System.out.println(results);
    }
}
