package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * A UI for the status bar that is displayed at the footer of the application.
 */
public class StatusBarFooter extends UiPart<Region> {

    private static final String FXML = "StatusBarFooter.fxml";
    private static final double TOOLTIP_MAX_WIDTH = 400;

    @FXML
    private Label saveLocationStatus;

    /**
     * Creates a {@code StatusBarFooter} with the given {@code Path}.
     * A long path is shortened from the left so the file name stays visible; the tooltip shows it in full,
     * wrapped to a bounded width and kept open until the pointer leaves.
     */
    public StatusBarFooter(Path saveLocation) {
        super(FXML);
        String text = formatSaveLocation(saveLocation);
        saveLocationStatus.setText(text);
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
        tooltip.setShowDuration(Duration.INDEFINITE);
        saveLocationStatus.setTooltip(tooltip);
    }

    /** Returns the footer text for {@code saveLocation}, without changing the actual data path. */
    static String formatSaveLocation(Path saveLocation) {
        requireNonNull(saveLocation);
        return "Data file: " + Paths.get(".").resolve(saveLocation);
    }

}
