package com.zhenq.config;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.checkpoint.RedisSaver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangGraph4j 工作流 Checkpoint 持久化（Redis）
 */
@Slf4j
@Configuration
public class CodeGenWorkflowCheckpointConfig {

    @Bean(destroyMethod = "")
    @ConditionalOnProperty(prefix = "app", name = "workflow-checkpoint-enabled", havingValue = "true")
    public RedisSaver codeGenWorkflowRedisSaver(AppProperties appProperties, RedisProperties redisProperties) {
        RedisSaver saver = RedisSaver.builder()
                .host(redisProperties.getHost())
                .port(redisProperties.getPort())
                .password(redisProperties.getPassword())
                .database(appProperties.getWorkflowCheckpointRedisDatabase())
                .build();
        log.info("LangGraph4j Checkpoint 已启用，Redis db={}", appProperties.getWorkflowCheckpointRedisDatabase());
        return saver;
    }
}
