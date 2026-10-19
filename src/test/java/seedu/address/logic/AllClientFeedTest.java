package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Tests the committed full-data feed independently of the command list and candidate models. */
public class AllClientFeedTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    public Path testFolder;

    private Person first;
    private Person second;
    private Model model;
    private Logic logic;
    private ObservableList<Person> feed;
    private List<List<Person>> publications;

    @BeforeEach
    public void setUp() {
        first = new PersonBuilder().withName("Rachel Lim").withPhone("111")
                .withEmail("first@example.com").withFollowUp(new FollowUp(TODAY.plusDays(2), "Call first"))
                .build();
        second = new PersonBuilder().withName("Rachel Lim").withPhone("222")
                .withEmail("second@example.com").withFollowUp(new FollowUp(TODAY, "Call second")).build();
        AddressBook book = new AddressBook();
        book.addPerson(first);
        book.addPerson(second);
        model = new ModelManager(book, new UserPrefs(), TODAY, false);
        logic = new LogicManager(model, storage(false), CLOCK);
        feed = logic.getAllPersonList();
        publications = new ArrayList<>();
        feed.addListener((ListChangeListener<Person>) change -> publications.add(List.copyOf(feed)));
    }

    @Test
    public void getAllPersonList_stableReadOnlyView_preservesCanonicalRecords() {
        assertSame(feed, logic.getAllPersonList());
        assertSame(model.getAddressBook().getPersonList(), feed);
        assertSame(first, feed.get(0));
        assertSame(second, feed.get(1));
        assertThrows(UnsupportedOperationException.class, () -> feed.add(first));
        assertThrows(UnsupportedOperationException.class, () -> feed.set(0, second));
        assertThrows(UnsupportedOperationException.class, () -> feed.remove(0));
        assertThrows(UnsupportedOperationException.class, feed::clear);
        assertEquals(List.of(first, second), feed);
        assertTrue(publications.isEmpty());
    }

    @Test
    public void readOnlyCommands_keepFullInsertionOrderAndDoNotPublishData() throws Exception {
        logic.execute("view 2");
        assertSame(second, logic.selectedPersonProperty().get());
        logic.execute("followups");
        assertEquals(List.of(second, first), logic.getFilteredPersonList());
        assertEquals(List.of(first, second), feed);
        assertSame(second, logic.selectedPersonProperty().get());
        logic.execute("find Nobody");
        assertTrue(logic.getFilteredPersonList().isEmpty());
        assertNull(logic.selectedPersonProperty().get());
        assertEquals(List.of(first, second), feed);
        logic.execute("list");
        assertSame(feed, logic.getAllPersonList());
        assertTrue(publications.isEmpty());
        assertFalse(Files.exists(testFolder.resolve("clients.json")));
    }

    @Test
    public void committedMutations_publishFullRecordsAndRetainSelectedIdentity() throws Exception {
        logic.execute("view 2");
        logic.execute("followups");
        logic.execute("followup 1 d/2026-10-22 m/Updated second");
        assertEquals(1, publications.size());
        Person replacement = feed.get(1);
        assertSame(first, feed.get(0));
        assertSame(replacement, logic.selectedPersonProperty().get());
        assertSame(replacement, logic.getFilteredPersonList().get(1));
        assertEquals("222", replacement.getPhone().value);
        assertEquals("Updated second", replacement.getFollowUp().orElseThrow().getDescription());

        logic.execute("followup 2 clear");
        assertEquals(2, publications.size());
        assertTrue(feed.get(1).getFollowUp().isEmpty());
        assertNull(logic.selectedPersonProperty().get());
        assertEquals(List.of(first), logic.getFilteredPersonList());

        logic.execute("add n/Amanda Lee p/333 e/amanda@example.com a/Street");
        assertEquals(3, publications.size());
        assertEquals(List.of("Rachel Lim", "Rachel Lim", "Amanda Lee"),
                feed.stream().map(person -> person.getName().fullName).toList());
        logic.execute("find Amanda");
        logic.execute("delete 1");
        assertEquals(4, publications.size());
        assertEquals(2, feed.size());
        assertSame(first, feed.get(0));
        assertEquals("222", feed.get(1).getPhone().value);
        assertSame(feed, logic.getAllPersonList());
        assertEquals(feed, new JsonAddressBookStorage(testFolder.resolve("clients.json"))
                .readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void candidateChanges_remainPrivateUntilCommit() {
        Model candidate = model.forkForCommand(TODAY);
        Person replacement = second.withoutFollowUp();
        candidate.setPerson(second, replacement);
        candidate.deletePerson(first);
        assertEquals(List.of(first, second), feed);
        assertTrue(publications.isEmpty());

        model.commitFrom(candidate);
        assertSame(feed, logic.getAllPersonList());
        assertEquals(List.of(replacement), feed);
        assertSame(replacement, feed.get(0));
        assertEquals(List.of(List.of(replacement)), publications);
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void rejectedCommandsAndFailedSaves_doNotPublishCandidateData() throws Exception {
        logic.execute("view 2");
        logic.execute("followups");
        Logic failingLogic = new LogicManager(model, storage(true), CLOCK);
        for (String command : List.of("delete 1", "followup 1 clear",
                "followup 1 d/2026-10-22 m/Unsaved action",
                "add n/Amanda Lee p/333 e/amanda@example.com a/Street")) {
            CommandException error = assertThrows(CommandException.class, () -> failingLogic.execute(command));
            assertEquals(LogicManager.MESSAGE_SAVE_FAILED, error.getMessage());
            assertUnpublished();
        }
        assertThrows(ParseException.class, () -> logic.execute("unknown"));
        assertThrows(CommandException.class, () -> logic.execute("delete 3"));
        assertThrows(CommandException.class, () -> logic.execute("followup 1 d/2026-10-18 m/Past"));
        assertThrows(CommandException.class, () ->
                logic.execute("add n/Rachel Lim p/222 e/other@example.com a/Street"));
        assertUnpublished();
        assertFalse(Files.exists(testFolder.resolve("clients.json")));
    }

    @Test
    public void blockedSession_exposesEmptyStableFeedWithoutPublishingRejectedMutations() {
        Model blockedModel = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, true);
        Logic blockedLogic = new LogicManager(blockedModel, storage(false), CLOCK);
        ObservableList<Person> blockedFeed = blockedLogic.getAllPersonList();
        List<List<Person>> blockedPublications = new ArrayList<>();
        blockedFeed.addListener((ListChangeListener<Person>) change ->
                blockedPublications.add(List.copyOf(blockedFeed)));
        assertThrows(ParseException.class, () -> blockedLogic.execute("delete 1"));
        assertSame(blockedFeed, blockedLogic.getAllPersonList());
        assertTrue(blockedFeed.isEmpty());
        assertTrue(blockedPublications.isEmpty());
    }

    private void assertUnpublished() {
        assertEquals(List.of(first, second), feed);
        assertSame(first, feed.get(0));
        assertSame(second, feed.get(1));
        assertSame(second, logic.selectedPersonProperty().get());
        assertEquals(List.of(second, first), logic.getFilteredPersonList());
        assertTrue(logic.showingFollowUpsProperty().get());
        assertEquals(TODAY, logic.todayProperty().get());
        assertTrue(publications.isEmpty());
        assertFalse(model.hasUnsavedChanges());
    }

    private StorageManager storage(boolean failSave) {
        return new StorageManager(new JsonAddressBookStorage(testFolder.resolve("clients.json")),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook book) throws IOException {
                if (failSave) {
                    throw new IOException("Simulated save failure");
                }
                // Saving must finish before any live-feed observer receives the candidate.
                assertEquals(publications.isEmpty() ? List.of(first, second) : publications.getLast(), feed);
                super.saveAddressBook(book);
            }
        };
    }
}
