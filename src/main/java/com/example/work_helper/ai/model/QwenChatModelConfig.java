package com.example.work_helper.ai.model;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 配置类，用于配置QwenChatModel相关属性
 * 使用@Configuration注解标记为配置类
 * 使用@ConfigurationProperties注解将配置文件中以"langchain4j.community.dashscope.chat-model"为前缀的属性绑定到此类
 * 使用@Data注解自动生成getter、setter等方法
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.chat-model")
@Data
public class QwenChatModelConfig {

    // 模型名称属性
    private String modelName;

    // API密钥属性
    private String apiKey;

    // 注入ChatModelListener监听器
    @Resource
    private ChatModelListener chatModelListener;

    /**
     * 创建并配置ChatModel Bean
     * @return 配置好的QwenChatModel实例
     */
    @Bean
    public ChatModel myQwenChatModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)    // 设置API密钥
                .modelName(modelName)  // 设置模型名称
                .listeners(List.of(chatModelListener))  // 设置监听器列表
                .build();  // 构建并返回ChatModel实例
    }
}
