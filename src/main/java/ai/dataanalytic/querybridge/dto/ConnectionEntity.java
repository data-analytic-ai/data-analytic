package ai.dataanalytic.querybridge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user_connections")
public class ConnectionEntity {
    @Id
    private String id; // Puede ser el connectionId
    private String userId;
    private String connectionId;
    private String databaseType;
    private String host;
    private int port;
    private String databaseName;
    private String userName;
    private String password; // TODO: cifrar password
    private String sid;
    private String instance;
}
