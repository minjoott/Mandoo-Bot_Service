package minjoott.mandooBot.config;

import lombok.experimental.UtilityClass;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;

@UtilityClass
public final class OpenAiOptions {

    public final OpenAiEmbeddingOptions EMBEDDING = OpenAiEmbeddingOptions.builder()
            .model("text-embedding-ada-002")
            .build();

    public final OpenAiChatOptions RAG_CONTEXT_DECISION = OpenAiChatOptions.builder()
            .model("gpt-4.1")
            .temperature(0.0)
            .topP(1.0)
            .maxTokens(5)
            .build();

    public final OpenAiChatOptions RAG_CONTEXT_FILTER = OpenAiChatOptions.builder()
            .model("gpt-4.1")
            .temperature(0.0)
            .topP(1.0)
            .maxTokens(5000)
            .build();

    public final OpenAiChatOptions REPLY = OpenAiChatOptions.builder()
            .model("gpt-4.1")
            .temperature(0.5)
            .topP(0.3)
            .presencePenalty(0.3)
            .maxTokens(250)
            .build();
}
