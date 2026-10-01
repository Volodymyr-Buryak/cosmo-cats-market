package space.cosmocats.marketplace.common.exception;

public abstract class BusinessRuleException extends ApplicationException {
    protected BusinessRuleException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
