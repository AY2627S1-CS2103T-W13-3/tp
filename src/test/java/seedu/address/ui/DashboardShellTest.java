package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import javafx.geometry.Rectangle2D;
import seedu.address.commons.core.GuiSettings;

public class DashboardShellTest {
    private static final Rectangle2D DISPLAY = new Rectangle2D(0, 25, 1440, 875);

    @Test
    public void getWindowBounds_firstLaunch_centersDashboardWithinDisplay() {
        assertEquals(new Rectangle2D(220, 102.5, 1000, 720),
                MainWindow.getWindowBounds(new GuiSettings(), DISPLAY));
    }

    @Test
    public void getWindowBounds_validSavedPosition_preservesBounds() {
        GuiSettings saved = new GuiSettings(900, 640, 120, 80);
        assertEquals(new Rectangle2D(120, 80, 900, 640), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_tinySavedSize_keepsWorkspaceUsable() {
        GuiSettings saved = new GuiSettings(80, 90, 50, 30);
        assertEquals(new Rectangle2D(50, 30, 640, 600), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_oversizedOffscreenWindow_fitsDisplay() {
        GuiSettings saved = new GuiSettings(8000, 6000, 9999, 9999);
        assertEquals(DISPLAY, MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_offscreenCoordinates_recoversVisiblePosition() {
        GuiSettings saved = new GuiSettings(900, 640, 5000, -1000);
        assertEquals(new Rectangle2D(540, 25, 900, 640), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_negativeMonitorCoordinates_preservesVisibleWindow() {
        Rectangle2D secondDisplay = new Rectangle2D(-1920, 0, 1920, 1080);
        GuiSettings saved = new GuiSettings(900, 640, -1800, 100);
        assertEquals(new Rectangle2D(-1800, 100, 900, 640),
                MainWindow.getWindowBounds(saved, secondDisplay));
    }

    @Test
    public void getWindowBounds_smallDisplay_limitsMinimumToAvailableSpace() {
        Rectangle2D smallDisplay = new Rectangle2D(0, 0, 600, 400);
        assertEquals(smallDisplay,
                MainWindow.getWindowBounds(new GuiSettings(640, 600, 0, 0), smallDisplay));
    }

    @Test
    public void getWindowBounds_nonFiniteSavedSize_usesFiniteDefaults() {
        GuiSettings saved = new GuiSettings(Double.NaN, Double.POSITIVE_INFINITY, 100, 100);
        assertEquals(new Rectangle2D(100, 100, 1000, 720), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_nonPositiveSavedSize_usesUsableDefaults() {
        GuiSettings saved = new GuiSettings(-1, 0, 100, 100);
        assertEquals(new Rectangle2D(100, 100, 1000, 720), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_fractionalDisplayBounds_staysInsideDisplay() {
        Rectangle2D scaledDisplay = new Rectangle2D(100.5, 10.25, 853.333, 430.5);
        assertEquals(scaledDisplay,
                MainWindow.getWindowBounds(new GuiSettings(1000, 720, 100, 10), scaledDisplay));
    }

    @Test
    public void getWindowBounds_nullArguments_rejectsProgrammerError() {
        assertThrows(NullPointerException.class, () -> MainWindow.getWindowBounds(null, DISPLAY));
        assertThrows(NullPointerException.class, () -> MainWindow.getWindowBounds(new GuiSettings(), null));
    }

    @Test
    public void getWindowBounds_emptyDisplay_rejectsProgrammerError() {
        assertThrows(IllegalArgumentException.class, () ->
                MainWindow.getWindowBounds(new GuiSettings(), new Rectangle2D(0, 0, 0, 900)));
    }
}
