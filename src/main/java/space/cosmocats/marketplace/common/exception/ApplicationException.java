package space.cosmocats.marketplace.common.exception;

import lombok.Getter;
import java.util.Objects;

@Getter
public abstract class ApplicationException extends RuntimeException {
    private final Object[] args;
    private final String messageKey;

    protected ApplicationException(String messageKey, Object... args) {
        super(Objects.requireNonNull(messageKey, "Message key must not be null"));
        this.args = args.clone();
        this.messageKey = messageKey;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
