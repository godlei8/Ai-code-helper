package com.example.work_helper.ai.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    @Value("${minimax.api-key:}")
    private String apiKey;

    @Value("${minimax.api-host:https://api.minimaxi.com}")
    private String apiHost;

    @Value("${minimax.mcp.command:uvx}")
    private String mcpCommand;

    @Value("${minimax.mcp.base-path:target/minimax-mcp}")
    private String mcpBasePath;

    @Value("${minimax.mcp.resource-mode:url}")
    private String resourceMode;

    @Bean
    public ToolProvider mcpToolProvider() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("MCP initialization skipped, minimax.api-key is not configured");
            return request -> new ToolProviderResult(Map.of());
        }

        try {
            Path basePath = Path.of(mcpBasePath).toAbsolutePath();
            Files.createDirectories(basePath);

            McpTransport transport = new StdioMcpTransport.Builder()
                    .command(List.of(mcpCommand, "minimax-coding-plan-mcp", "-y"))
                    .environment(Map.of(
                            "MINIMAX_API_KEY", apiKey,
                            "MINIMAX_API_HOST", apiHost,
                            "MINIMAX_MCP_BASE_PATH", basePath.toString(),
                            "MINIMAX_API_RESOURCE_MODE", resourceMode
                    ))
                    .logEvents(true)
                    .build();

            McpClient mcpClient = new DefaultMcpClient.Builder()
                    .key("miniMaxCodingPlanMcp")
                    .transport(transport)
                    .build();

            return McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .build();
        } catch (Exception exception) {
            log.warn("MCP initialization failed, MCP tools disabled: {}", exception.getMessage());
            return request -> new ToolProviderResult(Map.of());
        }
    }
}
