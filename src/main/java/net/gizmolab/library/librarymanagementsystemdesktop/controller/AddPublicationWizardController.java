package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.picker.Cells;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AddPublicationFlow;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.CatalogService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PublicationDetailFormatter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Consumer;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Shared steps of the add wizards: preview of an existing publication, local form, copies, "Ολοκλήρωση".
 * The first page (ISBN or brochure search) belongs to the subclass. A publication is created only in
 * handleFinish (through AddPublicationFlow), so cancelling earlier creates nothing.
 */
public abstract class AddPublicationWizardController extends BaseController {

    protected enum Page { START, PREVIEW, FORM, COPIES }

    @Autowired protected CatalogService catalog;

    @FXML protected Node startPage;
    @FXML protected ScrollPane previewPage;
    @FXML protected Label previewNote;
    @FXML protected PublicationSummaryController summaryController;
    @FXML protected ScrollPane formPage;
    @FXML protected LocalPublicationFormController formController;
    @FXML protected VBox duplicatesBox;
    @FXML protected Label duplicatesLabel;
    @FXML protected ListView<PublicationDTO> duplicatesList;
    @FXML protected Button useExistingButton;
    @FXML protected VBox copiesPage;
    @FXML protected Label copiesTitle;
    @FXML protected CopiesInputController copiesController;
    @FXML protected Label copiesMessage;
    @FXML protected ProgressIndicator busyIndicator;
    @FXML protected Button backButton;
    @FXML protected Button nextButton;
    @FXML protected Button finishButton;
    @FXML protected Button cancelButton;

    private Page page = Page.START;
    private Page beforeCopies = Page.PREVIEW;
    private PublicationDTO existing;
    private PublicationDraft draft;
    private AddPublicationFlow flow;
    private AddPublicationFlow.Outcome partial;
    private boolean busy;

    /** Text above the candidates when the server reports that the publication already exists. */
    protected abstract String duplicatesMessage();

    protected static void openWizard(FXMLLoaderFactory factory, Window owner, String fxml, String title) throws Exception {
        openWizard(factory, owner, fxml, title, controller -> {});
    }

