package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/** A magazine title of the shared catalog (issues are publications of type "Περιοδικό"). */
public class MagazineDTO {
    private String documentId;
    private String title;
    private String qualifier;
    private String issn;
    private String place;
    private String periodicity;
    private String nlgBiblionumber;
    private PublisherDTO publisher;
    private int issuesInLibrary;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getQualifier() { return qualifier; }
    public void setQualifier(String qualifier) { this.qualifier = qualifier; }
    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getPeriodicity() { return periodicity; }
    public void setPeriodicity(String periodicity) { this.periodicity = periodicity; }
    public String getNlgBiblionumber() { return nlgBiblionumber; }
    public void setNlgBiblionumber(String nlgBiblionumber) { this.nlgBiblionumber = nlgBiblionumber; }
    public PublisherDTO getPublisher() { return publisher; }
    public void setPublisher(PublisherDTO publisher) { this.publisher = publisher; }
    public int getIssuesInLibrary() { return issuesInLibrary; }
    public void setIssuesInLibrary(int issuesInLibrary) { this.issuesInLibrary = issuesInLibrary; }

    public boolean isFromNlg() { return nlgBiblionumber != null; }

    /** "Αναρχία (Θεσσαλονίκη)" when there is a qualifier. */
    public String getDisplayName() {
        return qualifier == null || qualifier.isBlank() ? title : title + " (" + qualifier + ")";
    }

    @Override
    public String toString() { return getDisplayName(); }
}
