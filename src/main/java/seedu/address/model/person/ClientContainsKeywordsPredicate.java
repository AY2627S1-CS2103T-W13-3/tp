package seedu.address.model.person;

import java.util.List;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/** Matches any whole name word or exact full phone number, retaining phone leading zeroes. */
public class ClientContainsKeywordsPredicate implements Predicate<Person> {
    private final List<String> keywords;
    private final NameContainsKeywordsPredicate namePredicate;
    private final List<Phone> phoneKeywords;

    /** Creates a search predicate from an immutable snapshot of the supplied keywords. */
    public ClientContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = List.copyOf(keywords);
        namePredicate = new NameContainsKeywordsPredicate(this.keywords);
        phoneKeywords = this.keywords.stream()
                .filter(keyword -> keyword.matches("[0-9]{3,}"))
                .map(Phone::new)
                .toList();
    }

    @Override
    public boolean test(Person person) {
        return namePredicate.test(person)
                || phoneKeywords.stream().anyMatch(person.getPhone()::hasSameDigitsAs);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ClientContainsKeywordsPredicate otherPredicate)) {
            return false;
        }
        return keywords.equals(otherPredicate.keywords);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}
