package com.example.work_helper.controller;

import com.example.work_helper.ai.AiCodeHelperService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * REST控制器，处理与AI相关的HTTP请求
 * 路径前缀为"/ai"
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    /**
     * 注入AiCodeHelperService服务
     * 用于处理AI代码相关的业务逻辑
     */
    @Resource
    private AiCodeHelperService aiCodeHelperService;

    /**
     * 处理GET请求"/ai/chat"
     * 提供聊天功能，返回服务器发送事件(SSE)流
     *
     * @param memoryId 记忆ID，用于标识会话
     * @param message 用户发送的消息内容
     * @return Flux<ServerSentEvent<String>> 服务器发送事件流，每个事件包含聊天消息块
     */
    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message) {
        return aiCodeHelperService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
}