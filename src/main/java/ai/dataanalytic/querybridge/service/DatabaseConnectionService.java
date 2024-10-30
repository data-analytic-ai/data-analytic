package ai.dataanalytic.querybridge.service;

import ai.dataanalytic.sharedlibrary.dto.DatabaseConnectionRequest;
import ai.dataanalytic.sharedlibrary.dto.MultiDatabaseConnectionRequest;
import org.springframework.http.ResponseEntity;

public interface DatabaseConnectionService {
    ResponseEntity<String> setMultipleConnections(MultiDatabaseConnectionRequest connectionRequest);
}
