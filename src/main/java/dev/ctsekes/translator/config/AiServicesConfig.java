package dev.ctsekes.translator.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServicesConfig {

    @Bean
    public Assistant assistant(
            ChatLanguageModel chatLanguageModel
    ) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }
}
