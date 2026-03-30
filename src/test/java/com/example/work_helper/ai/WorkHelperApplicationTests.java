package com.example.work_helper.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkHelperApplicationTests {
    @Resource
    private AiCodeHelper aiCodeHelper;

    @Test
    void contextLoads() {
        aiCodeHelper.chat("你好，我是程序员God磊");
    }
}
