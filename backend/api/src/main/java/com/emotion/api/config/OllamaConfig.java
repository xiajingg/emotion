package com.emotion.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ollama配置类 - 创建ChatClient（其他使用自动配置）
 */
@Slf4j
@Configuration
public class OllamaConfig {

    /**
     * 创建ChatClient（基于自动配置的OllamaChatModel）
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatModel) {
        log.info("========== 创建ChatClient ==========");
        ChatClient client = ChatClient.create(ollamaChatModel);
        log.info("ChatClient 创建成功");
        log.info("=====================================");
        return client;
    }
}
