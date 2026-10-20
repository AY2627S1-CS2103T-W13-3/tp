package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/**
 * A UI for the status bar that is displayed at the footer of the application.
 */
public class StatusBarFooter extends UiPart<Region> {

    private static final String FXML = "StatusBarFooter.fxml";

    @FXML
    private Label saveLocationStatus;

    /**
     * Creates a {@code StatusBarFooter} with the given {@code Path}.
     * A long path is shortened from the left so the file name stays visible; the tooltip shows it in full.
     */
    public StatusBarFooter(Path saveLocation) {
        super(FXML);
        String text = formatSaveLocation(saveLocation);
        saveLocationStatus.setText(text);
        saveLocationStatus.setTooltip(new Tooltip(text));
    }

    /** Returns the footer text for {@code saveLocation}, without changing the actual data path. */
    static String formatSaveLocation(Path saveLocation) {
        requireNonNull(saveLocation);
        return "Data file: " + Paths.get(".").resolve(saveLocation);
    }

}
