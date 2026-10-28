package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class FollowUpTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    @Test
    public void constructor_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FollowUp(null, "Call client"));
        assertThrows(NullPointerException.class, () -> new FollowUp(TODAY, null));
    }

    @Test
    public void constructor_outerSpaces_normalizesDescription() {
        FollowUp followUp = new FollowUp(TODAY, "  Call  Rachel - quotation #2!  ");
        assertEquals("Call  Rachel - quotation #2!", followUp.getDescription());
        assertEquals(TODAY, followUp.getDueDate());
    }

    @Test
    public void constructor_validDescriptionBoundaries_acceptsDescription() {
        assertEquals("!", new FollowUp(TODAY, "!").getDescription());
        assertEquals("~".repeat(200), new FollowUp(TODAY, "  " + "~".repeat(200) + "  ").getDescription());
    }

    @Test
    public void constructor_invalidDescription_throwsIllegalArgumentException() {
        String[] invalidDescriptions = {"", "   ", "x".repeat(201), "\tCall", "Call\n", "Call\r",
            "Call\u0000", "Call\u007f", "Call\u0085", "Call\u2028", "Call\u2029", "Call\tclient",
            "\ud800", "\udc00", "Call\ud800end", "\udc00\ud800", "\ud800\ud800\udc00",
            "Call\u0378", "Call\uffff"};
        for (String description : invalidDescriptions) {
            assertThrows(IllegalArgumentException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, () ->
                    new FollowUp(TODAY, description));
        }
    }

    @Test
    public void constructor_multilingualTextAndJoiningSequences_preservesExactDescription() {
        String[] descriptions = {"续保电话", "வாடிக்கையாளரை அழைக்கவும்", "اتصال بشأن التجديد", "Café",
            "Cafe\u0301", "Call 😀", "👩\u200d💻", "மீண்டும்  அழைக்கவும்", "می\u200cروم",
            "\u00a0Call\u3000", "续保，Call — renewal!", "\ufffd"};
        for (String description : descriptions) {
            assertEquals(description, new FollowUp(TODAY, " " + description + " ").getDescription());
        }
    }

    @Test
    public void constructor_unicodeLengthBoundary_countsCodePointsRatherThanUtf16Units() {
        for (String description : new String[] {"续".repeat(200), "😀".repeat(200), "e\u0301".repeat(100),
            "👩\u200d💻".repeat(66) + "!!"}) {
            assertEquals(200, description.codePointCount(0, description.length()));
            assertEquals(description, new FollowUp(TODAY, " " + description + " ").getDescription());
            assertThrows(IllegalArgumentException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, () ->
                    new FollowUp(TODAY, description + "!"));
        }
    }

    @Test
    public void constructor_invisibleOrCombiningMarksOnly_rejectsDescription() {
        String[] descriptions = {"\u00a0", "\u3000", "\u200b", "\u200c\u200d", "\ufeff", "\u034f",
            "\ufe0f", "\udb40\udd00", "\u0301", "\u115f\u1160", "\u3164", "\uffa0", "\u2800",
            " \u00a0\u200b\u3000\ufe0f "};
        for (String description : descriptions) {
            assertThrows(IllegalArgumentException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, () ->
                    new FollowUp(TODAY, description));
        }
    }

    @Test
    public void constructor_allIsoControls_rejectsDescription() {
        for (int codePoint = 0; codePoint <= 0x9f; codePoint++) {
            if (Character.isISOControl(codePoint)) {
                String description = "Call" + (char) codePoint + "client";
                assertThrows(IllegalArgumentException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, () ->
                        new FollowUp(TODAY, description));
            }
        }
    }

    @Test
    public void constructor_supportedYearRange_acceptsPastDates() {
        LocalDate earliest = LocalDate.of(1, 1, 1);
        LocalDate latest = LocalDate.of(9999, 12, 31);
        assertEquals(earliest, new FollowUp(earliest, "Overdue action").getDueDate());
        assertEquals(latest, new FollowUp(latest, "Future action").getDueDate());
    }

    @Test
    public void constructor_unsupportedYear_throwsIllegalArgumentException() {
        for (LocalDate date : new LocalDate[] {LocalDate.of(0, 1, 1), LocalDate.of(-1, 1, 1),
            LocalDate.of(10000, 1, 1)}) {
            assertThrows(IllegalArgumentException.class, FollowUp.MESSAGE_INVALID_DATE, () ->
                    new FollowUp(date, "Call client"));
        }
    }

    @Test
    public void parseDate_validDates_returnsStrictDate() {
        assertEquals(LocalDate.of(2028, 2, 29), FollowUp.parseDate("  2028-02-29  "));
        assertEquals(LocalDate.of(2000, 2, 29), FollowUp.parseDate("2000-02-29"));
        assertEquals(LocalDate.of(1, 1, 1), FollowUp.parseDate("0001-01-01"));
        assertEquals(LocalDate.of(9999, 12, 31), FollowUp.parseDate("9999-12-31"));
    }

    @Test
    public void parseDate_invalidDates_throwsIllegalArgumentException() {
        String[] invalidDates = {"", " ", "2026-02-29", "1900-02-29", "2026-02-30", "2026-04-31",
            "2026-00-01", "2026-13-01", "2026-01-00", "2026-01-32", "0000-01-01", "10000-01-01",
            "+10000-01-01", "-0001-01-01", "+2026-01-01", "2026-1-01", "2026-01-1", "26-01-01",
            "01/01/2026", "2026- 01-01", "２０２６-01-01", "\t2026-01-01", "2026-01-01\n",
            "\u00a02026-01-01", "2026-01-01T00:00"};
        for (String date : invalidDates) {
            assertThrows(IllegalArgumentException.class, FollowUp.MESSAGE_INVALID_DATE, () ->
                    FollowUp.parseDate(date));
        }
    }

    @Test
    public void parseDate_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> FollowUp.parseDate(null));
    }

    @Test
    public void getStatus_relativeToSuppliedDate_returnsStatus() {
        FollowUp followUp = new FollowUp(TODAY, "Call client");
        assertEquals(FollowUpStatus.UPCOMING, followUp.getStatus(LocalDate.of(2026, 10, 4)));
        assertEquals(FollowUpStatus.DUE_TODAY, followUp.getStatus(TODAY));
        assertEquals(FollowUpStatus.OVERDUE, followUp.getStatus(LocalDate.of(2026, 10, 6)));
        assertEquals("UPCOMING", followUp.getStatus(LocalDate.of(2026, 10, 4)).getDisplayName());
        assertEquals("DUE TODAY", followUp.getStatus(TODAY).getDisplayName());
        assertEquals("OVERDUE", followUp.getStatus(LocalDate.of(2026, 10, 6)).getDisplayName());
    }

    @Test
    public void getStatus_nullDate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FollowUp(TODAY, "Call client").getStatus(null));
    }

    @Test
    public void equals_comparesDateAndNormalizedDescription() {
        FollowUp followUp = new FollowUp(TODAY, "Call client");
        FollowUp sameValue = new FollowUp(TODAY, "  Call client  ");
        assertTrue(followUp.equals(followUp));
        assertTrue(followUp.equals(sameValue));
        assertTrue(sameValue.equals(followUp));
        assertEquals(followUp.hashCode(), sameValue.hashCode());
        assertFalse(followUp.equals(null));
        assertFalse(followUp.equals("Call client"));
        assertFalse(followUp.equals(new FollowUp(LocalDate.of(2026, 10, 6), "Call client")));
        assertFalse(followUp.equals(new FollowUp(TODAY, "Call Client")));
        assertFalse(followUp.equals(new FollowUp(TODAY, "Call  client")));
    }
}
