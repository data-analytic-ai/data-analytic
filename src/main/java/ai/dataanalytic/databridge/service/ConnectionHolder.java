package ai.dataanalytic.databridge.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionHolder {
    private static final Map<String, Map<String, JdbcTemplate>> connections = new ConcurrentHashMap<>();

    public static void storeJdbcTemplate(String jobId, String type, JdbcTemplate jdbcTemplate) {
        connections.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>()).put(type, jdbcTemplate);
    }

    public static JdbcTemplate getJdbcTemplate(String jobId, String type) {
        Map<String, JdbcTemplate> jobConnections = connections.get(jobId);
        if (jobConnections != null) {
            return jobConnections.get(type);
        }
        return null;
    }

    public static void removeJob(String jobId) {
        connections.remove(jobId);
    }
}
