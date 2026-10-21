package seedu.address.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Manual fixture: shows the status bar footer for a path of about 200 characters,
 * so the footer ellipsis and tooltip wrapping can be checked by hovering.
 */
public class StatusBarFooterLongPathFixture {
    public static void main(String[] args) {
        Path longPath = Paths.get(String.join("/", Collections.nCopies(14, "nested-folder")), "addressbook.json");
        Platform.startup(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(new StatusBarFooter(longPath).getRoot(), 600, 40));
            stage.show();
        });
    }
}
