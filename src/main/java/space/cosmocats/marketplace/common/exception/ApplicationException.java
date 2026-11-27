package space.cosmocats.marketplace.common.exception;

import lombok.Getter;
import java.util.Objects;

@Getter
public abstract class ApplicationException extends RuntimeException {
    private static final String LOG_MESSAGE_REQUIRED = "Log message must not be null";
    private static final String MESSAGE_KEY_REQUIRED = "Message key must not be null";
    private static final String MESSAGE_ARGUMENTS_REQUIRED = "Message arguments must not be null";

    private final Object[] args;
    private final String messageKey;

    protected ApplicationException(String messageKey, String logMessage, Object... args) {
        super(Objects.requireNonNull(logMessage, LOG_MESSAGE_REQUIRED));
        this.messageKey = Objects.requireNonNull(messageKey, MESSAGE_KEY_REQUIRED);
        this.args = Objects.requireNonNull(args, MESSAGE_ARGUMENTS_REQUIRED).clone();
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
