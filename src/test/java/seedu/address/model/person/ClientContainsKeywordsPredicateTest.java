package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class ClientContainsKeywordsPredicateTest {
    @Test
    public void test_nameWords_preservesCaseInsensitiveWholeWordOrMatching() {
        Person person = new PersonBuilder().withName("Alice Baker").build();
        assertTrue(new ClientContainsKeywordsPredicate(List.of("aLiCe", "Missing")).test(person));
        assertTrue(new ClientContainsKeywordsPredicate(List.of("Baker")).test(person));
        for (String keyword : List.of("Ali", "Baker,", "\"Alice\"", "Alice\u00a0Baker")) {
            assertFalse(new ClientContainsKeywordsPredicate(List.of(keyword)).test(person));
        }
    }

    @Test
    public void test_fullPhone_preservesLeadingZeroesAndRejectsPartialNumbers() {
        Person person = new PersonBuilder().withName("Alice Baker").withPhone("00123456").build();
        assertTrue(new ClientContainsKeywordsPredicate(List.of("00123456")).test(person));
        assertTrue(new ClientContainsKeywordsPredicate(List.of("Missing", "00123456")).test(person));
        for (String keyword : List.of("001", "123456", "000123456", "+00123456", "0012-3456")) {
            assertFalse(new ClientContainsKeywordsPredicate(List.of(keyword)).test(person));
        }
        assertFalse(new ClientContainsKeywordsPredicate(List.of("0012", "3456")).test(person));
    }

    @Test
    public void test_numericNameWord_retainsNameMatchingWhenPhoneDiffers() {
        Person person = new PersonBuilder().withName("Agent 12345").withPhone("98765").build();
        assertTrue(new ClientContainsKeywordsPredicate(List.of("12345")).test(person));
        assertFalse(new ClientContainsKeywordsPredicate(List.of("1234")).test(person));
    }

    @Test
    public void test_emptyKeywordsOrOtherFields_doNotMatch() {
        Person person = new PersonBuilder().withName("Alice Baker").withPhone("81234562")
                .withEmail("alice@example.com").withAddress("Main Street").withTags("health").build();
        assertFalse(new ClientContainsKeywordsPredicate(List.of()).test(person));
        assertFalse(new ClientContainsKeywordsPredicate(List.of("alice@example.com", "Main", "health"))
                .test(person));
    }

    @Test
    public void constructor_callerChangesKeywords_keepsOriginalNameAndPhoneMatching() {
        List<String> keywords = new ArrayList<>(List.of("Alice", "81234562"));
        ClientContainsKeywordsPredicate predicate = new ClientContainsKeywordsPredicate(keywords);
        keywords.clear();
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Baker").withPhone("999").build()));
        assertTrue(predicate.test(new PersonBuilder().withName("Bob Baker").withPhone("81234562").build()));
        assertEquals(new ClientContainsKeywordsPredicate(List.of("Alice", "81234562")), predicate);
    }

    @Test
    public void equalsAndToString_useKeywordValues() {
        ClientContainsKeywordsPredicate predicate = new ClientContainsKeywordsPredicate(List.of("Bob", "81234562"));
        assertEquals(predicate, predicate);
        assertEquals(new ClientContainsKeywordsPredicate(List.of("Bob", "81234562")), predicate);
        assertNotEquals(new ClientContainsKeywordsPredicate(List.of("Bob", "81234563")), predicate);
        assertNotEquals(new NameContainsKeywordsPredicate(List.of("Bob", "81234562")), predicate);
        assertNotEquals(null, predicate);
        assertEquals(ClientContainsKeywordsPredicate.class.getCanonicalName()
                + "{keywords=[Bob, 81234562]}", predicate.toString());
    }
}
