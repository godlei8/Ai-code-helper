package com.example.work_helper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiCodeHelper {

    @Resource
    private ChatModel qwenChatModel;

    private static final String SYSTEM_MESSAGE = "你是编程领域的小助手，帮助用产解答编程学习和求职面试相关的问题，并给出建议。" +
            "重点关注4个方向\n" +
            "1.规划清晰的编程学习路线\n" +
            "2.提供项目学习建议\n" +
            "3.给出程序员求职全流程指南（比如简历优化、投递技巧）\n" +
            "4.分享高频面试题和面试技巧\n" +
            "请用简洁易懂的语言回答，助力用户高效学习与求职。";


    public String chat(String message) {
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_MESSAGE);
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse chatResponse = qwenChatModel.chat(systemMessage,userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("AI response: {}", aiMessage);
        return aiMessage.text();
    }

    //简单对话-自定义用户消息解释|添加注释|X
    public String chatWithMessage(UserMessage userMessage) {
        ChatResponse chatResponse = qwenChatModel.chat(userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("AI response: {}", aiMessage);
        return aiMessage.text();
    }
    };