    /** beforeShow runs after the FXML is loaded and before the window opens (e.g. a preselected magazine). */
    protected static <C extends AddPublicationWizardController> void openWizard(
            FXMLLoaderFactory factory, Window owner, String fxml, String title, Consumer<C> beforeShow) throws Exception {
        var loaded = factory.<Parent, C>loadWithController(fxml);
        C controller = loaded.getController();
        beforeShow.accept(controller);
        AddPublicationWizardController wizard = controller;
        Stage stage = new Stage();
        stage.setOnCloseRequest(e -> {
            if (wizard.busy) e.consume(); // "Ολοκλήρωση" is running: closing now would hide its outcome
        });
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle(title);
        Scene scene = new Scene(loaded.getRoot(), 860, 740);
        StylesheetHelper.applyModernTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    /** The subclass's first page has an inner step to go back to (e.g. issue list → magazine search). */
    protected boolean canGoBackOnStart() {
        return false;
    }

    /** "Πίσω" on the first page when canGoBackOnStart(); the subclass calls show(Page.START) afterwards. */
    protected void backOnStart() {
    }

    @FXML
    protected void initialize() {
        flow = new AddPublicationFlow(catalog, AuthService.getCurrentLibraryId());
        duplicatesList.setCellFactory(Cells.text(AddPublicationWizardController::describe));
        duplicatesList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, pub) -> useExistingButton.setDisable(pub == null));
        busyIndicator.setVisible(false);
        show(Page.START);
    }

    /** "Title — Authors (Year)". */
    static String describe(PublicationDTO pub) {
        String authors = pub.getAuthorNames().isEmpty() ? "" : " — " + pub.getAuthorNames();
        String year = pub.getYearPublished() == null ? "" : " (" + pub.getYearPublished() + ")";
        return PublicationDetailFormatter.displayTitle(pub) + authors + year;
    }

    /** An existing publication (catalog, Biblionet import, chosen duplicate): read-only preview, then copies. */
    protected void showPreview(PublicationDTO pub, String sourceNote) {
        existing = pub;
        draft = null;
        summaryController.show(pub);
        boolean inLibrary = pub.getTotalCopies() > 0;
        previewNote.setText(inLibrary
                ? "Υπάρχει ήδη στη βιβλιοθήκη σας (" + pub.getTotalCopies() + " αντίτυπα). Μπορείτε να προσθέσετε επιπλέον αντίτυπα."
                : sourceNote);
        nextButton.setText(inLibrary ? "Προσθήκη επιπλέον αντιτύπων" : i18nManager.getMessage("common.next"));
        show(Page.PREVIEW);
    }

    /** The local cataloguing form (the subclass has already called formController.setBook/setBrochure). */
    protected void showForm() {
        existing = null;
        hideDuplicates();
        nextButton.setText(i18nManager.getMessage("common.next"));
        show(Page.FORM);
        formPage.setVvalue(0); // duplicates and errors are at the top of the form
    }

    protected void show(Page next) {
        page = next;
        setPage(startPage, next == Page.START);
        setPage(previewPage, next == Page.PREVIEW);
        setPage(formPage, next == Page.FORM);
        setPage(copiesPage, next == Page.COPIES);
        boolean onCopies = next == Page.COPIES;
        backButton.setDisable((next == Page.START && !canGoBackOnStart()) || partial != null);
        nextButton.setVisible(!onCopies);
        nextButton.setManaged(!onCopies);
        nextButton.setDisable(next == Page.START);
        finishButton.setVisible(onCopies);
        finishButton.setManaged(onCopies);
    }

    private static void setPage(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    @FXML
    protected void handleNext() {
        if (page == Page.PREVIEW) {
            beforeCopies = Page.PREVIEW;
            openCopies(PublicationDetailFormatter.displayTitle(existing));
        } else if (page == Page.FORM) {
            Optional<PublicationDraft> built = formController.buildDraft();
            if (built.isEmpty()) {
                formPage.setVvalue(0); // the errors are shown at the top of the form
                return;
            }
            draft = built.get();
            beforeCopies = Page.FORM;
            openCopies(draftTitle(draft));
        }
    }

    private static String draftTitle(PublicationDraft d) {
        String label = PublicationDraft.PERIODICAL.equals(d.getType())
                ? PublicationDetailFormatter.issueLabel(d.getIssueNumber(), d.getPeriod()) : "";
        return label.isEmpty() ? d.getTitle().trim() : d.getTitle().trim() + " — " + label;
    }

    private void openCopies(String title) {
        copiesTitle.setText("Αντίτυπα για: " + title);
        copiesMessage.setText("");
        finishButton.setText(i18nManager.getMessage("wizard.finish"));
        show(Page.COPIES);
    }

    @FXML
    protected void handleBack() {
        if (page == Page.START) {
            backOnStart();
            return;
        }
        if (page == Page.COPIES) {
            show(beforeCopies);
        } else {
            existing = null;
            draft = null;
            nextButton.setText(i18nManager.getMessage("common.next"));
            show(Page.START);
        }
    }

    @FXML
    protected void handleFinish() {
        AddPublicationFlow.Outcome toRetry = partial;
        OptionalInt typed = copiesController.getCount();
        if (toRetry == null && typed.isEmpty()) {
            copiesMessage.setText("Το πλήθος αντιτύπων πρέπει να είναι αριθμός από 1 έως 50.");
            return;
        }
        int count = typed.orElse(0);
        String condition = copiesController.getCondition();
        PublicationDTO toCopy = existing;
        PublicationDraft toCreate = draft;
        setBusy(true);
        BackgroundTasks.run(() -> {
            if (toRetry != null) return flow.retryCopies(toRetry, condition);
            if (toCopy != null) return flow.addCopies(toCopy, count, condition);
            return flow.createAndAddCopies(toCreate, count, condition);
        }, this::onOutcome, error -> {
            setBusy(false);
            copiesMessage.setText(UserMessages.describe(error));
        });
    }

    private void onOutcome(AddPublicationFlow.Outcome outcome) {
        setBusy(false);
        switch (outcome.kind()) {
            case DONE -> {
                if (outcome.foundElsewhere()) {
                    alertManager.showInfo(i18nManager.getMessage("info.title"),
                            "Το βιβλίο υπήρχε ήδη (στη Biblionet ή στον κατάλογο)· χρησιμοποιήθηκαν τα στοιχεία του και προστέθηκαν τα αντίτυπα.");
                }
                close();
            }
            case DUPLICATES -> {
                showForm();
                showDuplicates(outcome.duplicates());
            }
            case PARTIAL_COPIES -> {
                partial = outcome;
                copiesController.setLocked(true);
                copiesMessage.setText("Δημιουργήθηκαν " + outcome.copies().created() + " από "
                        + outcome.copies().requested() + " αντίτυπα. " + outcome.copies().errorMessage());
                finishButton.setText(i18nManager.getMessage("wizard.retry"));
                backButton.setDisable(true);
            }
        }
    }

    private void showDuplicates(List<PublicationDTO> duplicates) {
        duplicatesLabel.setText(duplicatesMessage());
        duplicatesList.getItems().setAll(duplicates);
        if (!duplicates.isEmpty()) duplicatesList.getSelectionModel().selectFirst();
        duplicatesBox.setVisible(true);
        duplicatesBox.setManaged(true);
    }

    private void hideDuplicates() {
        duplicatesBox.setVisible(false);
        duplicatesBox.setManaged(false);
    }

    @FXML
    protected void handleUseExisting() {
        PublicationDTO chosen = duplicatesList.getSelectionModel().getSelectedItem();
        if (chosen == null) return;
        hideDuplicates();
        showPreview(chosen, "Υπάρχει ήδη στον κατάλογο του δικτύου.");
    }

    @FXML
    protected void handleCancel() {
        close();
    }

    protected void setBusy(boolean busy) {
        this.busy = busy;
        busyIndicator.setVisible(busy);
        cancelButton.setDisable(busy);
        finishButton.setDisable(busy);
        nextButton.setDisable(busy || page == Page.START);
        backButton.setDisable(busy || (page == Page.START && !canGoBackOnStart()) || partial != null);
    }

    private void close() {
        ((Stage) finishButton.getScene().getWindow()).close();
    }
}
