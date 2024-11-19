package ai.dataanalytic.querybridge.config;


import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Service class to manage dynamic data sources.
 */
@Slf4j
@Service
public class DynamicDataSourceManager {


    // Database drivers
    private static final Map<String, String> DRIVER_MAP = Map.of(
            "postgresql", "org.postgresql.Driver",
            "mysql", "com.mysql.cj.jdbc.Driver",
            "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "oracle", "oracle.jdbc.driver.OracleDriver",
            "db2", "com.ibm.db2.jcc.DB2Driver"
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
        hikariConfig.setMaximumPoolSize(1000);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(30000);

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

        return switch (databaseType) {
            case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
            case "sqlserver" -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
            case "oracle" -> {
                String sidOrService = credentials.getSid();
                if (sidOrService == null || sidOrService.isEmpty()) {
                    throw new IllegalArgumentException("SID or Service Name is required for Oracle connections.");
                }
                yield String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, sidOrService);
            }
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        };
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