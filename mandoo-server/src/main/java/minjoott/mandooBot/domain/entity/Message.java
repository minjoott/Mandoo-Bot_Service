package minjoott.mandooBot.domain.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import minjoott.mandooBot.domain.dto.RedisChatTurn;
import minjoott.mandooBot.domain.vo.ChatTurnVo;
import minjoott.mandooBot.domain.vo.MessageVo;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String room;
    private String sender;
    private String msg;

    @Column(name = "is_group")
    private boolean isGroupChat;

    @Column(name = "date_time")
    private LocalDateTime dateTime;

    @Column(columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    private float[] embedding;

    public static List<RagContextMessageVo> toVoList(List<Message> messages) {
        return messages.stream()
                .map(Message::toRagContextMessageVo)
                .collect(Collectors.toList());
    }

    public MessageVo toMessageVo() {
        return MessageVo.builder()
                .id(this.id)
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .dateTime(this.dateTime)
                .build();
    }

    public RagContextMessageVo toRagContextMessageVo() {
        return RagContextMessageVo.builder()
                .sender(this.sender)
                .msg(this.msg)
                .dateTime(this.dateTime)
                .build();
    }

}
