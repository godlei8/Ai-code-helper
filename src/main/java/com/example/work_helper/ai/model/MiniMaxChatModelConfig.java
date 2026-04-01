package com.example.work_helper.ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.util.List;

@Configuration
public class MiniMaxChatModelConfig {

    private static final Logger log = LoggerFactory.getLogger(MiniMaxChatModelConfig.class);

    @Value("${minimax.api-key:}")
    private String apiKey;

    @Value("${minimax.base-url:https://api.minimaxi.com/v1}")
    private String baseUrl;

    @Value("${minimax.chat-model.model-name:MiniMax-M2.7}")
    private String chatModelName;

    @Value("${minimax.streaming-chat-model.model-name:MiniMax-M2.7}")
    private String streamingChatModelName;

    @Value("${minimax.chat-model.timeout-seconds:120}")
    private long chatTimeoutSeconds;

    @Value("${minimax.streaming-chat-model.timeout-seconds:120}")
    private long streamingTimeoutSeconds;

    @Resource
    @Lazy
    private ChatModelListener chatModelListener;

    @Bean
    @Lazy
    public ChatModel miniMaxChatModel() {
        if (!isConfigured()) {
            log.warn("MiniMax ChatModel is not configured. AI chat features will be disabled. " +
                    "Set 'minimax.api-key' in application configuration to enable.");
            return null;
        }

        log.info("MiniMax API key configured, enabling chat model with baseUrl: {}, model: {}",
                baseUrl, chatModelName);

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(chatModelName)
                .timeout(Duration.ofSeconds(chatTimeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .listeners(List.of(chatModelListener))
                .build();
    }

    @Bean
    @Lazy
    public StreamingChatModel miniMaxStreamingChatModel() {
        if (!isConfigured()) {
            log.warn("MiniMax StreamingChatModel is not configured. Streaming AI features will be disabled.");
            return null;
        }

        log.info("MiniMax StreamingChatModel configured with baseUrl: {}, model: {}",
                baseUrl, streamingChatModelName);

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(streamingChatModelName)
                .timeout(Duration.ofSeconds(streamingTimeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .listeners(List.of(chatModelListener))
                .build();
    }

    private boolean isConfigured() {
        return !isBlankOrPlaceholder(apiKey);
    }

    private boolean isBlankOrPlaceholder(String value) {
        if (value == null) {
            return true;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return true;
        }

        String lowerCase = normalized.toLowerCase();
        return normalized.startsWith("<")
                || normalized.contains("${")
                || lowerCase.contains("your minimax api key");
    }
}
