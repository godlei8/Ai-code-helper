package com.example.work_helper.ai;

import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // Spring Boot测试注解，用于启用Spring上下文进行测试
class AiCodeHelperServiceTest { // 测试类，用于测试AiCodeHelperService的功能
    @Resource // 依赖注入注解，用于注入AiCodeHelperService实例
    private AiCodeHelperService aiCodeHelperService;

    /**
     * 测试聊天功能的方法
     * 使用aiCodeHelperService的chat方法进行交互
     * 打印聊天结果
     */
    @Test // 标记这是一个JUnit测试方法
    void chat() { // 测试方法，用于验证聊天功能是否正常工作
        String chat = aiCodeHelperService.chat("你好，我是程序员God磊"); // 调用聊天服务，传入用户消息
        System.out.println(chat); // 输出聊天服务返回的结果
    }

    /**
     * 测试带有记忆功能的对话方法
     * 该测试用例验证AI助手是否能记住之前的对话内容
     */
    @Test // 标记这是一个JUnit测试方法
    void chatWithMemory() {  // 测试方法：测试带有记忆功能的对话
        String chat = aiCodeHelperService.chat("你好，我是程序员God磊");  // 第一次对话，告诉AI自己的身份
        System.out.println(chat);  // 输出第一次对话的回复
        String back = aiCodeHelperService.chat("你好，我是谁？");  // 第二次对话，询问AI自己的身份
        System.out.println(back);  // 输出第二次对话的回复
    }

    /**
     * 测试与RAG模型对话的方法
     * 该测试用例验证AI助手是否能正确回答问题
     */
    @Test
    void chatWithRag() {
        Result<String> chat = aiCodeHelperService.chatWithRag("怎么学习java,推荐一些面试题");
        System.out.println(chat.sources());
        System.out.println(chat.content());
    }

    /** 测试与工具对话的方法 */
    @Test
    void chatWithTools() {
        String chat = aiCodeHelperService.chat("有哪些常见的计算机网络面试题？");
        System.out.println(chat);
    }

    /** 测试与MCP模型对话的方法 */
    @Test
    void chatWithMcp() {
        String chat = aiCodeHelperService.chat("kill？");
        System.out.println(chat);
    }
}