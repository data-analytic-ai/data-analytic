package ai.dataanalytic.querybridge.service;

import ai.dataanalytic.sharedlibrary.dto.MultiDatabaseConnectionRequest;
import ai.dataanalytic.querybridge.datasource.MultiDatabaseConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DatabaseConnectionServiceImpl implements DatabaseConnectionService {

    @Autowired
    private MultiDatabaseConnectionManager multiDatabaseConnectionManager;

    @Override
    public ResponseEntity<String> setMultipleConnections(MultiDatabaseConnectionRequest request) {
        for (MultiDatabaseConnectionRequest.DatabaseDetails dbDetails : request.getDatabases()) {
            if (!multiDatabaseConnectionManager.connectToDatabase(dbDetails)) {
                return new ResponseEntity<>("Failed to connect to: " + dbDetails.getDatabaseName(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("All databases connected successfully", HttpStatus.OK);
    }
}
