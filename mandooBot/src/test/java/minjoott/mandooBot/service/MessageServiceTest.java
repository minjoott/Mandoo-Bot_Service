//package minjoott.mandooBot.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.util.List;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import minjoott.mandooBot.domain.Message;
//import minjoott.mandooBot.repository.MessageRepository;
//
//@SpringBootTest
//class MessageServiceTest {
//
//    @Autowired
//    private MessageService messageService;
//
//    @Autowired
//    private MessageRepository messageRepository;
//
//    @Test
//    void testFindSimilarMessagesBySender() {
//        // given
//        Message message = Message.builder()
//                .room("민주")
//                .sender("민주")
//                .msg("만덕아, 놀자")
//                .isGroupChat(false)
//                .build();
//
//        double threshold = 0.25;
//
//        // when
//        String results = messageService.generateReply(message, threshold).toString();
//
//        // then: 결과 출력
//        System.out.println(results);
//    }
//}
