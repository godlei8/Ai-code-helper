package com.example.work_helper.ai;

import com.example.work_helper.ai.tools.InterviewQuestionTool;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeHelperServiceFactory {
    @Resource
    private ChatModel myQwenChatModel;  // 注入聊天模型，用于处理AI对话
    @Resource
    private ContentRetriever contentRetriever;  // 注入内容检索器，用于获取相关内容
    @Resource
    private McpToolProvider mcpToolProvider; // 注入工具提供者，用于管理外部工具
    @Resource
    private StreamingChatModel qwenStreamingChatModel; // 注入流式聊天模型，用于流式对话

    /**
     * 创建并配置AiCodeHelperService的Bean实例。
     * 该方法使用LangChain4j的AiServices构建器创建服务实例，
     * 并注入聊天模型和会话记忆功能。
     *
     * @return 配置好的AiCodeHelperService实例，由Spring容器管理
     */
    @Bean
    public AiCodeHelperService aiCodeHelperService() {
        // 配置会话记忆，限制保留最近10条消息
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        
        // 构建AiCodeHelperService实例，注入聊天模型和记忆组件
        return AiServices.builder(AiCodeHelperService.class)
                .chatModel(myQwenChatModel) // 聊天模型，用于AI对话处理
                .chatMemory(chatMemory) // 可选，用于会话记忆
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10)) // 可选，用于提供自定义的会话记忆组件
                .contentRetriever(contentRetriever) // 可选，用于检索外部内容
                .tools(new InterviewQuestionTool()) // 可选，用于集成外部工具
                .toolProvider(mcpToolProvider) // 可选，用于管理外部工具
                .streamingChatModel(qwenStreamingChatModel) // 流式聊天模型，用于流式对话
                .build();
    }
}
