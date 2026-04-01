package com.example.work_helper.ai;

import com.example.work_helper.ai.tools.InterviewQuestionTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class AiCodeHelperServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(AiCodeHelperServiceFactory.class);

    @Resource
    @Lazy
    private ChatModel miniMaxChatModel;

    @Resource
    @Lazy
    private ContentRetriever contentRetriever;

    @Resource
    @Lazy
    private ToolProvider mcpToolProvider;

    @Resource
    @Lazy
    private StreamingChatModel miniMaxStreamingChatModel;

    @Bean
    @ConditionalOnMissingBean
    public AiCodeHelperService aiCodeHelperService() {
        if (miniMaxChatModel == null) {
            log.warn("MiniMax ChatModel not configured. AiCodeHelperService will not be available.");
            return null;
        }

        log.info("Building AiCodeHelperService with ChatModel: {}, StreamingChatModel: {}",
                miniMaxChatModel != null ? "configured" : "not configured",
                miniMaxStreamingChatModel != null ? "configured" : "not configured");

        // 如果StreamingChatModel已配置，则使用它构建服务
        if (miniMaxStreamingChatModel != null) {
            return AiServices.builder(AiCodeHelperService.class)
                    .chatModel(miniMaxChatModel)
                    .streamingChatModel(miniMaxStreamingChatModel)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();
        } else {
            return AiServices.builder(AiCodeHelperService.class)
                    .chatModel(miniMaxChatModel)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();
        }
    }
}
