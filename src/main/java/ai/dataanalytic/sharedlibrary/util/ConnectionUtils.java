package ai.dataanalytic.sharedlibrary.util;

public class ConnectionUtils {
    public static boolean validateConnectionDetails(String host, int port, String databaseName) {
        return host != null && !host.isEmpty() && port > 0 && databaseName != null && !databaseName.isEmpty();
    }
}
