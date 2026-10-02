package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/** Person (author, translator, …) from the shared authority file. */
public class PersonDTO {
    private String documentId;
    private String name;
    private String qualifier;
    private String firstname;
    private String middlename;
    private String lastname;
    private String bornYear;
    private String deathYear;
    private String biography;
    private String biblionetPersonId;
    private boolean reviewed;
    private int bookCount;

    /** "Name (qualifier)" when a qualifier exists, otherwise the name. */
    public String getDisplayName() {
        String base = name != null ? name : "";
        return isBlank(qualifier) ? base : base + " (" + qualifier.trim() + ")";
    }

    /** "1950–2010", "1950–", "–2010" or "". */
    public String getLifespan() {
        String born = trimmed(bornYear);
        String death = trimmed(deathYear);
        if (born.isEmpty() && death.isEmpty()) return "";
        return born + "–" + death;
    }

    private static String trimmed(String s) { return s == null ? "" : s.trim(); }

    public boolean isFromBiblionet() { return biblionetPersonId != null; }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQualifier() { return qualifier; }
    public void setQualifier(String qualifier) { this.qualifier = qualifier; }
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getMiddlename() { return middlename; }
    public void setMiddlename(String middlename) { this.middlename = middlename; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getBornYear() { return bornYear; }
    public void setBornYear(String bornYear) { this.bornYear = bornYear; }
    public String getDeathYear() { return deathYear; }
    public void setDeathYear(String deathYear) { this.deathYear = deathYear; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
    public String getBiblionetPersonId() { return biblionetPersonId; }
    public void setBiblionetPersonId(String biblionetPersonId) { this.biblionetPersonId = biblionetPersonId; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }

    @Override
    public String toString() { return getDisplayName(); }
}
