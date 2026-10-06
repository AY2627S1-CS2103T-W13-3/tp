package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Represents a Person in the address book.
 * Guarantees: details are present and not null, field values are validated, immutable.
 */
public class Person {

    // Identity fields
    private final Name name;
    private final Phone phone;
    private final Email email;

    // Data fields
    private final Address address;
    private final Optional<FollowUp> followUp;
    private final Set<Tag> tags = new HashSet<>();

    /**
     * Constructs a person without a pending follow-up.
     * Every field must be present and not null.
     */
    public Person(Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        this(name, phone, email, address, tags, Optional.empty());
    }

    /**
     * Constructs a person with an optional pending follow-up.
     * Every argument, including the optional container, must be non-null.
     */
    public Person(Name name, Phone phone, Email email, Address address, Set<Tag> tags, Optional<FollowUp> followUp) {
        requireAllNonNull(name, phone, email, address, tags, followUp);
        this.followUp = followUp;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.tags.addAll(tags);
    }

    public Optional<FollowUp> getFollowUp() {
        return followUp;
    }

    /**
     * Returns a copy with the supplied follow-up, preserving all contact details and tags.
     */
    public Person withFollowUp(FollowUp followUp) {
        return new Person(name, phone, email, address, tags, Optional.of(followUp));
    }

    /**
     * Returns a copy without a follow-up, preserving all contact details and tags.
     */
    public Person withoutFollowUp() {
        return new Person(name, phone, email, address, tags, Optional.empty());
    }

    public Name getName() {
        return name;
    }

    public Phone getPhone() {
        return phone;
    }

    public Email getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if both persons have the same name.
     * This defines a weaker notion of equality between two persons.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && otherPerson.getName().equals(getName());
    }

    /**
     * Returns true if both persons have the same identity and data fields.
     * This defines a stronger notion of equality between two persons.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Person otherPerson)) {
            return false;
        }

        return name.equals(otherPerson.name)
                && phone.equals(otherPerson.phone)
                && email.equals(otherPerson.email)
                && address.equals(otherPerson.address)
                && tags.equals(otherPerson.tags)
                && followUp.equals(otherPerson.followUp);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, address, tags, followUp);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("address", address)
                .add("tags", tags);
        followUp.ifPresent(value -> builder.add("followUp", value));
        return builder.toString();
    }

}
