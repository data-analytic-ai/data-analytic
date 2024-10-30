package ai.dataanalytic.databridge.service;

import ai.dataanalytic.databridge.dto.DatabaseConnectionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class QueryBridgeDatabaseClient {

    private final RestTemplate restTemplate;

    @Value("${query.bridge.database.url}")
    private String queryBridgeUrl;

    public QueryBridgeDatabaseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void connectToDatabase(DatabaseConnectionRequest request) {
        log.info("Connecting to database: {} on host: {}", request.getDatabaseName(), request.getHost());
        // Append /connect to the base URL defined in application.properties
        restTemplate.postForEntity(queryBridgeUrl + "/connect", request, Void.class);
        log.info("Database connection established successfully");
    }
}
