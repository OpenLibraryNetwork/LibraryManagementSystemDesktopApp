package net.gizmolab.library.librarymanagementsystemdesktop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Τοπικό entity δανεισμού — αποθηκεύεται στην H2.
 *
 * Αναφέρεται σε Strapi resources μέσω IDs (όχι JPA relations):
 * - strapiCopyId    → Copy.id στο Strapi
 * - strapiPublicationId → Book(Έντυπα).id στο Strapi
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    // === Strapi αναφορές (IDs, not JPA) ===
    @Column(name = "STRAPI_COPY_ID", nullable = false)
    private Long strapiCopyId;

    @Column(name = "STRAPI_PUBLICATION_ID")
    private Long strapiPublicationId;

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
