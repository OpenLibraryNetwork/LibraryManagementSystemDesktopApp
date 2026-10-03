package net.gizmolab.library.librarymanagementsystemdesktop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Τοπικό entity δανεισμού — αποθηκεύεται στην H2.
 *
 * Αναφέρεται σε Strapi resources μέσω documentId (όχι JPA relations):
 * - strapiCopyDocumentId        → Copy.documentId στη Strapi 5
 * - strapiPublicationDocumentId → Book(Έντυπα).documentId στη Strapi 5
 *
 * Cached πεδία (publicationTitle, publicationType, isbn, authorName)
 * αποφεύγουν REST calls για εμφάνιση σε λίστες.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "BORROWS")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Τοπική JPA σχέση ===
    // null once the borrow is anonymised (BorrowRetentionService); kept out of toString so no log line names a borrower
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    @ToString.Exclude
    private User user;

    // === Strapi αναφορές (documentIds, not JPA) ===
    @Column(name = "STRAPI_COPY_DOCUMENT_ID", nullable = false)
    private String strapiCopyDocumentId;

    @Column(name = "STRAPI_PUBLICATION_DOCUMENT_ID")
    private String strapiPublicationDocumentId;

    @Column(name = "COPY_NUMBER")
    private Integer copyNumber;

    // === Cached πεδία (αποφεύγουν REST calls) ===
    @Column(name = "PUBLICATION_TITLE")
    private String publicationTitle;

    @Column(name = "PUBLICATION_TYPE")
    private String publicationType;  // "Βιβλίο", "Μπροσούρα", "Περιοδικό"

    @Column(name = "ISBN")
    private String isbn;  // null για μπροσούρες

    @Column(name = "AUTHOR_NAME")
    private String authorName;  // Comma-separated αν πολλαπλοί

    // === Ημερομηνίες ===
    @Column(name = "BORROW_DATE", nullable = false)
    private LocalDate borrowDate;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;  // borrowDate + 14 days (configurable)

    @Column(name = "RETURN_DATE")
    private LocalDate returnDate;  // null αν δεν επιστράφηκε

    @Column(name = "RETURNED", nullable = false)
    private boolean returned = false;
}
