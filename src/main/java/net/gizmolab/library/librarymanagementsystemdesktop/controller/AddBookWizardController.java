package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/** "Προσθήκη βιβλίου": ISBN → catalog/Biblionet preview or local form → copies. */
@Controller
@Scope("prototype")
public class AddBookWizardController extends AddPublicationWizardController {

    @FXML private TextField isbnField;
    @FXML private Button searchButton;
    @FXML private Label isbnMessage;

    public static void open(FXMLLoaderFactory factory, Window owner) throws Exception {
        openWizard(factory, owner, "/fxml/add-book-wizard.fxml", "Προσθήκη βιβλίου");
    }

    @Override
    protected String duplicatesMessage() {
        return "Το ISBN υπάρχει ήδη στον κατάλογο. Χρησιμοποιήστε την υπάρχουσα εγγραφή:";
    }

    @FXML
    private void handleSearch() {
        String isbn = isbnField.getText() == null ? "" : isbnField.getText().trim();
        if (isbn.isEmpty()) {
            isbnMessage.setText("Πληκτρολογήστε ISBN.");
            return;
        }
        isbnMessage.setText("");
        searchButton.setDisable(true);
        busyIndicator.setVisible(true);
        BackgroundTasks.run(() -> catalog.lookupIsbn(isbn), result -> {
            searchButton.setDisable(false);
            busyIndicator.setVisible(false);
            switch (result.source()) {
                case NOT_FOUND -> {
                    formController.setBook(isbn);
                    showForm();
                }
                case BIBLIONET -> showPreview(result.publication(),
                        "Βρέθηκε στη Biblionet και προστέθηκε στον κατάλογο του δικτύου.");
                default -> showPreview(result.publication(), "Βρέθηκε στον κατάλογο του δικτύου.");
            }
        }, error -> {
            searchButton.setDisable(false);
            busyIndicator.setVisible(false);
            isbnMessage.setText(UserMessages.describe(error)); // invalid ISBN, Biblionet limit (429) or outage (502)
        });
    }
}
