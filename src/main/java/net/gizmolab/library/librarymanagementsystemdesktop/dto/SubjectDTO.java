package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * DTO for DDC Subject classification.
 * Maps to Strapi Subject (Θέματα DDC) content type.
 */
public class SubjectDTO {
    private String documentId;
    private String subjectTitle;      // Τίτλος θέματος (e.g., "Νεοελληνική πεζογραφία")
    private String subjectDDC;        // DDC κωδικός (e.g., "889.3")
    private String biblionetSubjectId; // Biblionet Subject ID

    public SubjectDTO() {}

    public SubjectDTO(String documentId, String subjectTitle, String subjectDDC, String biblionetSubjectId) {
        this.documentId = documentId;
        this.subjectTitle = subjectTitle;
        this.subjectDDC = subjectDDC;
        this.biblionetSubjectId = biblionetSubjectId;
    }

    // --- Getters & Setters ---
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getSubjectTitle() { return subjectTitle; }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }

    public String getSubjectDDC() { return subjectDDC; }
    public void setSubjectDDC(String subjectDDC) { this.subjectDDC = subjectDDC; }

    public String getBiblionetSubjectId() { return biblionetSubjectId; }
    public void setBiblionetSubjectId(String biblionetSubjectId) { this.biblionetSubjectId = biblionetSubjectId; }

    @Override
    public String toString() {
        if (subjectDDC != null && !subjectDDC.isEmpty()) {
            return subjectTitle + " (" + subjectDDC + ")";
        }
        return subjectTitle;
    }
}
