package minjoott.mandooBot.decorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalAiClientDecorator {

    private final OpenAiChatModel openAiChatModel;
    private final EmbeddingModel embeddingModel;

    public float[] createEmbedding(String query) {
        return embeddingModel.embed(query);
    }

    public String createGptReply(Prompt prompt) {
        ChatResponse response = openAiChatModel.call(prompt);
        String reply = response.getResult().getOutput().getText();
        log.info("Created reply={} with prompt={}", reply, prompt.getInstructions());
        return reply;
    }
}
