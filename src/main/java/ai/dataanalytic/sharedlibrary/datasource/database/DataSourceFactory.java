package ai.dataanalytic.sharedlibrary.datasource.database;

import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Component
public class DataSourceFactory {

    private static final Map<String, String> DRIVER_MAP = Map.of(
            "postgresql", "org.postgresql.Driver",
            "mysql", "com.mysql.cj.jdbc.Driver",
            "mariadb", "org.mariadb.jdbc.Driver",
            "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "oracle", "oracle.jdbc.OracleDriver",
            "db2", "com.ibm.db2.jcc.DB2Driver",
            "mongodb", "mongodb.jdbc.MongoDriver"
    );

    public DataSource createDataSource(DatabaseConnectionRequest credentials) {
        log.info("Creating data source for database type: {}", credentials.getDatabaseType());

        HikariConfig hikariConfig = new HikariConfig();
        String driverClassName = DRIVER_MAP.get(credentials.getDatabaseType().toLowerCase());

        if (driverClassName == null) {
            log.error("Unsupported database type: {}", credentials.getDatabaseType());
            throw new IllegalArgumentException("Unsupported database type: " + credentials.getDatabaseType());
        }

        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setJdbcUrl(buildJdbcUrl(credentials));
        hikariConfig.setUsername(credentials.getUserName());
        hikariConfig.setPassword(credentials.getPassword());

        // Advanced Pool Configuration
        hikariConfig.setMinimumIdle(5); // Set minimum idle connections
        hikariConfig.setMaximumPoolSize(20); // Max connections in the pool
        hikariConfig.setConnectionTimeout(30000); // Timeout for connection acquisition
        hikariConfig.setIdleTimeout(600000); // Connection idle time before eviction
        hikariConfig.setMaxLifetime(1800000); // Max lifetime for a connection

        log.debug("DataSource created with URL: {}", hikariConfig.getJdbcUrl());

        return new HikariDataSource(hikariConfig);
    }

    private String buildJdbcUrl(DatabaseConnectionRequest credentials) {
        return String.format("jdbc:%s://%s:%d/%s",
                credentials.getDatabaseType(),
                credentials.getHost(),
                credentials.getPort(),
                credentials.getDatabaseName());
    }
}
