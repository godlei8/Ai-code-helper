package com.example.work_helper.controller;

import com.example.work_helper.ai.AiCodeHelperService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestParam int memoryId, @RequestParam String message) {
        try {
            return aiCodeHelperService.chatStream(memoryId, message)
                    .map(chunk -> ServerSentEvent.<String>builder()
                            .data(chunk)
                            .build())
                    .onErrorResume(exception -> {
                        logModelFailure("Chat stream failed", memoryId, exception);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data(toUserFriendlyMessage(exception))
                                .build());
                    });
        } catch (Exception exception) {
            logModelFailure("Chat stream initialization failed", memoryId, exception);
            return Flux.just(ServerSentEvent.<String>builder()
                    .data(toUserFriendlyMessage(exception))
                    .build());
        }
    }

    private void logModelFailure(String message, int memoryId, Throwable exception) {
        if (isKnownProviderFailure(exception)) {
            log.warn("{} memoryId={}, reason={}", message, memoryId, summarizeException(exception));
            return;
        }

        log.error("{} memoryId={}", message, memoryId, exception);
    }

    private boolean isKnownProviderFailure(Throwable exception) {
        String summary = summarizeException(exception).toLowerCase();
        return summary.contains("arrearage")
                || summary.contains("overdue-payment")
                || summary.contains("invalidapikey")
                || summary.contains("invalid api-key")
                || summary.contains("unauthorized")
                || summary.contains("forbidden")
                || summary.contains("401")
                || summary.contains("403")
                || summary.contains("quota")
                || summary.contains("429")
                || summary.contains("timed out")
                || summary.contains("connection refused")
                || summary.contains("unknown host")
                || summary.contains("failed to resolve");
    }

    private String toUserFriendlyMessage(Throwable exception) {
        String summary = summarizeException(exception).toLowerCase();
        if (summary.contains("arrearage") || summary.contains("overdue-payment")) {
            return "当前模型账户状态异常或已欠费，请先检查 MiniMax 平台账单和套餐状态。";
        }
        if (summary.contains("invalidapikey")
                || summary.contains("invalid api-key")
                || summary.contains("api key")
                || summary.contains("unauthorized")
                || summary.contains("forbidden")
                || summary.contains("401")
                || summary.contains("403")) {
            return "当前模型 API Key 无效或未配置，请检查 minimax.api-key。";
        }
        if (summary.contains("quota") || summary.contains("429")) {
            return "当前模型额度不足或请求过于频繁，请稍后再试。";
        }
        if (summary.contains("timed out")
                || summary.contains("connection refused")
                || summary.contains("unknown host")
                || summary.contains("failed to resolve")) {
            return "当前模型服务网络连接失败，请检查服务器出网、DNS 和 minimax.base-url 配置。";
        }
        return "当前对话服务暂时不可用，请检查模型配置或稍后重试。";
    }

    private String summarizeException(Throwable exception) {
        StringBuilder builder = new StringBuilder();
        Throwable current = exception;
        int depth = 0;
        while (current != null && depth < 5) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" | ");
                }
                builder.append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }
        return builder.length() == 0 ? exception.getClass().getSimpleName() : builder.toString();
    }
}
