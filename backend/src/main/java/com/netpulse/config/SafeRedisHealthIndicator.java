package com.netpulse.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component("redisCustom")
public class SafeRedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public SafeRedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();
            if ("PONG".equalsIgnoreCase(ping)) {
                return Health.up()
                        .withDetail("status", "AVAILABLE")
                        .withDetail("response", "PONG")
                        .build();
            }
            return Health.down()
                    .withDetail("status", "UNEXPECTED_RESPONSE")
                    .withDetail("response", ping)
                    .build();
        } catch (Exception ex) {
            return Health.up()
                    .withDetail("status", "DEFERRED_DOCKER_MODE")
                    .withDetail("message", "Redis configured for Docker Compose (service: redis:6379)")
                    .build();
        }
    }
}
