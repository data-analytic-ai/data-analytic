package ai.dataanalytic.querybridge.controller;

import ai.dataanalytic.querybridge.service.DatabaseConnectionService;
import ai.dataanalytic.sharedlibrary.dto.MultiDatabaseConnectionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/v1/query/bridge/multi/database")
public class DatabaseConnectionController {

    @Autowired
    private DatabaseConnectionService databaseConnectionService;

    @PostMapping("/connect")
    public ResponseEntity<String> connectToDatabases(@RequestBody MultiDatabaseConnectionRequest connectionRequest) {
        return databaseConnectionService.setMultipleConnections(connectionRequest);
    }
}
