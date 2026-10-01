package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.picker.ContributorPickerController;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.picker.PublisherPickerController;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.picker.SubjectPickerController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.MagazineDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;

/** Local cataloguing form, shared by "ISBN not in Biblionet", "new brochure" and "new issue". */
@Controller
@Scope("prototype")
public class LocalPublicationFormController extends BaseController {

    @FXML private Label messageLabel;
    @FXML private Label isbnCaption;
    @FXML private Label isbnValue;
    @FXML private TextField titleField;
    @FXML private TextField subtitleField;
    @FXML private TextField yearField;
    @FXML private TextField pagesField;
    @FXML private TextField languageField;
    @FXML private TextArea summaryArea;
    @FXML private TextField originalLanguageField;
    @FXML private TextField editionField;
    @FXML private TextField seriesField;
    @FXML private TextField placeField;
    @FXML private Label errorLabel;
    @FXML private ContributorPickerController contributorsController;
    @FXML private PublisherPickerController publisherController;
    @FXML private SubjectPickerController subjectsController;
    @FXML private GridPane issueGrid;
    @FXML private Label magazineValue;
    @FXML private TextField issueNumberField;
    @FXML private TextField periodField;
    @FXML private Label titleCaption;
    @FXML private Label subtitleCaption;
    @FXML private VBox publisherBox;
    @FXML private TitledPane morePane;

    private String type;
    private String isbn;
    private String forWhat; // the ISBN, brochure search or magazine the typed data belongs to
    private MagazineDTO magazine;

    /** ISBN not found anywhere: the ISBN is fixed, the rest is typed by the librarian. */
    public void setBook(String isbn) {
        startFor("isbn:" + isbn, null);
        this.type = PublicationDraft.BOOK;
        this.isbn = isbn;
        this.magazine = null;
        isbnValue.setText(isbn);
        applyLayout(true, false);
        showMessage("Το ISBN δεν βρέθηκε στη Biblionet. Η εγγραφή θα ελεγχθεί από τον καταλογογράφο.");
    }

    public void setBrochure(String initialTitle) {
        String title = initialTitle == null ? "" : initialTitle.trim();
        startFor("brochure:" + title, title);
        this.type = PublicationDraft.BROCHURE;
        this.isbn = null;
        this.magazine = null;
        applyLayout(false, false);
        showMessage("Νέα μπροσούρα. Η εγγραφή θα ελεγχθεί από τον καταλογογράφο.");
    }

    /** A new issue of this magazine: its title is the magazine's, its publisher too (set by the server). */
    public void setIssue(MagazineDTO magazine) {
        startFor("issue:" + magazine.getDocumentId(), magazine.getTitle());
        this.type = PublicationDraft.PERIODICAL;
        this.isbn = null;
        this.magazine = magazine;
        magazineValue.setText(magazine.getDisplayName());
        applyLayout(false, true);
        showMessage("Νέο τεύχος. Η εγγραφή θα ελεγχθεί από τον καταλογογράφο.");
    }

    private void applyLayout(boolean book, boolean issue) {
        shown(isbnCaption, book);
        shown(isbnValue, book);
        shown(issueGrid, issue);
        shown(titleCaption, !issue);
        shown(titleField, !issue);
        shown(publisherBox, !issue);
        shown(morePane, !issue);
        subtitleCaption.setText(i18nManager.getMessage(issue ? "form.issueTheme" : "form.subtitle"));
    }

    private static void shown(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** The draft, or empty (with the errors shown) when it does not validate. */
    public Optional<PublicationDraft> buildDraft() {
        PublicationDraft draft = new PublicationDraft();
        draft.setType(type);
        draft.setIsbn(isbn);
        draft.setTitle(titleField.getText());
        draft.setSubtitle(subtitleField.getText());
        draft.setYearText(yearField.getText());
        draft.setPagesText(pagesField.getText());
        draft.setLanguage(languageField.getText());
        draft.setSummary(summaryArea.getText());
        draft.setOriginalLanguage(originalLanguageField.getText());
        draft.setEdition(editionField.getText());
        draft.setSeries(seriesField.getText());
        draft.setPlace(placeField.getText());
        PublisherDTO publisher = publisherController.getPublisher();
        draft.setPublisherId(publisher != null ? publisher.getDocumentId() : null);
        draft.setContributors(contributorsController.getContributors());
        draft.setSubjectIds(subjectsController.getSelectedSubjectIds());
        if (PublicationDraft.PERIODICAL.equals(type)) {
            draft.setMagazineId(magazine.getDocumentId());
            draft.setIssueNumber(issueNumberField.getText());
            draft.setPeriod(periodField.getText());
            draft.setPublisherId(null); // the magazine's publisher (server side)
        }

        Map<String, String> errors = draft.validate();
        if (errors.isEmpty()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            return Optional.of(draft);
        }
        errorLabel.setText(String.join("\n", errors.values()));
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        return Optional.empty();
    }

    /**
     * Coming back for the same ISBN or brochure search keeps what was typed; anything else starts
     * an empty form, so a previous publication's contributors or subjects never carry over.
     */
    private void startFor(String key, String initialTitle) {
        if (key.equals(forWhat)) return;
        forWhat = key;
        for (TextField field : new TextField[]{titleField, subtitleField, yearField, pagesField, languageField,
                originalLanguageField, editionField, seriesField, placeField, issueNumberField, periodField}) {
            field.clear();
        }
        summaryArea.clear();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        contributorsController.reset();
        publisherController.reset();
        subjectsController.reset();
        if (initialTitle != null) titleField.setText(initialTitle);
    }

    public void showMessage(String text) {
        messageLabel.setText(text);
    }
}
