package ai.dataanalytic.sharedlibrary.dto;

import lombok.Data;

@Data
public class DatabaseConnectionRequest {
    private String databaseType;  // e.g., postgresql, mysql
    private String host;          // e.g., localhost
    private int port;             // e.g., 5432
    private String databaseName;  // e.g., for-devs-university
    private String userName;      // e.g., postgres
    private String password;      // e.g., toor
    private String sid;          // e.g., ORCL
    private String instance;    // e.g., SQL-Server
}
