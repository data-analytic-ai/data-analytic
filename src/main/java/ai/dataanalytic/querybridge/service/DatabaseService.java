package ai.dataanalytic.querybridge.service;


import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Interface for database-related operations.
 */
@Service
public interface DatabaseService {
    ResponseEntity<String> disconnectDatabase(String userId, String connectionId);
    ResponseEntity<String> setDatabaseConnection(DatabaseConnectionRequest databaseConnectionRequest, HttpSession session);
    ResponseEntity<List<String>> listTables(HttpSession session, String connectionId);
    ResponseEntity<List<Map<String, Object>>> listColumns(String tableName, HttpSession session, String connectionId);
    ResponseEntity<Map<String, Object>> getTableData(String tableName, int page, int size, HttpSession session, String connectionId);
    ResponseEntity<List<Map<String, Object>>> executeQuery(String query, HttpSession session, String connectionId);
    JdbcTemplate getJdbcTemplateFromSession(HttpSession session, String connectionId);
    String getUserIdFromSession(HttpSession session);
}

