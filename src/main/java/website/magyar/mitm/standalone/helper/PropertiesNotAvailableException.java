package website.magyar.mitm.standalone.helper;

/**
 * Thrown when the configuration properties cannot be loaded.
 * @author Tamas Kohegyi
 *
 */
public class PropertiesNotAvailableException extends RuntimeException {

    /**
     * Constructor with a cause.
     * @param message the message of the exception
     * @param throwable the cause of the exception
     */
    public PropertiesNotAvailableException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructor with a message only.
     * @param message the message of the exception
     */
    public PropertiesNotAvailableException(final String message) {
        super(message);
    }

}
