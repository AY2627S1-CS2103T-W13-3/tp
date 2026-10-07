package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Supplier;
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
    public static final String MESSAGE_SAVE_FAILED =
            "Changes could not be saved. No changes were kept. Try the command again.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final Supplier<Clock> clockSource;
    private final AddressBookParser addressBookParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this(model, storage, Clock::systemDefaultZone);
    }

    /**
     * Constructs a {@code LogicManager} with an injectable clock for date-dependent commands.
     */
    public LogicManager(Model model, Storage storage, Clock clock) {
        this(model, storage, fixedClockSource(clock));
    }

    private LogicManager(Model model, Storage storage, Supplier<Clock> clockSource) {
        this.model = requireNonNull(model);
        this.storage = requireNonNull(storage);
        this.clockSource = requireNonNull(clockSource);
        addressBookParser = new AddressBookParser();
    }

    private static Supplier<Clock> fixedClockSource(Clock clock) {
        requireNonNull(clock);
        return () -> clock;
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        Command command = addressBookParser.parseCommand(commandText, model.isDataLoadingBlocked());
        logger.info("Executing " + command.getClass().getSimpleName());
        Model candidate = model.forkForCommand(LocalDate.now(clockSource.get()));
        CommandResult commandResult = command.execute(candidate);

        if (candidate.hasUnsavedChanges()) {
            try {
                storage.saveAddressBook(candidate.getAddressBook());
            } catch (IOException e) {
                throw new CommandException(MESSAGE_SAVE_FAILED, e);
            }
        }

        model.commitFrom(candidate);
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
    public void refreshToday() {
        model.updateToday(LocalDate.now(clockSource.get()));
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
