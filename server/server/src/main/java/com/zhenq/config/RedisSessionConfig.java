package com.zhenq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis 分布式 Session 配置
 * <p>
 * 登录态存储在 Redis，支持多实例部署共享 Session。
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400 * 30)
public class RedisSessionConfig {
}
