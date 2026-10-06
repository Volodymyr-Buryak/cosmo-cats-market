package space.cosmocats.marketplace.common.exception;

public abstract class DomainRuleViolationException extends ApplicationException {
    protected DomainRuleViolationException(String messageKey, String logMessage, Object... args) {
        super(messageKey, logMessage, args);
    }
}
