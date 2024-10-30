package ai.dataanalytic.databridge.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DataSourceValidator {

    @Autowired
    @Qualifier("dataSource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("sourceDataSource")
    private DataSource sourceDataSource;

    @Autowired
    @Qualifier("destinationDataSource")
    private DataSource destinationDataSource;

    @PostConstruct
    public void validateDataSources() {
        validateDataSource(dataSource, "dataSource");
        validateDataSource(sourceDataSource, "sourceDataSource");
        validateDataSource(destinationDataSource, "destinationDataSource");
    }

    private void validateDataSource(DataSource dataSource, String name) {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println(name + " connection is valid: " + !conn.isClosed());
        } catch (SQLException e) {
            System.err.println("Error connecting to " + name + ": " + e.getMessage());
        }
    }
}
