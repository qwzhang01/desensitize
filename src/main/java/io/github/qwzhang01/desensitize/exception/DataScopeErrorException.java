package io.github.qwzhang01.desensitize.exception;

/**
 * Bean copying exception.
 * This exception is thrown when errors occur during bean copying operations.
 *
 * @author avinzhang
 */
public class DataScopeErrorException extends DesensitizeException {
    public DataScopeErrorException(String message) {
        super(message);
    }
}