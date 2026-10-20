package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class HelpAndFooterTest {

    @Test
    public void userGuideUrl_pointsToTeamGuide() {
        assertEquals("https://ay2627s1-cs2103t-w13-3.github.io/tp/UserGuide.html", HelpWindow.USERGUIDE_URL);
    }

    @Test
    public void getCopyStatusMessage_result_reportsSuccessOrFallback() {
        assertEquals("Link copied.", HelpWindow.getCopyStatusMessage(true));
        assertNotEquals(HelpWindow.getCopyStatusMessage(true), HelpWindow.getCopyStatusMessage(false));
    }

    @Test
    public void formatSaveLocation_relativePath_showsLabelledPath() {
        assertEquals("Data file: " + Paths.get(".", "data", "addressbook.json"),
                StatusBarFooter.formatSaveLocation(Paths.get("data", "addressbook.json")));
    }

    @Test
    public void formatSaveLocation_longPath_keepsCompletePath() {
        String longFolder = "f".repeat(150);
        String text = StatusBarFooter.formatSaveLocation(Paths.get(longFolder, "addressbook.json"));

        assertEquals("Data file: " + Paths.get(".", longFolder, "addressbook.json"), text);
    }

    @Test
    public void formatSaveLocation_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StatusBarFooter.formatSaveLocation(null));
    }
}
