package net.gizmolab.library.librarymanagementsystemdesktop.dto;

import java.time.LocalDate;

/**
 * DTO for Borrow records.
 * Combines local H2 data (user, dates) with cached Strapi data (publication info).
 */
public class BorrowDTO {
    private Long id;                   // Local Borrow.id

    // User info (from local H2)
    private Long userId;
    private String userFirstName;
    private String userLastName;

    // Strapi references
    private Long strapiCopyId;
    private Long strapiPublicationId;
    private Integer copyNumber;

    // Cached publication info (from Borrow entity)
    private String publicationTitle;
    private String publicationType;     // "Βιβλίο", "Μπροσούρα", "Περιοδικό"
    private String isbn;
    private String authorName;

    // Dates
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    // Status
    private boolean returned;
    private boolean overdue;
    private String status;             // "Active", "Overdue", "Returned"

    public BorrowDTO() {}

    // --- Convenience methods ---
    public String getUserFullName() {
        return ((userFirstName != null ? userFirstName : "") + " " +
                (userLastName != null ? userLastName : "")).trim();
    }

    public boolean isActive() {
        return !returned;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserFirstName() { return userFirstName; }
    public void setUserFirstName(String userFirstName) { this.userFirstName = userFirstName; }
    public String getUserLastName() { return userLastName; }
    public void setUserLastName(String userLastName) { this.userLastName = userLastName; }
    public Long getStrapiCopyId() { return strapiCopyId; }
    public void setStrapiCopyId(Long strapiCopyId) { this.strapiCopyId = strapiCopyId; }
    public Long getStrapiPublicationId() { return strapiPublicationId; }
    public void setStrapiPublicationId(Long strapiPublicationId) { this.strapiPublicationId = strapiPublicationId; }
    public Integer getCopyNumber() { return copyNumber; }
    public void setCopyNumber(Integer copyNumber) { this.copyNumber = copyNumber; }
    public String getPublicationTitle() { return publicationTitle; }
    public void setPublicationTitle(String publicationTitle) { this.publicationTitle = publicationTitle; }
    public String getPublicationType() { return publicationType; }
    public void setPublicationType(String publicationType) { this.publicationType = publicationType; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }
    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
