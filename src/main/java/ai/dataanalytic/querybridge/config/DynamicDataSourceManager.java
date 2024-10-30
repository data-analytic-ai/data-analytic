package ai.dataanalytic.querybridge.config;


import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class to manage dynamic data sources.
 */
@Slf4j
@Service
public class DynamicDataSourceManager {

    // Cache to store JdbcTemplate instances by session ID
    private final Map<String, JdbcTemplate> dataSourceCache = new ConcurrentHashMap<>();

    // Database drivers
    private static final Map<String, String> DRIVER_MAP = Map.of(
            "postgresql", "org.postgresql.Driver",
            "mysql", "com.mysql.cj.jdbc.Driver",
            "mongodb", "org.mongodb.driver"
    );

    /**
     * Creates and tests a new database connection using the provided credentials.
     *
     * @param credentials the database credentials
     * @return JdbcTemplate if the connection is successful, null otherwise
     */
    public JdbcTemplate createAndTestConnection(DatabaseConnectionRequest credentials) {
        try {
            DataSource dataSource = createDataSource(credentials);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            if (testConnection(jdbcTemplate)) {
                return jdbcTemplate;
            } else {
                closeDataSource(dataSource);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating and testing connection", e);
            return null;
        }
    }

    /**
     * Tests the connection using the provided JdbcTemplate.
     *
     * @param jdbcTemplate the JdbcTemplate to test
     * @return true if the connection is successful, false otherwise
     */
    private boolean testConnection(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("Error testing connection", e);
            return false;
        }
    }

    /**
     * Creates a DataSource using the provided credentials.
     *
     * @param credentials the database credentials
     * @return the created DataSource
     */
    private DataSource createDataSource(DatabaseConnectionRequest credentials) {
        HikariConfig hikariConfig = new HikariConfig();

        // Set the driver class name based on the database type
        String driverClassName = DRIVER_MAP.get(credentials.getDatabaseType().toLowerCase());
        if (driverClassName == null) {
            throw new IllegalArgumentException("Unsupported database type: " + credentials.getDatabaseType());
        }
        hikariConfig.setDriverClassName(driverClassName);

        // Build the JDBC URL based on the database type
        String jdbcUrl = buildJdbcUrl(credentials);
        hikariConfig.setJdbcUrl(jdbcUrl);

        hikariConfig.setUsername(credentials.getUserName());
        hikariConfig.setPassword(credentials.getPassword());

        // Optional: Configure pool settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTimeout(30000);

        return new HikariDataSource(hikariConfig);
    }

    /**
     * Builds the JDBC URL based on the database type and credentials.
     *
     * @param credentials the database credentials
     * @return the JDBC URL
     */
    private String buildJdbcUrl(DatabaseConnectionRequest credentials) {
        String databaseType = credentials.getDatabaseType().toLowerCase();
        String host = credentials.getHost();
        int port = credentials.getPort();
        String databaseName = credentials.getDatabaseName();

        switch (databaseType) {
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
            case "oracle":
                // For Oracle, additional parameters like SID or service name might be needed
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, credentials.getSid());
            // Add more cases as needed
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
    }

    /**
     * Closes the data source.
     *
     * @param dataSource the data source to close
     */
    private void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }
}