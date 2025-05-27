package minjoott.mandooBot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * 텍스트 → 벡터(float[]) 계산
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * 주어진 텍스트에 대한 임베딩 벡터를 계산해 반환
     */
    public float[] embed(String message) {
        return embeddingModel.embed(message);
    }
}
