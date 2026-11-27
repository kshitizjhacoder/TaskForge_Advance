package com.kshitiz.taskforge.ai.adapter.llm;

import com.kshitiz.taskforge.ai.application.AiProperties;
import com.kshitiz.taskforge.ai.domain.port.AiAssistant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses which {@link AiAssistant} the application uses, from one setting:
 * {@code spring.ai.model.chat} (environment variable {@code AI_PROVIDER}).
 *
 * <pre>
 *   google-genai  real Google Gemini (needs GEMINI_API_KEY)
 *   fake          fixed answers, no network (development and tests)
 *   none          AI switched off (default)
 * </pre>
 * The same setting tells Spring AI whether to create its Gemini {@link ChatModel}, so there is
 * a single switch for the whole feature. Spring calls {@code close()} on the Gemini adapter at
 * shutdown (it implements {@link AutoCloseable}) to stop its worker threads.
 */
@Slf4j
@Configuration
public class AiAssistantConfig {

    /**
     * Creates the AI adapter for the configured provider.
     *
     * @param provider   value of {@code spring.ai.model.chat}
     * @param chatModel  Spring AI's chat model, only present when provider is google-genai
     * @param properties AI limits (timeout)
     * @return the adapter
     * @throws IllegalStateException if Gemini is selected but Spring AI created no model
     */
    @Bean
    public AiAssistant aiAssistant(@Value("${spring.ai.model.chat:none}") String provider,
                                   ObjectProvider<ChatModel> chatModel, AiProperties properties) {
        AiAssistant assistant = switch (provider) {
            case "google-genai" -> {
                ChatModel model = chatModel.getIfAvailable();
                if (model == null) {
                    throw new IllegalStateException(
                            "AI_PROVIDER=google-genai but no Gemini model was configured. Set GEMINI_API_KEY.");
                }
                yield new GeminiAiAssistant(model, properties.timeout());
            }
            case "fake" -> new FakeAiAssistant();
            default -> new DisabledAiAssistant();
        };
        log.info("AI provider: {}", assistant.providerName());
        return assistant;
    }
}
