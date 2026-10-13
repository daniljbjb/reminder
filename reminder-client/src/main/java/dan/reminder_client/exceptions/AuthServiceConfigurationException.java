package dan.reminder_client.exceptions;

/**
 * Indicates that reminder-client cannot authenticate itself in auth-service
 * or that the exchange endpoint is configured incorrectly.
 */
public class AuthServiceConfigurationException extends RuntimeException {

    public AuthServiceConfigurationException(String message) {
        super(message);
    }
}
