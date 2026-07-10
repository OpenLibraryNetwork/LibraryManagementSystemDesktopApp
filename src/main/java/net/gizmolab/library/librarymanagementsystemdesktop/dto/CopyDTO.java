package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * DTO for Strapi Copy (Αντίτυπα) content type.
 */
public class CopyDTO {
    private Long id;                // Strapi Copy ID
    private int copyNumber;
    private boolean isAvailable;
    private String condition;       // "NEW", "GOOD", "FAIR", "POOR"

    // Parent publication (flattened)
    private Long publicationId;
    private String publicationTitle;

    // Parent library (flattened)
    private Long libraryId;
    private String libraryName;

    public CopyDTO() {}

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getCopyNumber() { return copyNumber; }
    public void setCopyNumber(int copyNumber) { this.copyNumber = copyNumber; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public Long getPublicationId() { return publicationId; }
    public void setPublicationId(Long publicationId) { this.publicationId = publicationId; }
    public String getPublicationTitle() { return publicationTitle; }
    public void setPublicationTitle(String publicationTitle) { this.publicationTitle = publicationTitle; }
    public Long getLibraryId() { return libraryId; }
    public void setLibraryId(Long libraryId) { this.libraryId = libraryId; }
    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }
}
