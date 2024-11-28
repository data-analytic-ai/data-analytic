package ai.dataanalytic.sharedlibrary.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingUtils {
    public static void logInfo(String message) {
        log.info(message);
    }
    
    public static void logError(String message, Throwable t) {
        log.error(message, t);
    }
}
