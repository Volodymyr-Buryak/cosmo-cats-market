package space.cosmocats.marketplace.common.exception;

public abstract class ConflictException extends ApplicationException {
    protected ConflictException(String messageKey, String logMessage, Object... args) {
        super(messageKey, logMessage, args);
    }
}
