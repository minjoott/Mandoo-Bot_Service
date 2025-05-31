package minjoott.mandooBot.service.impl.embedding;

import lombok.RequiredArgsConstructor;
import minjoott.mandooBot.service.embedding.EmbeddingService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * 주어진 쿼리 문자열을 임베딩 벡터로 변환하여 반환
     *
     * @param query 변환할 텍스트
     * @return float[] 형태의 임베딩 벡터
     */
    @Override
    public float[] createEmbedding(String query) {
        return embeddingModel.embed(query);
    }
}
