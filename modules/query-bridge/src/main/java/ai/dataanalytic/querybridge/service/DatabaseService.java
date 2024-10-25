package ai.dataanalytic.querybridge.service;


import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Interface for database-related operations.
 */
@Service
public interface DatabaseService {
    ResponseEntity<String> setDatabaseConnection(DatabaseConnectionRequest databaseConnectionRequest, HttpSession session);
    ResponseEntity<List<String>> listTables(HttpSession session);
    ResponseEntity<List<Map<String, Object>>> listColumns(String tableName, HttpSession session);
    ResponseEntity<Map<String, Object>> getTableData(String tableName, int page, int size, HttpSession session);
    ResponseEntity<List<Map<String, Object>>> executeQuery(String query, HttpSession session);
}

