package com.netpulse.heartbeat.service;

import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);

    private final Map<String, Instant> heartbeatRegistry = new ConcurrentHashMap<>();
    private final Map<String, Boolean> suppressionRegistry = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;

    @Value("${app.heartbeat.timeout-seconds:10}")
    private long timeoutSeconds = 10;

    @Value("${app.heartbeat.suspected-threshold-seconds:5}")
    private long suspectedThresholdSeconds = 5;

    public HeartbeatService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordHeartbeat(String nodeId) {
        if (Boolean.TRUE.equals(suppressionRegistry.get(nodeId))) {
            log.debug("Heartbeat for node {} suppressed by operator configuration", nodeId);
            return;
        }

        Instant now = Instant.now();
        heartbeatRegistry.put(nodeId, now);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set("heartbeat:node:" + nodeId, String.valueOf(now.toEpochMilli()),
                        Duration.ofSeconds(timeoutSeconds * 2));
            } catch (Exception e) {
                log.debug("Redis heartbeat sync unavailable: {}", e.getMessage());
            }
        }
    }

    public void suppressHeartbeat(String nodeId, boolean suppress) {
        suppressionRegistry.put(nodeId, suppress);
        log.info("Heartbeat emission suppression for node {} set to {}", nodeId, suppress);
    }

    public boolean isHeartbeatSuppressed(String nodeId) {
        return Boolean.TRUE.equals(suppressionRegistry.get(nodeId));
    }

    public Instant getLastHeartbeat(String nodeId) {
        Instant mem = heartbeatRegistry.get(nodeId);
        if (mem != null) {
            return mem;
        }

        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get("heartbeat:node:" + nodeId);
                if (val != null) {
                    Instant redisInstant = Instant.ofEpochMilli(Long.parseLong(val));
                    heartbeatRegistry.put(nodeId, redisInstant);
                    return redisInstant;
                }
            } catch (Exception e) {
                log.debug("Redis heartbeat read unavailable: {}", e.getMessage());
            }
        }
        return null;
    }

    public LivenessStatus evaluateLiveness(String nodeId) {
        Instant last = getLastHeartbeat(nodeId);
        if (last == null) {
            return LivenessStatus.UNREACHABLE;
        }

        long ageSeconds = Duration.between(last, Instant.now()).getSeconds();
        if (ageSeconds < 0) {
            ageSeconds = 0;
        }

        if (ageSeconds < suspectedThresholdSeconds) {
            return LivenessStatus.ALIVE;
        } else if (ageSeconds < timeoutSeconds) {
            return LivenessStatus.SUSPECTED;
        } else {
            return LivenessStatus.UNREACHABLE;
        }
    }

    public HeartbeatStatusResponse getHeartbeatStatus(String nodeId) {
        Instant last = getLastHeartbeat(nodeId);
        Long ageSeconds = null;
        if (last != null) {
            ageSeconds = Math.max(0, Duration.between(last, Instant.now()).getSeconds());
        }
        LivenessStatus liveness = evaluateLiveness(nodeId);
        boolean suppressed = isHeartbeatSuppressed(nodeId);

        return new HeartbeatStatusResponse(nodeId, last, ageSeconds, liveness, suppressed);
    }

    public List<HeartbeatStatusResponse> getAllHeartbeatStatuses(List<String> nodeIds) {
        List<HeartbeatStatusResponse> list = new ArrayList<>();
        for (String nodeId : nodeIds) {
            list.add(getHeartbeatStatus(nodeId));
        }
        return list;
    }

    public void removeNode(String nodeId) {
        heartbeatRegistry.remove(nodeId);
        suppressionRegistry.remove(nodeId);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("heartbeat:node:" + nodeId);
            } catch (Exception e) {
                log.debug("Redis heartbeat delete unavailable: {}", e.getMessage());
            }
        }
    }

    public void setThresholds(long timeoutSeconds, long suspectedThresholdSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.suspectedThresholdSeconds = suspectedThresholdSeconds;
    }
}
