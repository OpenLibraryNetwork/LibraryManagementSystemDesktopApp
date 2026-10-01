package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * DTO for DDC Subject classification.
 * Maps to Strapi Subject (Θέματα DDC) content type.
 */
public class SubjectDTO {
    private Long id;                  // Strapi ID
    private String subjectTitle;      // Τίτλος θέματος (e.g., "Νεοελληνική πεζογραφία")
    private String subjectDDC;        // DDC κωδικός (e.g., "889.3")
    private String biblionetSubjectId; // Biblionet Subject ID

    public SubjectDTO() {}

    public SubjectDTO(Long id, String subjectTitle, String subjectDDC, String biblionetSubjectId) {
        this.id = id;
        this.subjectTitle = subjectTitle;
        this.subjectDDC = subjectDDC;
        this.biblionetSubjectId = biblionetSubjectId;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
