package net.gizmolab.library.librarymanagementsystemdesktop.dto.draft;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data of the local cataloguing form; payload for POST /api/books/local. */
public class PublicationDraft {

    public static final String BOOK = "Βιβλίο";
    public static final String BROCHURE = "Μπροσούρα";
    public static final String PERIODICAL = "Περιοδικό";

    private String type;
    private String isbn;
    private String title;
    private String subtitle;
    private String yearText;
    private String pagesText;
    private String language;
    private String originalLanguage;
    private String edition;
    private String place;
    private String series;
    private String summary;
    private String publisherId; // documentId
    private List<ContributorDTO> contributors = new ArrayList<>();
    private List<String> subjectIds = new ArrayList<>(); // documentIds
    private String magazineId; // documentId
    private String issueNumber;
    private String period;

    /** Field key → Greek message; empty when the draft can be sent. */
    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(title)) errors.put("title", "Ο τίτλος είναι υποχρεωτικός.");
        if (BOOK.equals(type) && isBlank(isbn)) errors.put("isbn", "Το ISBN είναι υποχρεωτικό.");
        if (!isBlank(yearText) && !isInteger(yearText)) errors.put("year", "Το έτος πρέπει να είναι αριθμός.");
        if (!isBlank(pagesText) && !isInteger(pagesText)) errors.put("pages", "Οι σελίδες πρέπει να είναι αριθμός.");
        if (contributors.stream().anyMatch(c -> c.getPerson() == null || c.getRole() == null)) {
            errors.put("contributors", "Κάθε συντελεστής χρειάζεται πρόσωπο και ρόλο.");
        }
        if (PERIODICAL.equals(type)) {
            if (magazineId == null) errors.put("magazine", "Επιλέξτε περιοδικό.");
            if (isBlank(issueNumber) && isBlank(period)) errors.put("issue", "Συμπληρώστε αριθμό ή περίοδο τεύχους.");
        }
        return errors;
    }

    /** { "data": {...} } with blank fields left out. Call only when validate() is empty. */
    public Map<String, Object> toPayload() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        if (BOOK.equals(type)) data.put("isbn", isbn.trim());
        data.put("title", title.trim());
        putText(data, "subtitle", subtitle);
        if (PERIODICAL.equals(type)) {
            data.put("magazine", magazineId);
            putText(data, "issueNumber", issueNumber);
            putText(data, "publicationMonthYear", period);
        }
        if (!isBlank(yearText)) data.put("yearPublished", Integer.parseInt(yearText.trim()));
        if (!isBlank(pagesText)) data.put("pages", Integer.parseInt(pagesText.trim()));
        putText(data, "language", language);
        putText(data, "originalLanguage", originalLanguage);
        putText(data, "edition", edition);
        putText(data, "place", place);
        putText(data, "series", series);
        putText(data, "summary", summary);
        if (publisherId != null) data.put("publisher", publisherId);
        if (!contributors.isEmpty()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ContributorDTO c : contributors) {
                rows.add(Map.of("person", c.getPerson().getDocumentId(), "role", c.getRole().getDocumentId()));
            }
            data.put("contributors", rows);
        }
        if (!subjectIds.isEmpty()) data.put("subjects", List.copyOf(subjectIds));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", data);
        return payload;
    }

    static void putText(Map<String, Object> data, String key, String value) {
        if (!isBlank(value)) data.put(key, value.trim());
    }

    static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isInteger(String s) {
        return s.trim().matches("\\d{1,9}");
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getYearText() { return yearText; }
    public void setYearText(String yearText) { this.yearText = yearText; }
    public String getPagesText() { return pagesText; }
    public void setPagesText(String pagesText) { this.pagesText = pagesText; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }
    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPublisherId() { return publisherId; }
    public void setPublisherId(String publisherId) { this.publisherId = publisherId; }
    public List<ContributorDTO> getContributors() { return contributors; }
    public void setContributors(List<ContributorDTO> contributors) {
        this.contributors = contributors != null ? new ArrayList<>(contributors) : new ArrayList<>();
    }
    public List<String> getSubjectIds() { return subjectIds; }
    public void setSubjectIds(List<String> subjectIds) {
        this.subjectIds = subjectIds != null ? new ArrayList<>(subjectIds) : new ArrayList<>();
    }
    public String getMagazineId() { return magazineId; }
    public void setMagazineId(String magazineId) { this.magazineId = magazineId; }
    public String getIssueNumber() { return issueNumber; }
    public void setIssueNumber(String issueNumber) { this.issueNumber = issueNumber; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
