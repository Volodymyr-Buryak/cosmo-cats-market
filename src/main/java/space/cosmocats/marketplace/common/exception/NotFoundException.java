package space.cosmocats.marketplace.common.exception;

public abstract class NotFoundException extends ApplicationException {
    protected NotFoundException(String messageKey, String logMessage, Object... args) {
        super(messageKey, logMessage, args);
    }
}
