package space.cosmocats.marketplace.common.exception;

public abstract class NotFoundException extends ApplicationException {
    protected NotFoundException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}