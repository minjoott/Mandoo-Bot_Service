package minjoott.mandooBot.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import minjoott.mandooBot.domain.vo.MessageVo;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = "embedding")
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

    /** Entity → VO 변환 메서드 */
    public MessageVo toVo() {
        return MessageVo.builder()
                .room(this.room)
                .sender(this.sender)
                .msg(this.msg)
                .isGroupChat(this.isGroupChat)
                .dateTime(this.dateTime)
                .build();
    }
}
