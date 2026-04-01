package com.example.work_helper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiCodeHelper {

    private static final String SYSTEM_MESSAGE = """
            你是编程领域的小助手，帮助用户解答编程学习和求职面试相关的问题，并给出建议。
            请重点关注以下方向：
            1. 规划清晰的编程学习路线
            2. 提供项目学习和实战建议
            3. 给出程序员求职全流程指导，包括简历优化和投递技巧
            4. 分享高频面试题和面试技巧
            请用简洁、易懂、可执行的语言回答。
            """;

    @Resource
    private ChatModel miniMaxChatModel;

    public String chat(String message) {
        if (miniMaxChatModel == null) {
            return "AI功能未配置，请联系管理员配置MiniMax API Key。";
        }

        try {
            SystemMessage systemMessage = SystemMessage.from(SYSTEM_MESSAGE);
            UserMessage userMessage = UserMessage.from(message);
            ChatResponse chatResponse = miniMaxChatModel.chat(systemMessage, userMessage);
            AiMessage aiMessage = chatResponse.aiMessage();
            log.info("AI response: {}", aiMessage);
            return aiMessage.text();
        } catch (Exception e) {
            log.error("Failed to get AI response", e);
            return "AI服务调用失败: " + e.getMessage();
        }
    }

    public String chatWithMessage(UserMessage userMessage) {
        if (miniMaxChatModel == null) {
            return "AI功能未配置，请联系管理员配置MiniMax API Key。";
        }

        try {
            ChatResponse chatResponse = miniMaxChatModel.chat(userMessage);
            AiMessage aiMessage = chatResponse.aiMessage();
            log.info("AI response: {}", aiMessage);
            return aiMessage.text();
        } catch (Exception e) {
            log.error("Failed to get AI response", e);
            return "AI服务调用失败: " + e.getMessage();
        }
    }
}
