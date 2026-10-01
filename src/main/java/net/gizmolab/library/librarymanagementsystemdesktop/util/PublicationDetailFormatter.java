package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.CopyDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.SubjectDTO;

import java.util.ArrayList;
import java.util.List;

/** Pure text formatting for the read-only publication window. */
public final class PublicationDetailFormatter {

    private PublicationDetailFormatter() {}

    public static final String PERIODICAL = "Περιοδικό";

    /** "τεύχ. 5 (Δεκέμβριος 2016)", "τεύχ. 5", "Άνοιξη 2020"; empty when both are blank. */
    public static String issueLabel(String number, String period) {
        boolean hasNumber = !isBlank(number);
        boolean hasPeriod = !isBlank(period);
        if (hasNumber && hasPeriod) return "τεύχ. " + number.trim() + " (" + period.trim() + ")";
        if (hasNumber) return "τεύχ. " + number.trim();
        return hasPeriod ? period.trim() : "";
    }

    /** An issue: "Κοινωνικός Αναρχισμός — τεύχ. 5"; anything else: its title. */
    public static String displayTitle(PublicationDTO pub) {
        if (!PERIODICAL.equals(pub.getType())) return pub.getTitle();
        String label = issueLabel(pub.getIssueNumber(), pub.getPublicationMonthYear());
        return label.isEmpty() ? pub.getTitle() : pub.getTitle() + " — " + label;
    }

    public static List<String> contributorLines(PublicationDTO pub) {
        return pub.getContributors().stream()
                .map(c -> c.getPerson().getDisplayName() + " — " + c.getRole().getName())
                .toList();
    }

    /** " — Author, Author" for list labels, or "" when the publication has no author. */
    public static String authorSuffix(PublicationDTO pub) {
        String authors = pub.getAuthorNames();
        return authors.isEmpty() ? "" : " — " + authors;
    }

    public static List<String> subjectLines(PublicationDTO pub) {
        if (pub.getSubjects() == null) return List.of();
        return pub.getSubjects().stream().map(PublicationDetailFormatter::subjectLine).toList();
    }

    private static String subjectLine(SubjectDTO s) {
        return isBlank(s.getSubjectDDC()) ? s.getSubjectTitle() : s.getSubjectDDC() + " " + s.getSubjectTitle();
    }

    public static String originLabel(PublicationDTO pub) {
        String origin = pub.isFromBiblionet() ? "Biblionet" : "Τοπική εγγραφή";
        return pub.isReviewed() ? origin : origin + " — εκκρεμεί έλεγχος από τον καταλογογράφο";
    }

    /** Label/value rows in a fixed order; empty values are left out. */
    public static List<String[]> detailRows(PublicationDTO pub) {
        List<String[]> rows = new ArrayList<>();
        add(rows, "Τύπος", pub.getType());
        if (PERIODICAL.equals(pub.getType())) {
            add(rows, "Περιοδικό", pub.getMagazineTitle() != null ? pub.getMagazineTitle() : pub.getTitle());
            add(rows, "Αριθμός", pub.getIssueNumber());
            add(rows, "Περίοδος", pub.getPublicationMonthYear());
        }
        add(rows, "ISBN", pub.getIsbn());
        add(rows, "Εκδότης", pub.getPublisher() != null ? pub.getPublisher().getDisplayName() : null);
        add(rows, "Έτος", pub.getYearPublished() != null ? String.valueOf(pub.getYearPublished()) : null);
        add(rows, "Σελίδες", pub.getPages() != null ? String.valueOf(pub.getPages()) : null);
        add(rows, "Γλώσσα", pub.getLanguage());
        add(rows, "Γλώσσα πρωτοτύπου", pub.getOriginalLanguage());
        add(rows, "Έκδοση", pub.getEdition());
        add(rows, "Σειρά", pub.getSeries());
        add(rows, "Τόπος", pub.getPlace());
        add(rows, "Βιβλιοδεσία", pub.getBinding());
        add(rows, "Διαστάσεις", pub.getDimensions());
        return rows;
    }

    private static void add(List<String[]> rows, String label, String value) {
        if (!isBlank(value)) rows.add(new String[]{label, value.trim()});
    }

    /** Strapi uploads are relative ("/uploads/…"); Biblionet fallbacks are absolute. */
    public static String resolveCoverUrl(String coverUrl, String baseUrl) {
        if (isBlank(coverUrl)) return null;
        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) return coverUrl;
        return baseUrl + (coverUrl.startsWith("/") ? "" : "/") + coverUrl;
    }

    /** A borrowed copy must be returned before it can be deleted (the server enforces this too). */
    public static boolean canDeleteCopy(CopyDTO copy) {
        return copy != null && copy.isAvailable();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
