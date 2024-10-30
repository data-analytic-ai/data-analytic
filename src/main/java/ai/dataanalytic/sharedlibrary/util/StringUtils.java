package ai.dataanalytic.sharedlibrary.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtils {

    private StringUtils() {}

    public static boolean allFieldsPresent(String... fields) {
        for (String field : fields) {
            if (field == null || field.isEmpty()) {
                log.warn("Field is either null or empty.");
                return false;
            }
        }
        log.info("All fields are present and non-empty.");
        return true;
    }
}


