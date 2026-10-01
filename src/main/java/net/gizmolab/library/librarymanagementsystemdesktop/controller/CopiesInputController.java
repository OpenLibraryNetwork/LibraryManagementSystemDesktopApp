package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.util.CopyCount;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.OptionalInt;

/** Number (1–50) and condition of the copies to add; numbers are assigned automatically. */
@Controller
@Scope("prototype")
public class CopiesInputController {

    @FXML private Spinner<Integer> countSpinner;
    @FXML private ComboBox<String> conditionCombo;

    @FXML
    private void initialize() {
        var factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(CopyCount.MIN, CopyCount.MAX, 1);
        // Invalid or empty text falls back to the last valid number instead of breaking the spinner
        factory.setConverter(new StringConverter<>() {
            @Override public String toString(Integer value) { return value == null ? "" : value.toString(); }
            @Override public Integer fromString(String text) {
                return CopyCount.parse(text).stream().boxed().findFirst().orElse(factory.getValue());
            }
        });
        countSpinner.setValueFactory(factory);
        countSpinner.getEditor().setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,2}") ? change : null));
        conditionCombo.getItems().setAll("NEW", "GOOD", "FAIR", "POOR");
        conditionCombo.setValue("NEW");
    }

    /** The typed number of copies, or empty when it is not a whole number from 1 to 50. */
    public OptionalInt getCount() {
        return CopyCount.parse(countSpinner.getEditor().getText());
    }

    public String getCondition() {
        return conditionCombo.getValue();
    }

    /** After a partial failure the retry creates only the missing copies with the same condition. */
    public void setLocked(boolean locked) {
        countSpinner.setDisable(locked);
        conditionCombo.setDisable(locked);
    }
}
