package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Opens the read-only publication window (with the library's copies) and waits until it closes. */
public final class PublicationDetailWindow {

    private PublicationDetailWindow() {}

    public static void open(FXMLLoaderFactory fxmlLoaderFactory, Window owner, PublicationDTO publication) throws Exception {
        var result = fxmlLoaderFactory.<Parent, PublicationDetailModalController>loadWithController(
                "/fxml/publication-detail-modal.fxml");
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle(publication.getTitle());
        Scene scene = new Scene(result.getRoot(), 900, 750);
        StylesheetHelper.applyModernTheme(scene);
        stage.setScene(scene);
        result.getController().setPublication(publication);
        stage.showAndWait();
    }
}
