package net.gizmolab.library.librarymanagementsystemdesktop.dto.draft;

import net.gizmolab.library.librarymanagementsystemdesktop.util.Issn;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft.isBlank;
import static net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft.putText;

/** "Νέο περιοδικό"; payload for POST /api/magazines/local. */
public class MagazineDraft {

    private String title;
    private String qualifier;
    private String issn;
    private String place;
    private String periodicity;
    private String publisherId; // documentId

    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(title)) errors.put("title", "Ο τίτλος είναι υποχρεωτικός.");
        if (!isBlank(issn) && Issn.normalize(issn).isEmpty()) errors.put("issn", "Μη έγκυρο ISSN.");
        return errors;
    }

    /** { "data": {...} } with blank fields left out. Call only when validate() is empty. */
    public Map<String, Object> toPayload() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", title.trim());
        putText(data, "qualifier", qualifier);
        if (!isBlank(issn)) data.put("issn", Issn.normalize(issn).orElseThrow());
        putText(data, "place", place);
        putText(data, "periodicity", periodicity);
        if (publisherId != null) data.put("publisher", publisherId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", data);
        return payload;
    }

    public void setTitle(String title) { this.title = title; }
    public void setQualifier(String qualifier) { this.qualifier = qualifier; }
    public void setIssn(String issn) { this.issn = issn; }
    public void setPlace(String place) { this.place = place; }
    public void setPeriodicity(String periodicity) { this.periodicity = periodicity; }
    public void setPublisherId(String publisherId) { this.publisherId = publisherId; }
}
