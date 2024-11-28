package ai.dataanalytic.sharedlibrary.error;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;
    private int statusCode;

    public ErrorResponse(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}
