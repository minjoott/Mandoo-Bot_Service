package minjoott.mandooBot.repository;

import minjoott.mandooBot.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query(value = """
            SELECT *
              FROM messages
             WHERE sender = :sender
               AND embedding <=> CAST(:embedding AS vector) <= :threshold
             ORDER BY date_time ASC
            """,
            nativeQuery = true)
    List<Message> findMessagesWithEmbeddingBySender(
            @Param("sender") String sender,
            @Param("embedding") float[] embedding,
            @Param("threshold") double threshold
    );

    @Query(value = """
        SELECT *
          FROM messages
         WHERE room = :room
           AND embedding <=> CAST(:embedding AS vector) <= :threshold
         ORDER BY date_time ASC
        """,
            nativeQuery = true)
    List<Message> findMessagesWithEmbeddingByRoom(
            @Param("room") String room,
            @Param("embedding") float[] embedding,
            @Param("threshold") double threshold
    );
}
