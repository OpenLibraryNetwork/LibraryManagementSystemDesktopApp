package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import org.springframework.stereotype.Controller;

/** "Μπροσούρες": publications without ISBN. */
@Controller
public class BrochureManagementController extends PublicationListController {

    @Override
    protected String publicationType() {
        return "Μπροσούρα";
    }

    @Override
    protected void openAddWizard() {
        try {
            AddBrochureWizardController.open(fxmlLoaderFactory, tableView.getScene().getWindow());
        } catch (Exception e) {
            handleException("Προσθήκη μπροσούρας", e);
        }
    }
}
