package seedu.address.model.person;

import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * A permanent positive client identifier, independent of contact values and displayed row positions.
 */
public final class ClientId {
    private final int value;

    /**
     * Constructs an identifier in the supported range {@code 1..Integer.MAX_VALUE}.
     */
    public ClientId(int value) {
        checkArgument(value > 0, "Client ID must be a positive integer from 1 to 2147483647.");
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ClientId otherId && value == otherId.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }
}
