package minjoott.mandooBot.repository;

import minjoott.mandooBot.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 1:1 채팅 (sender 기준) 에서
     * embedding 벡터와의 거리가 threshold 이내인 메시지 조회
     */
    @Query(value = """
        SELECT *
          FROM messages
         WHERE sender = :sender
           AND embedding <=> CAST(:vector AS vector) <= :threshold
        """,
            nativeQuery = true)
    List<Message> findContextMessagesBySender(
            @Param("sender") String sender,
            @Param("vector") float[] vector,
            @Param("threshold") double threshold
    );

    /**
     * 그룹 채팅 (room 기준) 에서
     * embedding 벡터와의 거리가 threshold 이내인 메시지 조회
     */
    @Query(value = """
        SELECT *
          FROM messages
         WHERE room = :room
           AND embedding <=> CAST(:vector AS vector) <= :threshold
        """,
            nativeQuery = true)
    List<Message> findContextMessagesByRoom(
            @Param("room") String room,
            @Param("vector") float[] vector,
            @Param("threshold") double threshold
    );

    // ----------------------------------TEST----------------------------------
    @Query(
            value = """
                      SELECT *
                        FROM messages
                       WHERE embedding <=> CAST(:vector AS vector) <= :threshold
                    """,
            nativeQuery = true
    )
    List<Message> findBySimilarityAbove(
            @Param("vector") float[] vector,
            @Param("threshold") double threshold
    );
}
