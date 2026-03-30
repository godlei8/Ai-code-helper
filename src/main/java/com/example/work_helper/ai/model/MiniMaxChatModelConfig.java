package com.example.work_helper.ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class MiniMaxChatModelConfig {

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
    private ChatModelListener chatModelListener;

    @Bean
    public ChatModel miniMaxChatModel() {
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
    public StreamingChatModel miniMaxStreamingChatModel() {
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
}
