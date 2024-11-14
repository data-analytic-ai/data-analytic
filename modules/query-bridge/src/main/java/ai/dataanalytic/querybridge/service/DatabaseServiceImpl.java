package ai.dataanalytic.querybridge.service;

import ai.dataanalytic.querybridge.config.DynamicDataSourceManager;
import ai.dataanalytic.querybridge.dto.ConnectionEntity;
import ai.dataanalytic.querybridge.dto.DynamicTableData;
import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import ai.dataanalytic.sharedlibrary.util.StringUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Implementation of the DatabaseService interface.
 * This class handles the business logic for database operations.
 */
@Slf4j
@Service
public class DatabaseServiceImpl implements DatabaseService {

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired
    private SchemaDiscoveryService schemaDiscoveryService;

    @Autowired
    private Environment environment;

    @Autowired
    private ConnectionRepository connectionRepository;

    private static final String SESSION_ATTRIBUTE_CONNECTION = "dbConnection";

    // Mapa para almacenar las conexiones por userId
    private final Map<String, Map<String, JdbcTemplate>> userConnections = new ConcurrentHashMap<>();




    @Override
    public ResponseEntity<String> setDatabaseConnection(DatabaseConnectionRequest databaseConnectionRequest, HttpSession session) {
        // Validate the provided credentials
        if (!validateCredentials(databaseConnectionRequest)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid credentials provided");
        }

        try {
            // Create and test the database connection
            JdbcTemplate jdbcTemplate = dynamicDataSourceManager.createAndTestConnection(databaseConnectionRequest);

            // Get the user ID from the session
            String userId = getUserIdFromSession(session);
            log.info("User ID: {}", userId);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            if (jdbcTemplate == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect to database");
            }

            // Get or create the user's connections map
            Map<String, JdbcTemplate> connections = userConnections.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

            // Store the JdbcTemplate with the provided connectionId
            String connectionId = databaseConnectionRequest.getConnectionId();
            if (connectionId == null || connectionId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Connection ID is required");
            }
            connections.put(connectionId, jdbcTemplate);

            // Store the connections map in the session
            session.setAttribute("dbConnections", connections);

            // Guardar los detalles de la conexión en MongoDB
            ConnectionEntity connectionEntity = new ConnectionEntity();
            connectionEntity.setUserId(userId);
            connectionEntity.setConnectionId(connectionId);
            connectionEntity.setDatabaseType(databaseConnectionRequest.getDatabaseType());
            connectionEntity.setHost(databaseConnectionRequest.getHost());
            connectionEntity.setPort(databaseConnectionRequest.getPort());
            connectionEntity.setDatabaseName(databaseConnectionRequest.getDatabaseName());
            connectionEntity.setUserName(databaseConnectionRequest.getUserName());
            connectionEntity.setPassword(databaseConnectionRequest.getPassword()); // Considera cifrar
            connectionEntity.setSid(databaseConnectionRequest.getSid());
            connectionEntity.setInstance(databaseConnectionRequest.getInstance());

            connectionRepository.save(connectionEntity);

            return ResponseEntity.ok("Connected successfully to database: " + databaseConnectionRequest.getDatabaseName());
        } catch (Exception e) {
            log.error("Error connecting to the database", e);
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<List<String>> listTables(HttpSession session, String connectionId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateFromSession(session, connectionId);

        if (jdbcTemplate == null) {
            return handleMissingCredentialsForList();
        }

        try {
            // Get the list of tables in the database
            List<String> tables = schemaDiscoveryService.listTables(jdbcTemplate);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            log.error("Error listing tables", e);
            return handleListingTablesExceptionAsListString(e);
        }
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> listColumns(String tableName, HttpSession session, String connectionId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateFromSession(session, connectionId);

        if (jdbcTemplate == null) {
            return handleMissingCredentialsForListMap();
        }

        try {
            // Validate table name
            if (!isValidIdentifier(tableName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // Get the list of columns in the specified table
            List<Map<String, Object>> columns = schemaDiscoveryService.listColumns(tableName, jdbcTemplate);
            return ResponseEntity.ok(columns);
        } catch (SQLException e) {
            return handleExceptionAsListMap(e, "SQL error listing columns for table: " + tableName);
        } catch (Exception e) {
            log.error("Error listing columns for table: {}", tableName, e);
            return handleExceptionAsListMap(e, "Error listing columns for table: " + tableName);
        }
    }


    @Override
    public ResponseEntity<Map<String, Object>> getTableData(String tableName, int page, int size, HttpSession session, String connectionId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateFromSession(session, connectionId);

        if (jdbcTemplate == null) {
            return handleMissingCredentialsForMap();
        }

        try {
            // Validate table name
            if (!isValidIdentifier(tableName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // Retrieve table data with pagination
            DynamicTableData tableData = schemaDiscoveryService.getTableDataWithPagination(tableName, jdbcTemplate, page, size).getBody();

            if (tableData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            // Create a response map that includes the data and pagination information
            Map<String, Object> response = new HashMap<>();
            response.put("rows", tableData.getRows());
            response.put("columns", tableData.getColumns());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalRows", tableData.getTotalRows());
            response.put("tableName", tableName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error obtaining data from table: {}", tableName, e);
            return handleExceptionAsMap(e);
        }
    }


    @Override
    public ResponseEntity<List<Map<String, Object>>> executeQuery(String query, HttpSession session , String connectionId) {
        JdbcTemplate jdbcTemplate = getJdbcTemplateFromSession(session, connectionId);

        if (jdbcTemplate == null) {
            return handleMissingCredentialsForListMap();
        }

        if (!isValidQuery(query)) {
            log.error("Invalid SQL query: {}", query);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        try {
            // Execute the query
            List<Map<String, Object>> result = jdbcTemplate.query(query, new ColumnMapRowMapper());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return handleExceptionAsListMap(e, "Error executing query: " + query);
        }
    }


    // Helper method to get JdbcTemplate from session
    public JdbcTemplate getJdbcTemplateFromSession(HttpSession session, String connectionId) {
        String userId = getUserIdFromSession(session);
        if (userId == null) {
            return null;
        }
        Map<String, JdbcTemplate> connections = userConnections.get(userId);
        if (connections == null) {
            connections = new ConcurrentHashMap<>();
            userConnections.put(userId, connections);
        }
        JdbcTemplate jdbcTemplate = connections.get(connectionId);
        if (jdbcTemplate == null) {
            // Intentar recuperar la conexión desde MongoDB
            ConnectionEntity connectionEntity = connectionRepository.findByUserIdAndConnectionId(userId, connectionId);
            if (connectionEntity != null) {
                // Reconstruir DatabaseConnectionRequest
                DatabaseConnectionRequest dbRequest = new DatabaseConnectionRequest();
                dbRequest.setDatabaseType(connectionEntity.getDatabaseType());
                dbRequest.setHost(connectionEntity.getHost());
                dbRequest.setPort(connectionEntity.getPort());
                dbRequest.setDatabaseName(connectionEntity.getDatabaseName());
                dbRequest.setUserName(connectionEntity.getUserName());
                dbRequest.setPassword(connectionEntity.getPassword()); //TODO:  cifrar/descifrar the password
                dbRequest.setSid(connectionEntity.getSid());
                dbRequest.setInstance(connectionEntity.getInstance());
                dbRequest.setConnectionId(connectionId);

                // Crear y probar la conexión
                jdbcTemplate = dynamicDataSourceManager.createAndTestConnection(dbRequest);
                if (jdbcTemplate != null) {
                    connections.put(connectionId, jdbcTemplate);
                }
            }
        }
        return jdbcTemplate;
    }


    public String getUserIdFromSession(HttpSession session) {
        return (String) session.getAttribute("userId");
    }

    /**
     * Validates the provided database credentials.
     *
     * @param databaseConnectionRequest The database credentials.
     * @return True if the credentials are valid, false otherwise.
     */
    private boolean validateCredentials(DatabaseConnectionRequest databaseConnectionRequest) {
        return StringUtils.allFieldsPresent(
                databaseConnectionRequest.getDatabaseName(),
                databaseConnectionRequest.getHost(),
                databaseConnectionRequest.getUserName(),
                databaseConnectionRequest.getPassword()
        );
    }

    /**
     * Validates identifiers like table names to prevent SQL injection.
     *
     * @param identifier The identifier to validate.
     * @return True if the identifier is valid, false otherwise.
     */
    private boolean isValidIdentifier(String identifier) {
        return identifier != null && identifier.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean isValidQuery(String query) {
        String sqlPattern = "^[a-zA-Z0-9_\\s,=*'();]*$";
        return Pattern.matches(sqlPattern, query);
    }

    private <T> ResponseEntity<T> handleException(Exception e) {
        log.error("Error connecting to the database.", e);
        if ("prod".equals(environment.getProperty("spring.profiles.active"))) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private ResponseEntity<List<Map<String, Object>>> handleExceptionAsListMap(Exception e, String message) {
        log.error(message, e);
        if ("prod".equals(environment.getProperty("spring.profiles.active"))) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        } else {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", message + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonList(errorMap));
        }
    }

    private ResponseEntity<Map<String, Object>> handleExceptionAsMap(Exception e) {
        log.error("Error processing request", e);
        if ("prod".equals(environment.getProperty("spring.profiles.active"))) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        } else {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Error processing request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }

    private ResponseEntity<List<String>> handleListingTablesExceptionAsListString(Exception e) {
        log.error("Error listing tables", e);
        if ("prod".equals(environment.getProperty("spring.profiles.active"))) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        } else {
            List<String> errorList = Collections.singletonList("Error listing tables: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorList);
        }
    }

    private ResponseEntity<List<String>> handleMissingCredentialsForList() {
        log.error("Credentials must be set before calling this method.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
    }

    private ResponseEntity<Map<String, Object>> handleMissingCredentialsForMap() {
        log.error("Credentials must be set before calling this method.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
    }

    private ResponseEntity<List<Map<String, Object>>> handleMissingCredentialsForListMap() {
        log.error("Credentials must be set before calling this method.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
    }
}
