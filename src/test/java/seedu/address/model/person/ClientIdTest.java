package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests permanent identity value boundaries and use as a lookup key. */
public class ClientIdTest {
    @Test
    public void constructor_nonPositiveValue_rejects() {
        for (int value : List.of(Integer.MIN_VALUE, -1, 0)) {
            assertThrows(IllegalArgumentException.class, () -> new ClientId(value));
        }
    }

    @Test
    public void constructor_supportedBoundaries_preservesDecimalValue() {
        ClientId first = new ClientId(1);
        ClientId last = new ClientId(Integer.MAX_VALUE);
        assertEquals(1, first.getValue());
        assertEquals("1", first.toString());
        assertEquals(Integer.MAX_VALUE, last.getValue());
        assertEquals("2147483647", last.toString());
    }

    @Test
    public void equals_sameValue_hasSameLookupKey() {
        HashSet<ClientId> keys = new HashSet<>();
        keys.add(new ClientId(17));
        keys.add(new ClientId(17));
        keys.add(new ClientId(41));
        assertEquals(2, keys.size());
        assertFalse(new ClientId(17).equals(null));
        assertFalse(new ClientId(17).equals("17"));
    }
}
