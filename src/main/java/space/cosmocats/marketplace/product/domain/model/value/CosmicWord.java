package space.cosmocats.marketplace.product.domain.model.value;

import java.util.Set;
import java.util.Locale;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum CosmicWord {
    STAR,
    GALAXY,
    COMET;

    private static final Pattern SEPARATOR = Pattern.compile("[^A-Z]+");
    private static final Set<String> NAMES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    public static boolean occursIn(String text) {
        if (text == null) return false;
        return SEPARATOR.splitAsStream(text.toUpperCase(Locale.ROOT))
                .anyMatch(NAMES::contains);
    }
}
