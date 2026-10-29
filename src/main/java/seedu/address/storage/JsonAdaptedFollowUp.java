package seedu.address.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.person.FollowUp;

/** Jackson-friendly representation of one pending follow-up. */
class JsonAdaptedFollowUp {
    private final String dueDate;
    private final String description;

    @JsonCreator
    public JsonAdaptedFollowUp(
            @JsonProperty("dueDate") @JsonDeserialize(using = StoredTextDeserializer.class) String dueDate,
            @JsonProperty("description") @JsonDeserialize(using = StoredTextDeserializer.class) String description) {
        this.dueDate = dueDate;
        this.description = description;
    }

    public JsonAdaptedFollowUp(FollowUp source) {
        dueDate = source.getDueDate().toString();
        description = source.getDescription();
    }

    /** Converts stored values to a validated domain value. */
    public FollowUp toModelType() throws IllegalValueException {
        if (dueDate == null || description == null) {
            throw new IllegalValueException("A follow-up must have both a due date and a description.");
        }
        try {
            return new FollowUp(FollowUp.parseDate(dueDate), description);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage(), e);
        }
    }
}
