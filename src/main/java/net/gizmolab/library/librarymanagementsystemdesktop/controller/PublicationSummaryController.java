package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PublicationDetailFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;

/** Read-only catalog data of a publication; used by the publication window and the add-wizard previews. */
@Controller
@Scope("prototype")
public class PublicationSummaryController extends BaseController {

    @Autowired private AuthService authService;

    @FXML private ImageView coverImage;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label originLabel;
    @FXML private GridPane detailsGrid;
    @FXML private VBox contributorsBox;
    @FXML private VBox subjectsBox;
    @FXML private VBox summarySection;
    @FXML private TextArea summaryArea;

    public void show(PublicationDTO pub) {
        titleLabel.setText(PublicationDetailFormatter.displayTitle(pub));
        setOptional(subtitleLabel, pub.getSubtitle());
        originLabel.setText(PublicationDetailFormatter.originLabel(pub));

        detailsGrid.getChildren().clear();
        List<String[]> rows = PublicationDetailFormatter.detailRows(pub);
        for (int i = 0; i < rows.size(); i++) {
            Label label = new Label(rows.get(i)[0] + ":");
            label.getStyleClass().add("form-label");
            Label value = new Label(rows.get(i)[1]);
            value.setWrapText(true);
            detailsGrid.addRow(i, label, value);
        }

        fillLines(contributorsBox, PublicationDetailFormatter.contributorLines(pub));
        fillLines(subjectsBox, PublicationDetailFormatter.subjectLines(pub));

        boolean hasSummary = pub.getSummary() != null && !pub.getSummary().isBlank();
        summarySection.setVisible(hasSummary);
        summarySection.setManaged(hasSummary);
        summaryArea.setText(hasSummary ? pub.getSummary() : "");

        loadCover(PublicationDetailFormatter.resolveCoverUrl(pub.getCoverImageUrl(), authService.getStrapiBaseUrl()));
    }

    private void fillLines(VBox box, List<String> lines) {
        box.getChildren().clear();
        if (lines.isEmpty()) {
            box.getChildren().add(new Label("—"));
        } else {
            lines.forEach(line -> box.getChildren().add(new Label(line)));
        }
    }

    private static void setOptional(Label label, String text) {
        boolean present = text != null && !text.isBlank();
        label.setText(present ? text : "");
        label.setVisible(present);
        label.setManaged(present);
    }

    /** Loads in the background; on failure the cover simply stays hidden. */
    private void loadCover(String url) {
        coverImage.setVisible(false);
        coverImage.setManaged(false);
        if (url == null) return;
        Image image = new Image(url, true);
        image.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !image.isError()) {
                coverImage.setImage(image);
                coverImage.setVisible(true);
                coverImage.setManaged(true);
            }
        });
        image.errorProperty().addListener((obs, oldV, isError) -> {
            if (isError) logInfo("Cover not loaded: %s", url);
        });
    }
}
