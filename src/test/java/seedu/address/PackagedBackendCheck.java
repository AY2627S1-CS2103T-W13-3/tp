package seedu.address;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

import seedu.address.logic.LogicManager;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.ui.DisplayMatrixFixture;

/** Self-checking backend acceptance run against the packaged JAR, separate from rendering timings. */
public class PackagedBackendCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: packagedBackendCheck --args='<fresh-folder>'");
        }
        if (!PackagedBackendCheck.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Run this check with assertions enabled.");
        }
        Path source = Path.of(LogicManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!source.toString().endsWith(".jar")) {
            throw new IllegalStateException("Production classes must come from the packaged JAR.");
        }
        System.out.println("Production classes: " + source);
        LocalDate today = LocalDate.now();
        DisplayMatrixFixture.main(new String[] {args[0], today.toString()});
        Path data = Path.of(args[0], "data", "addressbook.json");
        Storage storage = new StorageManager(new JsonAddressBookStorage(data),
                new JsonUserPrefsStorage(Path.of(args[0], "preferences.json")));
        long start = System.nanoTime();
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        System.out.printf("Load and model: %.3f ms%n", (System.nanoTime() - start) / 1_000_000.0);
        assert !model.isDataLoadingBlocked();
        assert model.getAddressBook().getPersonList().size() == 1000;
        LogicManager logic = new LogicManager(model, storage);
        execute(logic, "find Client");
        assert logic.getFilteredPersonList().size() == 999;
        execute(logic, "view " + idNamed(model, "Client 1000"));
        assert logic.selectedPersonProperty().get().getName().fullName.equals("Client 1000");
        execute(logic, "list");
        execute(logic, "view " + model.getAddressBook().getClientId(
                model.getAddressBook().getPersonList().getFirst()).orElseThrow());
        assert logic.selectedPersonProperty().get().getEmail().value.length() == 125;
        byte[] beforeError = Files.readAllBytes(data);
        try {
            logic.execute("view 0");
            throw new AssertionError("Invalid index was accepted.");
        } catch (ParseException expected) {
            assert Arrays.equals(beforeError, Files.readAllBytes(data));
        }
        execute(logic, "followups");
        assert logic.getFilteredPersonList().size() == 101;
        int reorderedId = model.getAddressBook().getClientId(
                logic.getFilteredPersonList().getFirst()).orElseThrow().getValue();
        execute(logic, "followup " + reorderedId + " d/" + today.plusDays(90) + " m/Reordered action");
        assert logic.getFilteredPersonList().getLast().getName().fullName.equals("Client 10");
        execute(logic, "followup " + reorderedId + " clear");
        assert logic.getFilteredPersonList().size() == 100;
        execute(logic, "delete " + model.getAddressBook().getClientId(
                logic.getFilteredPersonList().getFirst()).orElseThrow());
        assert model.getAddressBook().getPersonList().size() == 999;
        Model restarted = new MainApp().initModelManager(storage, new UserPrefs());
        assert !restarted.isDataLoadingBlocked();
        assert restarted.selectedPersonProperty().get() == null;
        assert restarted.getAddressBook().equals(model.getAddressBook());
        System.out.println("PASS: filter/permanent-ID, long profile, rejected input, "
                + "reorder, clear, delete and reload.");
        System.out.println("Backend timings only; input, rendering, scrolling and OS scaling remain separate.");
    }

    private static int idNamed(Model model, String name) {
        var person = model.getAddressBook().getPersonList().stream()
                .filter(client -> client.getName().fullName.equals(name)).findFirst().orElseThrow();
        return model.getAddressBook().getClientId(person).orElseThrow().getValue();
    }

    private static void execute(LogicManager logic, String command) throws Exception {
        long start = System.nanoTime();
        logic.execute(command);
        System.out.printf("%s: %.3f ms%n", command, (System.nanoTime() - start) / 1_000_000.0);
    }
}
