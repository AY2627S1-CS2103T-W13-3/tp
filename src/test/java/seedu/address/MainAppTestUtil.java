package seedu.address;

import seedu.address.model.Model;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.storage.Storage;

/** Test-only access to startup without reflection or widening the production API. */
public final class MainAppTestUtil {
    private MainAppTestUtil() {
    }

    /** Loads the initial model using the real application's saved-data protection. */
    public static Model loadModel(Storage storage, ReadOnlyUserPrefs userPrefs) {
        return new MainApp().initModelManager(storage, userPrefs);
    }
}
