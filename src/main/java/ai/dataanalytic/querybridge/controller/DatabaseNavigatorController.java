package ai.dataanalytic.querybridge.controller;

import ai.dataanalytic.querybridge.dto.ConnectionEntity;
import ai.dataanalytic.querybridge.mongodb.repository.ConnectionRepository;
import ai.dataanalytic.querybridge.service.DatabaseService;
import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for handling database navigation requests.
 */
@Slf4j
@RestController
@RequestMapping("/query/bridge/database")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DatabaseNavigatorController {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ConnectionRepository connectionRepository;



    /**
     * Connects to the database using dynamic data sources.
     *
     * @param databaseConnectionRequest The database credentials provided in the request body.
     * @param session                   The HTTP session.
     * @return ResponseEntity with connection status.
     */
    @PostMapping("/connect")
    public ResponseEntity<String> setDatabaseConnection(
            @RequestBody DatabaseConnectionRequest databaseConnectionRequest,
            HttpSession session) {
        return databaseService.setDatabaseConnection(databaseConnectionRequest, session);
    }

    /**
     * Lists the tables in the database.
     *
     * @param session The HTTP session.
     * @return ResponseEntity with the list of tables.
     */
    @GetMapping("/listTables/{connectionId}")
    public ResponseEntity<List<String>> listTables(HttpSession session,@PathVariable("connectionId") String connectionId) {
        return databaseService.listTables(session, connectionId);
    }

    /**
     * Lists the columns of a table.
     *
     * @param tableName The name of the table.
     * @param session   The HTTP session.
     * @return ResponseEntity with the list of columns.
     */
    @GetMapping("/columns/{connectionId}/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> listColumns(
            @PathVariable("tableName") String tableName,
            @PathVariable("connectionId") String connectionId,
            HttpSession session
            ) {
        return databaseService.listColumns(tableName, session, connectionId);
    }

    /**
     * Gets the data of a table with pagination.
     *
     * @param tableName The name of the table.
     * @param page      The page number.
     * @param size      The number of rows per page.
     * @param session   The HTTP session.
     * @return ResponseEntity with the table data.
     */
    @GetMapping("/data/{connectionId}/{tableName}")
    public ResponseEntity<Map<String, Object>> getTableData(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @PathVariable("connectionId") String connectionId,
            HttpSession session
            ) {
        return databaseService.getTableData(tableName, page, size, session, connectionId);
    }

    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionEntity>> getUserConnections(HttpSession session) {
        String userId = String.valueOf(databaseService.getUserIdFromSession(session));
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        List<ConnectionEntity> connections = connectionRepository.findByUserId(userId);
        return ResponseEntity.ok(connections);
    }

    @GetMapping("/connection/{connectionId}")
    public ResponseEntity<ConnectionEntity> getConnectionDetails(@PathVariable("connectionId") String connectionId, HttpSession session) {
        String userId = databaseService.getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        ConnectionEntity connection = connectionRepository.findByUserIdAndConnectionId(userId, connectionId);
        if (connection == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(connection);
    }
}
