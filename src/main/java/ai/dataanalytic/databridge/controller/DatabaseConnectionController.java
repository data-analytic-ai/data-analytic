package ai.dataanalytic.databridge.controller;

import ai.dataanalytic.databridge.service.QueryBridgeDatabaseClient;
import ai.dataanalytic.databridge.dto.DatabaseConnectionRequest;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("data/bridge/database")
public class DatabaseConnectionController {

    private final QueryBridgeDatabaseClient queryBridgeDatabaseClient;

    public DatabaseConnectionController(QueryBridgeDatabaseClient queryBridgeDatabaseClient) {
        this.queryBridgeDatabaseClient = queryBridgeDatabaseClient;
    }

    @PostMapping("/connect")  // Path for database connection
    public String connectToDatabase(@RequestBody DatabaseConnectionRequest request) {
        log.info("Received request to connect to database: {}", request.getDatabaseName());
        queryBridgeDatabaseClient.connectToDatabase(request);
        return "Connection Successful!";
    }


}
