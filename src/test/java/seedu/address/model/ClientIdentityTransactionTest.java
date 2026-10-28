package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Verifies Gwen's model contract against the real aggregate identity implementation. */
public class ClientIdentityTransactionTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final ClientId FIRST = new ClientId(2);
    private static final ClientId SELECTED = new ClientId(17);
    private static final ClientId LAST = new ClientId(41);
    private static final List<ClientId> IDS = List.of(FIRST, SELECTED, LAST);

    @TempDir
    public Path testFolder;

    @Test
    public void constructionAndFork_preserveIdsCounterAndIndependentAllocation() {
        AddressBook original = book(List.of(ALICE, BENSON, CARL), 42);
        Model live = new ModelManager(original, new UserPrefs(), TODAY, false);
        live.selectPerson(BENSON);
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        assertNotSame(original, live.getAddressBook());
        assertNotSame(live.getAddressBook(), candidate.getAddressBook());
        assertEquals(original, candidate.getAddressBook());
        assertSame(BENSON, candidate.selectedPersonProperty().get());
        assertFalse(candidate.hasUnsavedChanges());

        Person added = new PersonBuilder().withName("Daphne Tan").build();
        candidate.addPerson(added);
        assertEquals(new ClientId(42), candidate.getAddressBook().getClientId(added).orElseThrow());
        assertEquals(43, candidate.getAddressBook().getNextClientId());
        assertEquals(42, live.getAddressBook().getNextClientId());
        assertTrue(live.getAddressBook().getClientId(added).isEmpty());
        // Discarding a failed candidate does not reserve an ID in the live aggregate.
        Model retry = live.forkForCommand(TODAY);
        retry.addPerson(added);
        assertEquals(new ClientId(42), retry.getAddressBook().getClientId(added).orElseThrow());
    }

    @Test
    public void commit_reorderedCandidate_reconcilesSelectionByIdAndKeepsObservables() {
        Model live = model(ALICE, BENSON, CARL);
        live.selectPerson(BENSON);
        var displayed = live.getFilteredPersonList();
        var selected = live.selectedPersonProperty();
        var date = live.todayProperty();
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        AddressBook reordered = new AddressBook();
        reordered.restorePersons(List.of(CARL, ALICE, BENSON), List.of(LAST, FIRST, SELECTED), 42);
        candidate.setAddressBook(reordered);
        assertSame(BENSON, candidate.selectedPersonProperty().get());
        live.commitFrom(candidate);
        assertSame(displayed, live.getFilteredPersonList());
        assertSame(selected, live.selectedPersonProperty());
        assertSame(date, live.todayProperty());
        assertSame(live.getAddressBook().getPersonById(SELECTED).orElseThrow(), selected.get());
        assertEquals(IDS, List.of(live.getAddressBook().getClientId(ALICE).orElseThrow(),
                live.getAddressBook().getClientId(BENSON).orElseThrow(),
                live.getAddressBook().getClientId(CARL).orElseThrow()));
        assertEquals(TODAY.plusDays(1), date.get());
        assertFalse(live.hasUnsavedChanges());
    }

    @Test
    public void commit_metadataOnlyChange_publishesCounterWithoutReallocating() {
        Model live = model(ALICE, BENSON, CARL);
        live.selectPerson(BENSON);
        Model candidate = live.forkForCommand(TODAY);
        candidate.setAddressBook(book(List.of(ALICE, BENSON, CARL), 100));
        live.commitFrom(candidate);
        assertEquals(100, live.getAddressBook().getNextClientId());
        assertSame(BENSON, live.selectedPersonProperty().get());
        assertEquals(SELECTED, live.getAddressBook().getClientId(BENSON).orElseThrow());
        assertFalse(live.hasUnsavedChanges());
    }

    @Test
    public void replacementsAndPendingReorder_keepIdentityAndClearOnlyWhenHidden() {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY, "Call Alice"));
        Person benson = BENSON.withFollowUp(new FollowUp(TODAY.plusDays(1), "Call Benson"));
        Model live = model(alice, benson, CARL);
        live.showPendingFollowUps();
        live.selectPerson(benson);
        Model candidate = live.forkForCommand(TODAY);
        Person changed = new PersonBuilder(benson).withPhone("+65 9234 5678")
                .withFollowUp(new FollowUp(TODAY.minusDays(1), "保单更新 🏠")).build();
        candidate.setPerson(new PersonBuilder(benson).build(), changed);
        live.commitFrom(candidate);
        assertEquals(List.of(changed, alice), live.getFilteredPersonList());
        assertSame(changed, live.selectedPersonProperty().get());
        assertEquals(SELECTED, live.getAddressBook().getClientId(changed).orElseThrow());
        assertTrue(live.showingFollowUpsProperty().get());
        assertEquals(42, live.getAddressBook().getNextClientId());

        candidate = live.forkForCommand(TODAY);
        candidate.setPerson(changed, changed.withoutFollowUp());
        live.commitFrom(candidate);
        assertNull(live.selectedPersonProperty().get());
        assertEquals(List.of(alice), live.getFilteredPersonList());
        assertTrue(live.getAddressBook().getPersonById(SELECTED).isPresent());
    }

    @Test
    public void equalValuedReplacement_bindsToTheActualCommittedInstance() {
        Model live = model(ALICE, BENSON, CARL);
        live.selectPerson(BENSON);
        Model candidate = live.forkForCommand(TODAY);
        candidate.setPerson(BENSON, new PersonBuilder(BENSON).build());
        live.commitFrom(candidate);
        assertSame(live.getAddressBook().getPersonById(SELECTED).orElseThrow(),
                live.selectedPersonProperty().get());
        assertEquals(SELECTED, live.getAddressBook().getClientId(live.selectedPersonProperty().get()).orElseThrow());
    }

    @Test
    public void addAndEdit_searchPredicateAndSelectionRemainActive() {
        Model live = model(ALICE, BENSON, CARL);
        live.updateFilteredPersonList(person -> person.getName().fullName.startsWith("Alice"));
        live.selectPerson(ALICE);
        Model candidate = live.forkForCommand(TODAY);
        candidate.addPerson(new PersonBuilder().withName("Nonmatching Client").build());
        assertEquals(List.of(ALICE), candidate.getFilteredPersonList());
        Person matching = new PersonBuilder().withName("Alice New").build();
        candidate.addPerson(matching);
        live.commitFrom(candidate);
        assertEquals(List.of(ALICE, matching), live.getFilteredPersonList());
        assertSame(ALICE, live.selectedPersonProperty().get());
        assertFalse(live.showingFollowUpsProperty().get());
        candidate = live.forkForCommand(TODAY);
        candidate.setPerson(ALICE, new PersonBuilder(ALICE).withName("Zelda").build());
        live.commitFrom(candidate);
        assertEquals(List.of(matching), live.getFilteredPersonList());
        assertNull(live.selectedPersonProperty().get());
        assertEquals(FIRST, live.getAddressBook().getClientId(
                live.getAddressBook().getPersonById(FIRST).orElseThrow()).orElseThrow());
    }

    @Test
    public void add_pendingMode_keepsActionlessClientHiddenAndExistingSelection() {
        Person pending = BENSON.withFollowUp(new FollowUp(TODAY, "Renew policy"));
        Model live = model(ALICE, pending, CARL);
        live.showPendingFollowUps();
        live.selectPerson(pending);
        Model candidate = live.forkForCommand(TODAY);
        candidate.addPerson(new PersonBuilder().withName("Daphne Tan").build());
        live.commitFrom(candidate);
        assertEquals(List.of(pending), live.getFilteredPersonList());
        assertTrue(live.showingFollowUpsProperty().get());
        assertSame(pending, live.selectedPersonProperty().get());
    }

    @Test
    public void publication_listenersCanResolveIdsAndReadOnlyCommandsStayClean() {
        Model live = model(ALICE, BENSON, CARL);
        live.getFilteredPersonList().addListener((ListChangeListener<Person>) change -> {
            for (Person person : change.getList()) {
                assertTrue(live.getAddressBook().getClientId(person).isPresent());
            }
        });
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        candidate.deletePerson(ALICE);
        candidate.selectPerson(BENSON);
        live.commitFrom(candidate);
        Model query = live.forkForCommand(TODAY);
        query.updateFilteredPersonList(person -> person == BENSON);
        query.selectPerson(BENSON);
        assertFalse(query.hasUnsavedChanges());
        live.commitFrom(query);
        assertSame(BENSON, live.selectedPersonProperty().get());
        assertEquals(42, live.getAddressBook().getNextClientId());
    }

    @Test
    public void failedDeleteAndFollowUpSaves_preserveBytesIdsViewSelectionAndCounter() throws Exception {
        Person pending = BENSON.withFollowUp(new FollowUp(TODAY, "Call insurer"));
        Model live = model(ALICE, pending, CARL);
        live.showPendingFollowUps();
        live.selectPerson(pending);
        Path file = testFolder.resolve("clients.json");
        new JsonAddressBookStorage(file).saveAddressBook(live.getAddressBook());
        byte[] original = Files.readAllBytes(file);
        AddressBook before = new AddressBook(live.getAddressBook());
        JsonAddressBookStorage failing = new JsonAddressBookStorage(file) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
                throw new IOException("Synthetic save failure");
            }
        };
        StorageManager storage = new StorageManager(failing,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json")));
        LogicManager logic = new LogicManager(live, storage,
                Clock.fixed(TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        String saveMessage = String.format(LogicManager.MESSAGE_SAVE_FAILED, file);
        for (String command : List.of("delete 17", "followup 17 d/2026-10-20 m/保单更新", "followup 17 clear")) {
            assertThrows(CommandException.class, saveMessage, () -> logic.execute(command));
            assertEquals(before, live.getAddressBook());
            assertEquals(42, live.getAddressBook().getNextClientId());
            assertEquals(List.of(pending), live.getFilteredPersonList());
            assertSame(pending, live.selectedPersonProperty().get());
            assertTrue(live.showingFollowUpsProperty().get());
            assertEquals(TODAY, live.getToday());
            assertArrayEquals(original, Files.readAllBytes(file));
            assertFalse(live.hasUnsavedChanges());
        }
    }

    private static AddressBook book(List<Person> persons, long counter) {
        AddressBook book = new AddressBook();
        book.restorePersons(persons, IDS, counter);
        return book;
    }

    private static Model model(Person first, Person selected, Person last) {
        return new ModelManager(book(List.of(first, selected, last), 42), new UserPrefs(), TODAY, false);
    }
}
