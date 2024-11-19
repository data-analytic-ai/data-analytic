package ai.dataanalytic.querybridge.service;

import ai.dataanalytic.querybridge.config.DynamicDataSourceManager;
import ai.dataanalytic.querybridge.dto.DynamicTableData;
import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SchemaDiscoveryService {

    /**
     * Obtiene la lista de tablas de la base de datos y la devuelve como una lista de cadenas de texto
     * con el nombre de las tablas encontradas en la base de datos.
     */
    public List<String> listTables(JdbcTemplate jdbcTemplate) throws SQLException {
        return jdbcTemplate.execute((Connection con) -> {
            List<String> tableList = new ArrayList<>();
            DatabaseMetaData metaData = con.getMetaData();
            String[] types = {"TABLE"};
            try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
                while (rs.next()) {
                    tableList.add(rs.getString("TABLE_NAME"));
                }
            }
            return tableList;
        });
    }

    /**
     * Obtiene la lista de columnas de una tabla en la base de datos y la devuelve como una lista de mapas
     * con el nombre de la columna, el tipo de dato y el tamaño de la columna.
     */
    public List<Map<String, Object>> listColumns(String tableName, JdbcTemplate jdbcTemplate) throws SQLException {
        return jdbcTemplate.execute((Connection con) -> {
            List<Map<String, Object>> columnList = new ArrayList<>();
            DatabaseMetaData metaData = con.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
                while (rs.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
                    column.put("TYPE_NAME", rs.getString("TYPE_NAME"));
                    column.put("COLUMN_SIZE", rs.getInt("COLUMN_SIZE"));
                    columnList.add(column);
                }
            }
            return columnList;
        });
    }

    /**
     * Obtiene los datos de una tabla con paginación y el conteo total de filas.
     */
    public ResponseEntity<DynamicTableData> getTableDataWithPagination(String tableName, JdbcTemplate jdbcTemplate, int page, int size) {
        try {
            // Validar y sanitizar el nombre de la tabla
            if (!isValidIdentifier(tableName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // Obtener el total de filas
            String countSql = "SELECT COUNT(*) FROM " + tableName;
            int totalRows = jdbcTemplate.queryForObject(countSql, Integer.class);

            // Obtener las filas con paginación
            String dataSql = "SELECT * FROM " + tableName + " LIMIT ? OFFSET ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql, size, page * size);

            // Obtener las columnas
            List<Map<String, Object>> columns = listColumns(tableName, jdbcTemplate);

            DynamicTableData response = new DynamicTableData();
            response.setRows(rows);
            response.setColumns(columns);
            response.setTotalRows(totalRows);
            response.setTableName(tableName);
            response.setPageSize(size);
            response.setCurrentPage(page);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error obtaining data from table: " + tableName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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
}