package ai.dataanalytic.databridge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataTransferRequest {
    private String sourceConnectionId;
    private String destinationConnectionId;
    private String tableName;
}
