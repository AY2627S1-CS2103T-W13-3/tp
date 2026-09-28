package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void constructor_freeText_preservesValue() {
        assertEquals("", new Remark("").value);
        assertEquals("  Likes coffee!  ", new Remark("  Likes coffee!  ").value);
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Likes coffee");
        assertEquals(remark, remark);
        assertEquals(remark, new Remark("Likes coffee"));
        assertEquals(remark.hashCode(), new Remark("Likes coffee").hashCode());
        assertFalse(remark.equals(null));
        assertFalse(remark.equals("Likes coffee"));
        assertFalse(remark.equals(new Remark("")));
    }
}
