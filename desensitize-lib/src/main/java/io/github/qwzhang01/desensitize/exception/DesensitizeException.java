package io.github.qwzhang01.desensitize.exception;

/**
 * 总异常
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