package io.github.qwzhang01.desensitize.exception;

/**
 * Bean copying exception.
 * This exception is thrown when errors occur during bean copying operations.
 *
 * @author avinzhang
 */
public class BeanCopyException extends DesensitizeException {
    public BeanCopyException(String message, Throwable cause) {
        super(message, cause);
    }
}