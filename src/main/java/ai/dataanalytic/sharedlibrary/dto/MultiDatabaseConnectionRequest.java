package ai.dataanalytic.sharedlibrary.dto;

import lombok.Data;

import java.util.List;

@Data
public class MultiDatabaseConnectionRequest {
    private List<DatabaseDetails> databases;

    @Data
    public static class DatabaseDetails {
        private String databaseType;  // e.g., postgresql, mysql
        private String host;          // e.g., localhost
        private int port;             // e.g., 5432
        private String databaseName;  // e.g., for-devs-university
        private String userName;      // e.g., postgres
        private String password;      // e.g., toor
        private boolean isSource;     // true for source, false for destination
    }
}
