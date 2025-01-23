package dev.ctsekes.translator.config.ollama;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OllamaChatModelConfig {

    @Value("${ollama.base-url}")
    private String baseURL;

    @Value("${ollama.model-name}")
    private String modelName;

    @Bean
    ChatLanguageModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseURL)
                .modelName(modelName)
                .temperature(0.0)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofMinutes(2))
                .build();
    }
}
