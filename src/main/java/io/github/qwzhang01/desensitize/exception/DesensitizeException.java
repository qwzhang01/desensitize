package io.github.qwzhang01.desensitize.exception;

/**
 * Base exception class for desensitization library.
 *
 * <p>This is the root exception for all desensitization-related errors.
 * All custom exceptions in this library should extend this class to maintain
 * a clear exception hierarchy.</p>
 *
 * <p><strong>Exception Hierarchy:</strong></p>
 * <pre>
 * RuntimeException
 *   └── DesensitizeException (base)
 *       ├── BeanCopyException (bean operation errors)
 *       ├── DataScopeErrorException (data scope errors)
 *       └── JacksonException (JSON serialization errors)
 * </pre>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * // Simple error message
 * throw new DesensitizeException("Encryption failed");
 *
 * // With cause
 * throw new DesensitizeException("Encryption failed", ioException);
 * </pre>
 *
 * @author avinzhang
 */
public class DesensitizeException extends RuntimeException {

    /**
     * Constructs a new desensitize exception with the specified detail message.
     * The cause is not initialized.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public DesensitizeException(String message) {
        super(message);
    }

    /**
     * Constructs a new desensitize exception with the specified cause.
     * The detail message is set to the message of the cause.
     *
     * @param cause the underlying cause of this exception
     */
    public DesensitizeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new desensitize exception with the specified detail message and cause.
     * This is the most commonly used constructor for proper error reporting.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying cause of this exception
     */
    public DesensitizeException(String message, Throwable cause) {
        super(message, cause);
    }
}