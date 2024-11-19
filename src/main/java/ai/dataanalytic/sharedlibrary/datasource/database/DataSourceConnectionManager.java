package ai.dataanalytic.sharedlibrary.datasource.database;

import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DataSourceConnectionManager {

    private final Map<String, Map<String, JdbcTemplate>> dataSourceCache = new ConcurrentHashMap<>();
    private final DataSourceFactory dataSourceFactory;
    private final ConnectionTester connectionTester;

    @Autowired
    public DataSourceConnectionManager(DataSourceFactory dataSourceFactory, ConnectionTester connectionTester) {
        this.dataSourceFactory = dataSourceFactory;
        this.connectionTester = connectionTester;
        log.info("DataSourceConnectionManager initialized with factory and connection tester");
    }

    public boolean createAndTestConnection(DatabaseConnectionRequest credentials) {
        String outerKey = credentials.getDatabaseType().toLowerCase();
        String innerKey = generateHashKey(credentials);

        log.info("Creating and testing connection for outer key: {}, inner key: {}", outerKey, innerKey);

        dataSourceCache.computeIfAbsent(outerKey, k -> new ConcurrentHashMap<>());

        JdbcTemplate jdbcTemplate = dataSourceCache.get(outerKey).computeIfAbsent(innerKey, k -> {
            log.debug("Creating new DataSource for key: {}", innerKey);
            DataSource dataSource = dataSourceFactory.createDataSource(credentials);
            log.debug("DataSource created for key: {}", innerKey);
            return new JdbcTemplate(dataSource);
        });

        if (connectionTester.testConnection(jdbcTemplate)) {
            log.info("Connection successful for key: {}", innerKey);
            return true;
        } else {
            closeDataSource(outerKey, innerKey);
            log.warn("Connection failed for key: {}", innerKey);
            return false;
        }
    }

    public JdbcTemplate getJdbcTemplateForDb(String databaseType, String key) {
        log.debug("Retrieving JdbcTemplate for database type: {}, key: {}", databaseType, key);
        return dataSourceCache.getOrDefault(databaseType, Map.of()).getOrDefault(key, null);
    }

    public void closeDataSource(String databaseType, String key) {
        log.info("Closing DataSource for type: {} and key: {}", databaseType, key);
        Map<String, JdbcTemplate> typeMap = dataSourceCache.get(databaseType);

        if (typeMap != null) {
            JdbcTemplate jdbcTemplate = typeMap.remove(key);
            if (typeMap.isEmpty()) {
                dataSourceCache.remove(databaseType);
                log.debug("Removed outer key: {} as no more inner keys exist", databaseType);
            }
            if (jdbcTemplate != null) {
                DataSource dataSource = jdbcTemplate.getDataSource();
                if (dataSource instanceof HikariDataSource) {
                    ((HikariDataSource) dataSource).close();
                    log.info("HikariDataSource closed for key: {}", key);
                }
            }
        }
    }

    public String generateHashKey(DatabaseConnectionRequest credentials) {
        String rawKey = credentials.getDatabaseType() + "-" + credentials.getHost() + "-" +
                credentials.getPort() + "-" + credentials.getDatabaseName() + "-" + credentials.getUserName();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            log.debug("Generated hash key: {} for raw key: {}", hexString, rawKey);
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating hash key for raw key: {}", rawKey, e);
            return rawKey; // Fallback to raw key
        }
    }
}
