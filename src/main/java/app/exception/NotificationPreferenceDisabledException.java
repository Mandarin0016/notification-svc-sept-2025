package app.exception;

public class NotificationPreferenceDisabledException extends RuntimeException {

    public NotificationPreferenceDisabledException() {
    }

    public NotificationPreferenceDisabledException(String message) {
        super(message);
    }
}
