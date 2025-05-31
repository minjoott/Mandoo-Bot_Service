package minjoott.mandooBot.service.embedding;

public interface EmbeddingService {
    /**
     * 주어진 텍스트에 대한 임베딩 벡터를 계산해 반환
     *
     * @param query 입력 텍스트
     * @return float 배열 형태의 임베딩 벡터
     */
    float[] createEmbedding(String query);
}