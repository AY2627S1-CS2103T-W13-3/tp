package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.text.Normalizer;
import java.util.function.Predicate;

import seedu.address.commons.util.ToStringBuilder;

/** Matches a client using one explicit name, phone, or tag search. */
public class ClientSearchPredicate implements Predicate<Person> {
    /** Identifies the contact field used by an explicit client search. */
    public enum Field {
        NAME, PHONE, TAG
    }

    private final Field field;
    private final String query;

    public ClientSearchPredicate(Field field, String query) {
        requireNonNull(field);
        requireNonNull(query);
        this.field = field;
        this.query = query;
    }

    @Override
    public boolean test(Person person) {
        requireNonNull(person);
        return switch (field) {
        case NAME -> containsNameIgnoreCase(person.getName().fullName, query);
        case PHONE -> phoneDigits(person.getPhone().value).contains(phoneDigits(query));
        case TAG -> person.getTags().stream().anyMatch(tag -> tag.tagName.equalsIgnoreCase(query));
        };
    }

    private static boolean containsNameIgnoreCase(String name, String query) {
        String normalizedName = normalizeName(name);
        String normalizedQuery = normalizeName(query);
        for (int start = 0; start + normalizedQuery.length() <= normalizedName.length(); start++) {
            if (normalizedName.regionMatches(true, start, normalizedQuery, 0, normalizedQuery.length())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        StringBuilder collapsed = new StringBuilder(normalized.length());
        boolean previousWasSpace = false;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (character == ' ') {
                if (!previousWasSpace) {
                    collapsed.append(character);
                }
                previousWasSpace = true;
            } else {
                collapsed.append(character);
                previousWasSpace = false;
            }
        }
        return collapsed.toString();
    }

    private static String phoneDigits(String value) {
        return value.replace("+", "").replace(" ", "").replace("-", "");
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ClientSearchPredicate otherPredicate)) {
            return false;
        }
        return field == otherPredicate.field && query.equals(otherPredicate.query);
    }

    @Override
    public int hashCode() {
        return 31 * field.hashCode() + query.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("field", field).add("query", query).toString();
    }
}
