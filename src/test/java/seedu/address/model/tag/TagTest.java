package seedu.address.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        String invalidTagName = "";
        assertThrows(IllegalArgumentException.class, () -> new Tag(invalidTagName));
    }

    @Test
    public void isValidTagName() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));
    }

    @Test
    public void constructor_caseVariants_normalizesWithRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("insurance", new Tag("INSURANCE").tagName);
            assertEquals(new Tag("health"), new Tag("HEALTH"));
            assertEquals(new Tag("health").hashCode(), new Tag("HEALTH").hashCode());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void isValidTagName_alphanumericSegmentsWithSingleInternalSeparators() {
        for (String valid : new String[] {"HEALTH", "2026", "health2", "life-insurance", "high_priority", "A-2_b"}) {
            assertTrue(Tag.isValidTagName(valid));
        }
        for (String invalid : new String[] {"", " ", "health care", "-a", "a-", "_", "a--b", "a__b", "a-_b", "a_-b",
            "健康", "é", "a\t"}) {
            assertFalse(Tag.isValidTagName(invalid));
        }
    }

    @Test
    public void isValidTagName_longSegmentedTags_remainsBounded() {
        String valid = "a-b_".repeat(5000) + "z";
        assertTrue(Tag.isValidTagName(valid));
        assertFalse(Tag.isValidTagName(valid + "-"));
        assertFalse(Tag.isValidTagName(valid + "!"));
    }

    @Test
    public void constructor_segmentedCaseVariants_normalizesWithRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("life-insurance", new Tag("LIFE-INSURANCE").tagName);
            assertEquals(new Tag("high_priority"), new Tag("HIGH_PRIORITY"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void formatTags_sortsNormalizedValuesAndHandlesEmptySet() {
        assertEquals("[2026] [active] [health]",
                Tag.formatTags(Set.of(new Tag("HEALTH"), new Tag("active"), new Tag("2026"))));
        assertEquals("None", Tag.formatTags(Set.of()));
        assertThrows(NullPointerException.class, () -> Tag.formatTags(null));
    }

    @Test
    public void formatTags_prefixValues_sortsValuesBeforeAddingBrackets() {
        assertEquals("[1] [10] [a] [a0] [aa]", Tag.formatTags(Set.of(
                new Tag("aa"), new Tag("a0"), new Tag("a"), new Tag("10"), new Tag("1"))));
    }

}
