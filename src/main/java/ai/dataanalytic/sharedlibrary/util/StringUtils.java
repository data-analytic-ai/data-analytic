package ai.dataanalytic.sharedlibrary.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtils {

    private StringUtils() {}

    public static boolean allFieldsPresent(String... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null || fields[i].isEmpty()) {
                log.warn("Validation failed: field[{}] is null or empty", i);
                return false;
            }
        }
        log.debug("All {} fields validated successfully", fields.length);
        return true;
    }
}


