package space.cosmocats.marketplace.common.exception;

import lombok.Getter;
import java.util.Objects;

@Getter
public abstract class ApplicationException extends RuntimeException {
    private final Object[] args;
    private final String messageKey;

    protected ApplicationException(String messageKey, String logMessage, Object... args) {
        super(Objects.requireNonNull(logMessage, "Log message must not be null"));
        this.messageKey = Objects.requireNonNull(messageKey, "Message key must not be null");
        this.args = Objects.requireNonNull(args, "Message arguments must not be null").clone();
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
