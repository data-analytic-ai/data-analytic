package ai.dataanalytic.querybridge.datasource;

import ai.dataanalytic.sharedlibrary.dto.MultiDatabaseConnectionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MultiDatabaseConnectionManager {

    private final Map<String, DataSource> dataSourceMap = new HashMap<>();

    /**
     * Connects to multiple databases provided in the request.
     *
     * @param dbDetails List of database connection details.
     * @return true if all connections are successful, false otherwise.
     */
    public boolean connectToDatabase(MultiDatabaseConnectionRequest.DatabaseDetails dbDetails) {
        try {
            String url = buildJdbcUrl(dbDetails);

            DataSource dataSource = DataSourceBuilder.create()
                    .url(url)
                    .username(dbDetails.getUserName())
                    .password(dbDetails.getPassword())
                    .build();

            if (testConnection(dataSource)) {
                dataSourceMap.put(dbDetails.getDatabaseName(), dataSource);
                log.info("Successfully connected to {}", dbDetails.getDatabaseName());
                return true;
            } else {
                log.error("Failed to connect to {}", dbDetails.getDatabaseName());
                return false;
            }
        } catch (Exception e) {
            log.error("Error connecting to {}: {}", dbDetails.getDatabaseName(), e.getMessage());
            return false;
        }
    }

    /**
     * Disconnects from a specific database.
     *
     * @param databaseName The name of the database to disconnect from.
     * @return true if the disconnection was successful, false otherwise.
     */
    public boolean disconnectFromDatabase(String databaseName) {
        if (dataSourceMap.containsKey(databaseName)) {
            dataSourceMap.remove(databaseName);
            log.info("Disconnected from {}", databaseName);
            return true;
        } else {
            log.warn("No active connection found for {}", databaseName);
            return false;
        }
    }

    /**
     * Retrieves the DataSource for the specified database.
     *
     * @param databaseName The name of the database to retrieve the DataSource for.
     * @return The DataSource if found, null otherwise.
     */
    public DataSource getDataSource(String databaseName) {
        return dataSourceMap.get(databaseName);
    }

    /**
     * Builds the JDBC URL based on the provided database details.
     *
     * @param dbDetails The database connection details.
     * @return The formatted JDBC URL.
     */
    private String buildJdbcUrl(MultiDatabaseConnectionRequest.DatabaseDetails dbDetails) {
        return "jdbc:" + dbDetails.getDatabaseType() + "://" + dbDetails.getHost() + ":" + dbDetails.getPort() + "/" + dbDetails.getDatabaseName();
    }

    /**
     * Tests the connection to ensure that the database is reachable.
     *
     * @param dataSource The DataSource to test.
     * @return true if the connection is successful, false otherwise.
     */
    private boolean testConnection(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            if (!connection.isClosed()) {
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to establish a connection: {}", e.getMessage());
        }
        return false;
    }
}
