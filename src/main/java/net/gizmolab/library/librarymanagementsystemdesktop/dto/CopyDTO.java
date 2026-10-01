package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * DTO for Strapi Copy (Αντίτυπα) content type.
 */
public class CopyDTO {
    private String documentId;
    private int copyNumber;
    private boolean isAvailable;
    private String condition;       // "NEW", "GOOD", "FAIR", "POOR"

    // Parent publication (flattened)
    private String publicationDocumentId;
    private String publicationTitle;

    // Parent library (flattened)
    private String libraryDocumentId;
    private String libraryName;

    public CopyDTO() {}

    // --- Getters & Setters ---
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public int getCopyNumber() { return copyNumber; }
    public void setCopyNumber(int copyNumber) { this.copyNumber = copyNumber; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getPublicationDocumentId() { return publicationDocumentId; }
    public void setPublicationDocumentId(String publicationDocumentId) { this.publicationDocumentId = publicationDocumentId; }
    public String getPublicationTitle() { return publicationTitle; }
    public void setPublicationTitle(String publicationTitle) { this.publicationTitle = publicationTitle; }
    public String getLibraryDocumentId() { return libraryDocumentId; }
    public void setLibraryDocumentId(String libraryDocumentId) { this.libraryDocumentId = libraryDocumentId; }
    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }
}
