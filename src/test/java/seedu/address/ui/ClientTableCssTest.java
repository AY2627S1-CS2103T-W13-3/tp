package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Checks the row-state cascade in ClientTable.css. Real keyboard focus needs an OS-focused window,
 * so stand-in nodes set the table and row pseudo-classes directly.
 */
public class ClientTableCssTest {

    private static final Color PLAIN = Color.web("#ffffff");
    private static final Color FOCUSED = Color.web("#f6f7f9");
    private static final Color GREEN_BAR = Color.web("#2e7d4f");

    @BeforeAll
    public static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Another test class started the toolkit.
        }
        Platform.setImplicitExit(false);
    }

    @Test
    public void focusedSelectedRow_inFocusedTable_showsFocusFill() throws Exception {
        assertEquals(List.of(FOCUSED), rowFills(true, "row", "filled", "selected", "focused"));
    }

    @Test
    public void selectedRow_withoutKeyboardFocus_staysPlain() throws Exception {
        assertEquals(List.of(PLAIN), rowFills(false, "row", "filled", "selected", "focused"));
        assertEquals(List.of(PLAIN), rowFills(true, "row", "filled", "selected"));
    }

    @Test
    public void focusedViewedRow_keepsGreenBarWithFocusFill() throws Exception {
        assertEquals(List.of(GREEN_BAR, Color.web("#d5eddf")),
                rowFills(true, "viewed-client", "filled", "selected", "focused"));
        assertEquals(List.of(GREEN_BAR, Color.web("#eaf6ef")),
                rowFills(false, "viewed-client", "filled", "selected", "focused"));
    }

    /**
     * Returns the background fills ClientTable.css gives a row in a table that does or does not have focus.
     * {@code rowClass} "viewed-client" marks the viewed row; anything else is an ordinary row.
     */
    private static List<Paint> rowFills(boolean tableFocused, String rowClass, String... rowStates)
            throws Exception {
        CompletableFuture<List<Paint>> fills = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                StatePane row = new StatePane("table-row-cell", rowClass);
                row.set(rowStates);
                StatePane table = new StatePane("table-view", "client-table");
                if (tableFocused) {
                    table.set("focused");
                }
                table.getChildren().add(row);
                StatePane panel = new StatePane("client-table-panel");
                panel.getChildren().add(table);
                panel.getStylesheets().add(ClientTableCssTest.class
                        .getResource("/view/ClientTable.css").toExternalForm());
                new Scene(panel).getRoot().applyCss();
                fills.complete(row.getBackground().getFills().stream().map(BackgroundFill::getFill).toList());
            } catch (Throwable t) {
                fills.completeExceptionally(t);
            }
        });
        return fills.get(30, TimeUnit.SECONDS);
    }

    /** A pane whose CSS pseudo-classes the test can switch on. */
    private static class StatePane extends Pane {
        StatePane(String... styleClasses) {
            getStyleClass().addAll(styleClasses);
        }

        void set(String... pseudoClasses) {
            for (String pseudoClass : pseudoClasses) {
                pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClass), true);
            }
        }
    }
}
