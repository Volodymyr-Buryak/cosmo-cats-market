package space.cosmocats.marketplace.common.exception;

public abstract class BusinessRuleException extends ApplicationException {
    protected BusinessRuleException(String messageKey, String logMessage, Object... args) {
        super(messageKey, logMessage, args);
    }
}
