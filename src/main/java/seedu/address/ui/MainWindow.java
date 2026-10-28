package seedu.address.ui;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.awt.Point;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";
    private static final double DEFAULT_WINDOW_WIDTH = 1000;
    private static final double DEFAULT_WINDOW_HEIGHT = 720;
    private static final double MIN_WINDOW_WIDTH = 640;
    private static final double MIN_WINDOW_HEIGHT = 600;

    private final Logger logger = LogsCenter.getLogger(getClass());

    private final Stage primaryStage;
    private final Logic logic;
    private final Path dataFilePath;

    // Independent Ui parts residing in this Ui container
    private PersonListPanel personListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private PendingFollowUpProjection pendingProjection;
    private PendingFollowUpPanel pendingPanel;

    @FXML
    private Label loadWarning;

    @FXML
    private Label clientListTitle;

    @FXML
    private Label clientCount;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane clientProfilePlaceholder;

    @FXML
    private StackPane pendingFollowUpPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, requireNonNull(primaryStage));

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = requireNonNull(logic);
        this.dataFilePath = requireNonNull(dataFilePath);

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
        primaryStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> disposeDashboard());
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        if (resultDisplay != null) {
            throw new IllegalStateException("Dashboard components have already been initialized");
        }
        bindClientListLabels(logic, clientListTitle.textProperty(), clientCount.textProperty());

        personListPanel = new PersonListPanel(logic.getFilteredPersonList(), logic.todayProperty(),
                logic.showingFollowUpsProperty(), logic.selectedPersonProperty());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        ClientProfilePanel clientProfilePanel = new ClientProfilePanel(logic.selectedPersonProperty(),
                logic.todayProperty());
        clientProfilePlaceholder.getChildren().add(clientProfilePanel.getRoot());

        pendingProjection = new PendingFollowUpProjection(logic.getAllPersonList());
        pendingPanel = new PendingFollowUpPanel(pendingProjection.getPendingPersons(),
                logic.todayProperty(), logic.selectedPersonProperty());
        pendingFollowUpPlaceholder.getChildren().setAll(pendingPanel.getRoot());
        pendingFollowUpPlaceholder.setVisible(true);
        pendingFollowUpPlaceholder.setManaged(true);

        boolean showLoadWarning = logic.isDataLoadingBlocked();
        loadWarning.setText(Messages.MESSAGE_LOAD_WARNING);
        loadWarning.setVisible(showLoadWarning);
        loadWarning.setManaged(showLoadWarning);

        resultDisplay = new ResultDisplay();
        resultDisplay.setFeedbackToUser(ListCommand.MESSAGE_SUCCESS);
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(this::executeCommand, resultDisplay::setErrorFeedbackToUser);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
    }

    /** Binds the list heading to the active view and its displayed rows. */
    static void bindClientListLabels(Logic logic, StringProperty title, StringProperty count) {
        requireNonNull(logic);
        requireNonNull(title);
        requireNonNull(count);
        title.bind(Bindings.when(logic.showingFollowUpsProperty()).then("Pending clients").otherwise("Clients"));
        count.bind(Bindings.size(logic.getFilteredPersonList()).asString("%d shown"));
    }

    /**
     * Restores a usable window on the saved display, or the primary display if it is unavailable.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        Screen screen = Screen.getPrimary();
        Point coordinates = guiSettings.getWindowCoordinates();
        if (coordinates != null) {
            List<Screen> savedScreens = Screen.getScreensForRectangle(coordinates.x, coordinates.y, 1, 1);
            if (!savedScreens.isEmpty()) {
                screen = savedScreens.get(0);
            }
        }
        Rectangle2D visualBounds = screen.getVisualBounds();
        Rectangle2D windowBounds = getWindowBounds(guiSettings, visualBounds);
        primaryStage.setMinWidth(Math.min(MIN_WINDOW_WIDTH, visualBounds.getWidth()));
        primaryStage.setMinHeight(Math.min(MIN_WINDOW_HEIGHT, visualBounds.getHeight()));
        primaryStage.setWidth(windowBounds.getWidth());
        primaryStage.setHeight(windowBounds.getHeight());
        primaryStage.setX(windowBounds.getMinX());
        primaryStage.setY(windowBounds.getMinY());
    }

    /**
     * Fits persisted geometry inside a display's usable area, without growing to fit client content.
     * Invalid persisted dimensions fall back to the dashboard defaults; valid positions are retained.
     */
    static Rectangle2D getWindowBounds(GuiSettings guiSettings, Rectangle2D visualBounds) {
        requireNonNull(guiSettings);
        requireNonNull(visualBounds);
        checkArgument(visualBounds.getWidth() > 0 && visualBounds.getHeight() > 0
                && Double.isFinite(visualBounds.getMinX()) && Double.isFinite(visualBounds.getMinY())
                && Double.isFinite(visualBounds.getMaxX()) && Double.isFinite(visualBounds.getMaxY()),
                "Display bounds must be finite and have positive dimensions.");
        boolean isFirstLaunch = guiSettings.equals(new GuiSettings());
        double width = fitDimension(isFirstLaunch ? DEFAULT_WINDOW_WIDTH : guiSettings.getWindowWidth(),
                DEFAULT_WINDOW_WIDTH, MIN_WINDOW_WIDTH, visualBounds.getWidth());
        double height = fitDimension(isFirstLaunch ? DEFAULT_WINDOW_HEIGHT : guiSettings.getWindowHeight(),
                DEFAULT_WINDOW_HEIGHT, MIN_WINDOW_HEIGHT, visualBounds.getHeight());
        Point coordinates = guiSettings.getWindowCoordinates();
        double x = coordinates == null ? visualBounds.getMinX() + (visualBounds.getWidth() - width) / 2
                : Math.max(visualBounds.getMinX(), Math.min(coordinates.x, visualBounds.getMaxX() - width));
        double y = coordinates == null ? visualBounds.getMinY() + (visualBounds.getHeight() - height) / 2
                : Math.max(visualBounds.getMinY(), Math.min(coordinates.y, visualBounds.getMaxY() - height));
        Rectangle2D restored = new Rectangle2D(x, y, width, height);
        assert visualBounds.contains(restored) : "Restored window must fit its selected display";
        return restored;
    }

    private static double fitDimension(double requested, double fallback, double minimum, double available) {
        double validSize = Double.isFinite(requested) && requested > 0 ? requested : fallback;
        return Math.min(available, Math.max(minimum, validSize));
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        disposeDashboard();
        primaryStage.hide();
    }

    /** Releases the observers owned by this window, including when it is hidden programmatically. */
    private void disposeDashboard() {
        if (pendingPanel != null) {
            pendingPanel.dispose();
        }
        if (pendingProjection != null) {
            pendingProjection.close();
        }
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            // Feedback can contain client contact/action details, so it is never logged.
            logger.info("Command succeeded.");
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing a command.");
            resultDisplay.setErrorFeedbackToUser(e.getMessage());
            throw e;
        }
    }
}
