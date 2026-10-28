package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.ELLE;
import static seedu.address.testutil.TypicalPersons.FIONA;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientContainsKeywordsPredicate;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code FindCommand}.
 */
public class FindCommandTest {
    private Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
    private Model expectedModel = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void equals() {
        ClientContainsKeywordsPredicate firstPredicate =
                new ClientContainsKeywordsPredicate(List.of("first"));
        ClientContainsKeywordsPredicate secondPredicate =
                new ClientContainsKeywordsPredicate(List.of("second"));

        FindCommand findFirstCommand = new FindCommand(firstPredicate);
        FindCommand findSecondCommand = new FindCommand(secondPredicate);

        // same object -> returns true
        assertTrue(findFirstCommand.equals(findFirstCommand));

        // same values -> returns true
        FindCommand findFirstCommandCopy = new FindCommand(firstPredicate);
        assertTrue(findFirstCommand.equals(findFirstCommandCopy));

        // different types -> returns false
        assertFalse(findFirstCommand.equals(1));

        // null -> returns false
        assertFalse(findFirstCommand.equals(null));

        // different person -> returns false
        assertFalse(findFirstCommand.equals(findSecondCommand));
    }

    @Test
    public void execute_zeroKeywords_noPersonFound() {
        String expectedMessage = "0 client(s) listed!";
        ClientContainsKeywordsPredicate predicate = preparePredicate(" ");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredPersonList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredPersonList());
    }

    @Test
    public void execute_multipleKeywords_multiplePersonsFound() {
        String expectedMessage = "3 client(s) listed!";
        ClientContainsKeywordsPredicate predicate = preparePredicate("Kurz Elle Kunz");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredPersonList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredPersonList());
    }

    @Test
    public void toStringMethod() {
        ClientContainsKeywordsPredicate predicate = new ClientContainsKeywordsPredicate(List.of("keyword"));
        FindCommand findCommand = new FindCommand(predicate);
        String expected = FindCommand.class.getCanonicalName() + "{predicate=" + predicate + "}";
        assertEquals(expected, findCommand.toString());
    }

    @Test
    public void execute_caseInsensitiveWholeWordOr_preservesInsertionOrderAndAvoidsDuplicates() {
        ClientContainsKeywordsPredicate predicate = preparePredicate("kUnZ ELLE kUrZ Elle");
        assertEquals("3 client(s) listed!", new FindCommand(predicate).execute(model).getFeedbackToUser());
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredPersonList());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_substringsQuotesPunctuationAndOtherContactFields_doNotMatch() {
        for (String keyword : List.of("Car", "\"Carl\"", "Carl,",
                CARL.getEmail().value, "heinz", "owesMoney")) {
            assertEquals("0 client(s) listed!",
                    new FindCommand(preparePredicate(keyword)).execute(model).getFeedbackToUser());
            assertTrue(model.getFilteredPersonList().isEmpty());
        }
    }

    @Test
    public void execute_fullPhoneAndRepeatedName_matchesClientOnce() {
        assertEquals("1 client(s) listed!", new FindCommand(preparePredicate(CARL.getPhone().value + " Carl "
                + CARL.getPhone().value)).execute(model).getFeedbackToUser());
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_afterSearchAndPendingMode_searchesAllClientsAndReconcilesSelection() {
        LocalDate today = LocalDate.of(2026, 10, 12);
        Person alice = new PersonBuilder().withName("Alice Lim").withPhone("123")
                .withEmail("alice@example.com").withoutFollowUp().build();
        Person bob = new PersonBuilder().withName("Bob Lim").withPhone("456")
                .withEmail("bob@example.com").withFollowUp(new FollowUp(today, "Call Bob")).build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(alice);
        addressBook.addPerson(bob);
        Model searchModel = new ModelManager(addressBook, new UserPrefs(), today, false);
        searchModel.updateFilteredPersonList(person -> person == bob);
        searchModel.selectPerson(bob);

        assertEquals("2 client(s) listed!",
                new FindCommand(preparePredicate("Lim")).execute(searchModel).getFeedbackToUser());
        assertEquals(List.of(alice, bob), searchModel.getFilteredPersonList());
        assertSame(bob, searchModel.selectedPersonProperty().get());

        searchModel.showPendingFollowUps();
        assertEquals(List.of(bob), searchModel.getFilteredPersonList());
        assertEquals("1 client(s) listed!",
                new FindCommand(preparePredicate("Alice")).execute(searchModel).getFeedbackToUser());
        assertEquals(List.of(alice), searchModel.getFilteredPersonList());
        assertFalse(searchModel.showingFollowUpsProperty().get());
        assertNull(searchModel.selectedPersonProperty().get());
        new FindCommand(preparePredicate("Lim")).execute(searchModel);
        assertNull(searchModel.selectedPersonProperty().get());
        assertEquals(List.of(alice, bob), searchModel.getAddressBook().getPersonList());
        assertEquals(today, searchModel.getToday());
        assertFalse(searchModel.hasUnsavedChanges());
    }

    /**
     * Parses {@code userInput} into a {@code ClientContainsKeywordsPredicate}.
     */
    private ClientContainsKeywordsPredicate preparePredicate(String userInput) {
        return new ClientContainsKeywordsPredicate(List.of(userInput.split("\\s+")));
    }
}
