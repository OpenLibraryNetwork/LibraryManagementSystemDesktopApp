package net.gizmolab.library.librarymanagementsystemdesktop.controller.picker;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.SubjectDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.CatalogService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.SubjectFilter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.*;

/** Optional DDC subjects: only existing ones (librarians never create subjects). */
@Controller
@Scope("prototype")
public class SubjectPickerController extends BaseController {

    @Autowired private CatalogService catalog;

    @FXML private TextField filterField;
    @FXML private ListView<SubjectDTO> subjectsList;
    @FXML private Label countLabel;

    private List<SubjectDTO> all = List.of();
    private final Map<Long, BooleanProperty> checked = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        subjectsList.setCellFactory(CheckBoxListCell.forListView(this::checkedProperty, new StringConverter<>() {
            @Override public String toString(SubjectDTO s) { return s == null ? "" : SubjectFilter.label(s); }
            @Override public SubjectDTO fromString(String text) { return null; }
        }));
        filterField.textProperty().addListener((obs, old, text) ->
                subjectsList.getItems().setAll(SubjectFilter.filter(all, text)));
        updateCount();
        BackgroundTasks.run(catalog::getSubjects, subjects -> {
            all = subjects;
            subjectsList.getItems().setAll(SubjectFilter.filter(all, filterField.getText()));
        }, error -> countLabel.setText(UserMessages.describe(error)));
    }

    private BooleanProperty checkedProperty(SubjectDTO subject) {
        return checked.computeIfAbsent(subject.getId(), id -> {
            BooleanProperty property = new SimpleBooleanProperty(false);
            property.addListener((obs, was, is) -> updateCount());
            return property;
        });
    }

    private void updateCount() {
        long count = checked.values().stream().filter(BooleanProperty::get).count();
        countLabel.setText(count == 0 ? "Κανένα θέμα (προαιρετικό)" : "Επιλεγμένα θέματα: " + count);
    }

    /** No subjects checked and no filter (the form is reused for another publication). */
    public void reset() {
        checked.values().forEach(property -> property.set(false));
        filterField.clear();
    }

    public List<Long> getSelectedSubjectIds() {
        List<Long> ids = new ArrayList<>();
        checked.forEach((id, property) -> { if (property.get()) ids.add(id); });
        return ids;
    }
}
