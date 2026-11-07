// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.system.api.dto.CacheKeyDto;
import dev.strata.system.api.dto.CacheStatusDto;
import dev.strata.system.communication.biz.config.CommunityFeaturesProperties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheMonitorAppService {

    private static final Logger log = LoggerFactory.getLogger(CacheMonitorAppService.class);

    private static final int SCAN_MAX_KEYS = 500;

    private static final Map<String, String> LOCAL_STORE = new ConcurrentHashMap<>();

    static {
        seedLocalDemo();
    }

    private final StringRedisTemplate redisTemplate;
    private final CommunityFeaturesProperties communityFeatures;

    public CacheMonitorAppService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider, CommunityFeaturesProperties communityFeatures) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.communityFeatures = communityFeatures;
    }

    public CacheStatusDto status() {
        if (redisTemplate != null) {
            return redisStats();
        }
        if (communityFeatures.isCacheLocalDemo()) {
            return new CacheStatusDto("local", true);
        }
        return new CacheStatusDto("none", false);
    }

    private CacheStatusDto redisStats() {
        try {
            var connection = redisTemplate.getConnectionFactory().getConnection();
            try {
                java.util.Properties info = connection.serverCommands().info();
                String version = info.getProperty("redis_version");
                String usedMemory = info.getProperty("used_memory_human");
                Long connectedClients = parseLong(info.getProperty("connected_clients"));
                long hits = parseLongOrZero(info.getProperty("keyspace_hits"));
                long misses = parseLongOrZero(info.getProperty("keyspace_misses"));
                Double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) : null;
                Long totalKeys = connection.serverCommands().dbSize();
                return new CacheStatusDto("redis", false, version, usedMemory, totalKeys, connectedClients, hitRate);
            } finally {
                connection.close();
            }
        } catch (Exception ex) {
            log.error("Error in CacheMonitorAppService: {}", ex.getMessage(), ex);
            return new CacheStatusDto("redis", false);
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    private static long parseLongOrZero(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value.trim());
    }

    public List<CacheKeyDto> listKeys(String pattern) {
        String match = (pattern == null || pattern.isBlank()) ? "*" : pattern;
        if (redisTemplate != null) {
            return listRedisKeys(match);
        }
        if (!communityFeatures.isCacheLocalDemo()) {
            return List.of();
        }
        return listLocalKeys(match);
    }

    @OperationLog(module = "system:cache", action = "delete")
    public void deleteKey(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Cache key is required");
        }
        if (redisTemplate != null) {
            redisTemplate.delete(key);
            return;
        }
        if (communityFeatures.isCacheLocalDemo()) {
            LOCAL_STORE.remove(key);
        }
    }

    @OperationLog(module = "system:cache", action = "delete")
    public void flush() {
        if (redisTemplate != null) {
            deleteRedisKeysByPattern("*");
            return;
        }
        if (communityFeatures.isCacheLocalDemo()) {
            LOCAL_STORE.clear();
            seedLocalDemo();
        }
    }

    private List<CacheKeyDto> listRedisKeys(String pattern) {
        List<CacheKeyDto> result = new ArrayList<>();
        ScanOptions options =
                ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext() && result.size() < SCAN_MAX_KEYS) {
                String key = cursor.next();
                DataType type = redisTemplate.type(key);
                Long ttl = redisTemplate.getExpire(key);
                result.add(new CacheKeyDto(key, type == null ? "unknown" : type.code(), ttl));
            }
        }
        result.sort((a, b) -> a.key().compareTo(b.key()));
        return result;
    }

    private void deleteRedisKeysByPattern(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options =
                ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private List<CacheKeyDto> listLocalKeys(String pattern) {
        String regex = globToRegex(pattern);
        List<CacheKeyDto> result = new ArrayList<>();
        for (String key : LOCAL_STORE.keySet()) {
            if (key.matches(regex)) {
                result.add(new CacheKeyDto(key, "string", -1L));
            }
        }
        result.sort((a, b) -> a.key().compareTo(b.key()));
        return result;
    }

    private static void seedLocalDemo() {
        LOCAL_STORE.put("i18n:en", "{\"nav.dashboard\":\"Dashboard\"}");
        LOCAL_STORE.put("i18n:zh", "{\"nav.dashboard\":\"首页\"}");
        LOCAL_STORE.put("session:demo", "user:admin");
        LOCAL_STORE.put("login-lock:admin", "0");
        LOCAL_STORE.put("config:site", "{\"theme\":\"light\"}");
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|' ->
                    sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        return sb.append('$').toString();
    }
}
