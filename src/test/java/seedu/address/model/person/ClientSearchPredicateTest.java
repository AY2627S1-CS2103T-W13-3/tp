package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.ClientSearchPredicate.Field;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

class ClientSearchPredicateTest {
    @Test
    void test_nameSubstring_isUnicodeCaselessAndCollapsesOrdinarySpaces() {
        Person person = new PersonBuilder().withName("  RACHEL   Lim").build();
        assertTrue(new ClientSearchPredicate(Field.NAME, "rachel lim").test(person));
        assertTrue(new ClientSearchPredicate(Field.NAME, "lim").test(person));
        assertFalse(new ClientSearchPredicate(Field.NAME, "ache").test(person));

        Person composed = new PersonBuilder().withName("José Lim").build();
        assertTrue(new ClientSearchPredicate(Field.NAME, "Jose\u0301").test(composed));

        Person greek = new PersonBuilder().withName("ΟΣ Λάμ").build();
        assertTrue(new ClientSearchPredicate(Field.NAME, "ος").test(greek));

        Person dottedI = new PersonBuilder().withName("İpek Tan").build();
        assertTrue(new ClientSearchPredicate(Field.NAME, "ipek").test(dottedI));

        Person spaced = new PersonBuilder().withName("Long   Internal    Spaces").build();
        assertTrue(new ClientSearchPredicate(Field.NAME, "internal spaces").test(spaced));
    }

    @Test
    void test_phoneSubstring_ignoresDisplayFormattingAndRetainsZeroes() {
        Person person = new PersonBuilder().withPhone("+65 091-234").build();
        assertTrue(new ClientSearchPredicate(Field.PHONE, "0912").test(person));
        assertTrue(new ClientSearchPredicate(Field.PHONE, "+65 091").test(person));
        assertFalse(new ClientSearchPredicate(Field.PHONE, "923").test(person));
    }

    @Test
    void test_tag_matchesExactTagIgnoringCase() {
        Person person = new PersonBuilder().withTags("renewal", "urgent").build();
        assertTrue(new ClientSearchPredicate(Field.TAG, "ReNeWaL").test(person));
        assertFalse(new ClientSearchPredicate(Field.TAG, "new").test(person));
        assertFalse(new ClientSearchPredicate(Field.TAG, "unknown").test(person));
    }
}
