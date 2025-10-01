package io.github.qwzhang01.desensitize.exception;

/**
 * Base exception for desensitization operations.
 * This is the root exception class for all desensitization-related errors.
 *
 * @author avinzhang
 */
public class DesensitizeException extends RuntimeException {

    public DesensitizeException(String message) {
        super(message);
    }

    public DesensitizeException(Throwable cause) {
        super(cause);
    }

    public DesensitizeException(String message, Throwable cause) {
        super(message, cause);
    }
}