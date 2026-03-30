package com.example.work_helper.ai.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天模型监听器配置类
 * 该类用于配置和创建ChatModelListener实例，用于监听和处理聊天模型的请求、响应和错误事件
 */
@Configuration
@Slf4j
public class ChatModelListenerConfig {
    
    /**
     * 创建并返回一个ChatModelListener Bean
     * 该监听器实现了对聊天模型请求、响应和错误事件的监听和处理
     *
     * @return ChatModelListener 实例，用于监听聊天模型的各种事件
     */
    @Bean
    ChatModelListener chatModelListener() {
        return new ChatModelListener() {
            /**
             * 处理聊天模型请求事件
             * 当有新的聊天请求时，会记录请求信息到日志中
             *
             * @param requestContext 包含聊天请求上下文信息，可以获取到具体的chatRequest
             */
            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                log.info("onRequest(): {}", requestContext.chatRequest());
            }

            /**
             * 处理聊天模型响应事件
             * 当聊天模型返回响应时，会记录响应信息到日志中
             *
             * @param responseContext 包含聊天响应上下文信息，可以获取到具体的chatResponse
             */
            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                log.info("onResponse(): {}", responseContext.chatResponse());
            }

            /**
             * 处理聊天模型错误事件
             * 当聊天模型处理过程中发生错误时，会记录错误信息到日志中
             *
             * @param errorContext 包含错误上下文信息，可以获取到具体的错误信息
             */
            @Override
            public void onError(ChatModelErrorContext errorContext) {
                log.info("onError(): {}", errorContext.error().getMessage());
            }
        };
    }
}