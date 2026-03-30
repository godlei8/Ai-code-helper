package com.example.work_helper.ai;

import com.example.work_helper.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

/**
 * AI代码助手服务接口
 * 定义了与AI助手交互的各种方法，包括普通对话、基于检索增强生成(RAG)的对话以及流式对话
 */
@InputGuardrails({SafeInputGuardrail.class})
public interface AiCodeHelperService {
    /**
     * 基础对话方法
     * @param userMessage 用户输入的消息
     * @return AI助手的回复
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);

    /**
     * 带有检索增强生成(RAG)功能的对话方法
     * @param userMessage 用户输入的消息
     * @return 包含AI助手回复的结果对象
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(String userMessage);

    /**
     * 流式对话方法
     * @param memoryId 记忆ID，用于维护对话上下文
     * @param userMessage 用户输入的消息
     * @return 返回一个字符串的Flux流，用于流式输出AI助手的回复
     */
    // 流式对话
    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);
}
