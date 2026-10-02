package net.gizmolab.library.librarymanagementsystemdesktop.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified DTO for all publication types (Βιβλίο, Μπροσούρα, Περιοδικό).
 * Maps to Strapi Book (Έντυπα) content type.
 */
public class PublicationDTO {
    private String documentId;
    private String title;
    private String type;              // "Βιβλίο", "Μπροσούρα", "Περιοδικό"
    private String isbn;              // only for Βιβλίο
    private String subtitle;
    private Integer yearPublished;
    private String description;
    private String summary;
    private Integer pages;
    private String language;
    private String coverImageUrl;
    private String binding;
    private String edition;
    private String category;

    // Biblionet fields
    private String biblionetId;
    private String biblionetCategoryId;
    private String originalLanguage;
    private String dimensions;
    private String place;
    private String series;
    private Double price;
    private Integer weight;


    // Periodical-specific
    private String issueNumber;
    private String publicationMonthYear;
    private String magazineDocumentId;
    private String magazineTitle;

    // Relations (flattened for display)
    private PublisherDTO publisher;
    private List<SubjectDTO> subjects;


    // Copy stats (aggregated from Strapi)
    private int totalCopies;
    private int availableCopies;

    // --- Getters & Setters ---
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public Integer getYearPublished() { return yearPublished; }
    public void setYearPublished(Integer yearPublished) { this.yearPublished = yearPublished; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getBinding() { return binding; }
    public void setBinding(String binding) { this.binding = binding; }
    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIssueNumber() { return issueNumber; }
    public void setIssueNumber(String issueNumber) { this.issueNumber = issueNumber; }
    public String getPublicationMonthYear() { return publicationMonthYear; }
    public void setPublicationMonthYear(String publicationMonthYear) { this.publicationMonthYear = publicationMonthYear; }
    public String getMagazineDocumentId() { return magazineDocumentId; }
    public void setMagazineDocumentId(String magazineDocumentId) { this.magazineDocumentId = magazineDocumentId; }
    public String getMagazineTitle() { return magazineTitle; }
    public void setMagazineTitle(String magazineTitle) { this.magazineTitle = magazineTitle; }
    public PublisherDTO getPublisher() { return publisher; }
    public void setPublisher(PublisherDTO publisher) { this.publisher = publisher; }
    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public String getBiblionetId() { return biblionetId; }
    public void setBiblionetId(String biblionetId) { this.biblionetId = biblionetId; }
    public String getBiblionetCategoryId() { return biblionetCategoryId; }
    public void setBiblionetCategoryId(String biblionetCategoryId) { this.biblionetCategoryId = biblionetCategoryId; }
    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public List<SubjectDTO> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectDTO> subjects) { this.subjects = subjects; }

    private List<ContributorDTO> contributors = new ArrayList<>();
    private boolean reviewed;

    /** Contributors in presentation order; never null. */
    public List<ContributorDTO> getContributors() { return contributors; }
    public void setContributors(List<ContributorDTO> contributors) {
        this.contributors = contributors != null ? contributors : new ArrayList<>();
    }

    /** Display names of contributors with the author role, comma-separated ("" if none). */
    public String getAuthorNames() {
        return contributors.stream()
                .filter(c -> c.getRole().isAuthor())
                .map(c -> c.getPerson().getDisplayName())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

    public boolean isFromBiblionet() { return biblionetId != null; }
}
