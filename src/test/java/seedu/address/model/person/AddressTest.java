package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class AddressTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Address(null));
    }

    @Test
    public void constructor_invalidAddress_throwsIllegalArgumentException() {
        String invalidAddress = "";
        assertThrows(IllegalArgumentException.class, () -> new Address(invalidAddress));
    }

    @Test
    public void isValidAddress() {
        // null address
        assertThrows(NullPointerException.class, () -> Address.isValidAddress(null));

        // invalid addresses
        assertFalse(Address.isValidAddress("")); // empty string
        assertFalse(Address.isValidAddress(" ")); // spaces only

        // valid addresses
        assertTrue(Address.isValidAddress("Blk 456, Den Road, #01-355"));
        assertTrue(Address.isValidAddress("-")); // one character
        assertTrue(Address.isValidAddress("Leng Inc; 1234 Market St; San Francisco CA 2349879; USA")); // long address
    }

    @Test
    public void equals() {
        Address address = new Address("Valid Address");

        // same values -> returns true
        assertTrue(address.equals(new Address("Valid Address")));

        // same object -> returns true
        assertTrue(address.equals(address));

        // null -> returns false
        assertFalse(address.equals(null));

        // different types -> returns false
        assertFalse(address.equals(5.0f));

        // different values -> returns false
        assertFalse(address.equals(new Address("Other Valid Address")));
    }

    @Test
    public void constructor_singleLineUnicode_trimsOrdinarySpacesAndPreservesText() {
        assertEquals("新加坡 🏠 #02-01", new Address("  新加坡 🏠 #02-01  ").value);
        for (String invalid : new String[] {"A\nB", "A\rB", "A\tB", "A\u0085B", "A\u2028B", "A\u2029B"}) {
            assertFalse(Address.isValidAddress(invalid));
            assertThrows(IllegalArgumentException.class, () -> new Address(invalid));
        }
    }

    @Test
    public void isValidAddress_onlyUnicodeSpacesOrFormatting_rejectsBlankText() {
        for (String invisible : new String[] {"\u00a0", "\u3000", "\u200b", "\u2007", "\u202f",
            "\ufeff", "\u200c\u200d", " \u00a0\u3000\u200b "}) {
            assertFalse(Address.isValidAddress(invisible));
            assertThrows(IllegalArgumentException.class, () -> new Address(invisible));
            String visible = "Blk" + invisible + "1";
            assertTrue(Address.isValidAddress(visible));
            assertEquals(visible, new Address(visible).value);
        }
    }

    @Test
    public void isValidAddress_unpairedSurrogates_rejectsMalformedText() {
        for (String invalid : new String[] {"\uD800", "\uDC00", "\uDC00\uD800", "\uD800A",
            "\uD800\uD800", "A\uDC00B", "\uD83C\uDFE0\uD800"}) {
            assertFalse(Address.isValidAddress(invalid));
            assertThrows(IllegalArgumentException.class, () -> new Address(invalid));
        }
    }

    @Test
    public void constructor_validUnicode_preservesSupplementaryAndReplacementCharacters() {
        for (String valid : new String[] {"\uD800\uDC00", "\uDBFF\uDFFF", "新加坡 🏠 #02-01", "Blk \uFFFD"}) {
            assertTrue(Address.isValidAddress(valid));
            assertEquals(valid, new Address("  " + valid + "  ").value);
        }
    }

}
