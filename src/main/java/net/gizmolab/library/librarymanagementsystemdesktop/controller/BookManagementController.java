package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import org.springframework.stereotype.Controller;

/** "Βιβλία": publications with ISBN. */
@Controller
public class BookManagementController extends PublicationListController {

    @Override
    protected String publicationType() {
        return "Βιβλίο";
    }

    @Override
    protected void openAddWizard() {
        try {
            AddBookWizardController.open(fxmlLoaderFactory, tableView.getScene().getWindow());
        } catch (Exception e) {
            handleException("Προσθήκη βιβλίου", e);
        }
    }
}
