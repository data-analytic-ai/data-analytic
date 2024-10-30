package ai.dataanalytic.databridge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    private String tableName;
    private List<String> columns;
    private Map<String, String> filters;
}
