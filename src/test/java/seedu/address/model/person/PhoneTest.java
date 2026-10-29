package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PhoneTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Phone(null));
    }

    @Test
    public void constructor_invalidPhone_throwsIllegalArgumentException() {
        String invalidPhone = "";
        assertThrows(IllegalArgumentException.class, () -> new Phone(invalidPhone));
    }

    @Test
    public void isValidPhone() {
        // null phone number
        assertThrows(NullPointerException.class, () -> Phone.isValidPhone(null));

        // invalid phone numbers
        assertFalse(Phone.isValidPhone("")); // empty string
        assertFalse(Phone.isValidPhone(" ")); // spaces only
        assertFalse(Phone.isValidPhone("91")); // less than 3 numbers
        assertFalse(Phone.isValidPhone("phone")); // non-numeric
        assertFalse(Phone.isValidPhone("9011p041")); // alphabets within digits
        assertFalse(Phone.isValidPhone("9312  1534")); // repeated separators

        // valid phone numbers
        assertTrue(Phone.isValidPhone("911")); // exactly 3 numbers
        assertTrue(Phone.isValidPhone("93121534"));
        assertTrue(Phone.isValidPhone("124293842033123")); // long phone numbers
        assertTrue(Phone.isValidPhone("+65 9123 4567"));
        assertTrue(Phone.isValidPhone("65-9123-4567"));
    }

    @Test
    public void isValidPhone_incompleteDigitGroups_rejectedForNumbersAndQueries() {
        for (String invalid : List.of("123-", "123 ", "123-+4", "123-/", "123-:", "123-a")) {
            assertFalse(Phone.isValidPhone(invalid), invalid);
            assertFalse(Phone.isValidPhoneQuery(invalid), invalid);
        }
    }

    @Test
    public void equals() {
        Phone phone = new Phone("999");

        // same values -> returns true
        assertTrue(phone.equals(new Phone("999")));

        // same object -> returns true
        assertTrue(phone.equals(phone));

        // null -> returns false
        assertFalse(phone.equals(null));

        // different types -> returns false
        assertFalse(phone.equals(5.0f));

        // different values -> returns false
        assertFalse(phone.equals(new Phone("995")));
    }

    @Test
    public void hasSameDigitsAs_requiresAllDigitsAndPreservesLeadingZeroes() {
        Phone phone = new Phone("00123456");
        assertTrue(phone.hasSameDigitsAs(new Phone("00123456")));
        assertFalse(phone.hasSameDigitsAs(new Phone("123456")));
        assertFalse(phone.hasSameDigitsAs(new Phone("000123456")));
        assertFalse(phone.hasSameDigitsAs(new Phone("0012345")));
        assertFalse(phone.hasSameDigitsAs(null));
        String longNumber = "0" + "1".repeat(1000);
        assertTrue(new Phone(longNumber).hasSameDigitsAs(new Phone(longNumber)));
    }

    @Test
    public void constructor_asciiDigits_preservesLeadingZeroesWithoutLengthLimit() {
        assertEquals("001", new Phone("001").value);
        assertTrue(Phone.isValidPhone("1".repeat(1000)));
        for (String invalid : new String[] {"12", "1+23", "١٢٣", "１２３", "+12", "123-", "-123"}) {
            assertFalse(Phone.isValidPhone(invalid));
        }
        assertEquals("+65 9123 4567", new Phone("+65 9123 4567").value);
    }

    @Test
    public void hasSameDigitsAs_differentFormatting_matches() {
        Phone formatted = new Phone("+65 9123 4567");
        assertTrue(formatted.hasSameDigitsAs(new Phone("6591234567")));
        assertTrue(formatted.hasSameDigitsAs(new Phone("65-9123-4567")));
        assertFalse(formatted.hasSameDigitsAs(new Phone("6591234568")));
        assertFalse(formatted.equals(new Phone("6591234567")));
    }

}
