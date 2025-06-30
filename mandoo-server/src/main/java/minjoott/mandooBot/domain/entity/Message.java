package minjoott.mandooBot.domain.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import minjoott.mandooBot.domain.vo.RagContextMessageVo;
import minjoott.mandooBot.domain.vo.SavedMessageVo;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

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

    public SavedMessageVo toMessageVo() {
        return SavedMessageVo.builder()
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
