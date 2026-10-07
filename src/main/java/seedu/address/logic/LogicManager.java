package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.logging.Logger;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final Clock clock;
    private final AddressBookParser addressBookParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this(model, storage, Clock.systemDefaultZone());
    }

    /**
     * Constructs a {@code LogicManager} with an injectable clock for date-dependent commands.
     */
    public LogicManager(Model model, Storage storage, Clock clock) {
        this.model = requireNonNull(model);
        this.storage = requireNonNull(storage);
        this.clock = requireNonNull(clock);
        addressBookParser = new AddressBookParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        CommandResult commandResult;
        Command command = addressBookParser.parseCommand(commandText, model.isDataLoadingBlocked());
        commandResult = command.execute(model);

        try {
            storage.saveAddressBook(model.getAddressBook());
        } catch (AccessDeniedException e) {
            throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
        } catch (IOException ioe) {
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
        }

        return commandResult;
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public ReadOnlyObjectProperty<LocalDate> todayProperty() {
        return model.todayProperty();
    }

    @Override
    public ReadOnlyBooleanProperty showingFollowUpsProperty() {
        return model.showingFollowUpsProperty();
    }

    @Override
    public boolean isDataLoadingBlocked() {
        return model.isDataLoadingBlocked();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
