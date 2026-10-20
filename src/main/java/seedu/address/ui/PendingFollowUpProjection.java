package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.model.person.Person;

/**
 * Maintains an unnumbered, read-only summary of pending actions from a full client feed.
 * Records retain their source identity. Due-date ties retain source order, independently of any
 * filtered command list. Status labels remain the renderer's responsibility using the published date.
 * Use this projection on the same thread as its source; no window, clock or storage is accessed.
 */
public final class PendingFollowUpProjection implements AutoCloseable {
    private static final Comparator<Person> BY_DUE_DATE =
            Comparator.comparing(person -> person.getFollowUp().orElseThrow().getDueDate());

    private final ObservableList<Person> allPersons;
    private final ObservableList<Person> pendingPersons = FXCollections.observableArrayList();
    private final ObservableList<Person> readOnlyPendingPersons =
            FXCollections.unmodifiableObservableList(pendingPersons);
    private final ListChangeListener<Person> sourceListener = change -> rebuild();
    private boolean isClosed;

    /**
     * Builds a live projection of a full feed containing only non-null immutable client records.
     * The caller must keep the source free of null records throughout its lifetime.
     *
     * @throws NullPointerException if the source or an initial record is null.
     */
    public PendingFollowUpProjection(ObservableList<Person> allPersons) {
        this.allPersons = requireNonNull(allPersons);
        rebuild();
        allPersons.addListener(sourceListener);
    }

    /** Returns the stable, unmodifiable list of pending clients in due-date order. */
    public ObservableList<Person> getPendingPersons() {
        return readOnlyPendingPersons;
    }

    /** Detaches the source listener once. The exposed list retains its last snapshot after closing. */
    @Override
    public void close() {
        if (!isClosed) {
            allPersons.removeListener(sourceListener);
            isClosed = true;
        }
    }

    private void rebuild() {
        List<Person> pending = new ArrayList<>();
        for (Person person : allPersons) {
            requireNonNull(person, "The full client feed must not contain null records.");
            if (person.getFollowUp().isPresent()) {
                pending.add(person);
            }
        }
        // List.sort is stable, preserving full-feed insertion order for equal due dates.
        pending.sort(BY_DUE_DATE);
        pendingPersons.setAll(pending);
        assert pendingPersons.stream().allMatch(person -> person.getFollowUp().isPresent())
                : "Every summary client must have a pending action.";
    }
}
