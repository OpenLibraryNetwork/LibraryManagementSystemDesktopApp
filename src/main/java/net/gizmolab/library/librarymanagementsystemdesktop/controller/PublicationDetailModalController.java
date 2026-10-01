package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import javafx.fxml.FXML;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/** Publication window: catalog data (summary) on top, the library's copies below. */
@Controller
@Scope("prototype")
public class PublicationDetailModalController extends BaseController {

    @FXML private PublicationSummaryController summaryController; // fx:id="summary"
    @FXML private CopyManagementModalController copiesController; // fx:id="copies"

    public void setPublication(PublicationDTO pub) {
        summaryController.show(pub);
        copiesController.setPublication(pub);
    }
}
