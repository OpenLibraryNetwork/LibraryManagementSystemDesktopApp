package net.gizmolab.library.librarymanagementsystemdesktop.service.utilities;

import net.gizmolab.library.librarymanagementsystemdesktop.util.PublicationDetailFormatter;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.*;
import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts between Strapi JSON responses and DTOs.
 *
 * Strapi 5 response format (flat):
 * - Single: { "data": { "id": N, "documentId": "…", ... } }
 * - List:   { "data": [ { "id": N, "documentId": "…", ... }, ... ] }
 * - Relations: the related object (or an array of objects) in place, without a "data" wrapper.
 * Records are identified by documentId; the numeric id is not used by the client.
 */
public class DTOConverter {

    // ═══════════════════════════════════════════════════════
    // Publication (Έντυπα) — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    /**
     * Parse a Strapi Book response into PublicationDTO.
     * Accepts either the wrapper { "data": { ... } } or the record itself.
     */
    public static PublicationDTO publicationFromJson(JsonNode node) {
        return publicationFromJson(node, net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService.getCurrentLibraryDocumentId());
    }

    /**
     * @param currentLibraryDocumentId copies of other libraries are not counted; null counts every copy
     */
    public static PublicationDTO publicationFromJson(JsonNode node, String currentLibraryDocumentId) {
        if (node == null) return null;

        // Handle wrapper: { "data": { ... } }
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        PublicationDTO dto = new PublicationDTO();
        dto.setDocumentId(textOrNull(node, "documentId"));

        JsonNode attr = node;

        dto.setTitle(textOrNull(attr, "title"));
        dto.setType(textOrNull(attr, "type"));
        dto.setIsbn(textOrNull(attr, "isbn"));
        dto.setSubtitle(textOrNull(attr, "subtitle"));
        dto.setYearPublished(intOrNull(attr, "yearPublished"));
        dto.setDescription(textOrNull(attr, "description"));
        dto.setSummary(textOrNull(attr, "summary"));
        dto.setPages(intOrNull(attr, "pages"));
        dto.setLanguage(textOrNull(attr, "language"));
        dto.setCoverImageUrl(textOrNull(attr, "coverImageUrl"));
        dto.setBinding(textOrNull(attr, "binding"));
        dto.setEdition(textOrNull(attr, "edition"));
        dto.setCategory(textOrNull(attr, "category"));
        dto.setBiblionetId(textOrNull(attr, "biblionetId"));
        dto.setBiblionetCategoryId(textOrNull(attr, "biblionetCategoryId"));
        dto.setOriginalLanguage(textOrNull(attr, "originalLanguage"));
        dto.setDimensions(textOrNull(attr, "dimensions"));
        dto.setPlace(textOrNull(attr, "place"));
        dto.setSeries(textOrNull(attr, "series"));
        dto.setPrice(doubleOrNull(attr, "price"));
        dto.setWeight(intOrNull(attr, "weight"));
        dto.setIssueNumber(textOrNull(attr, "issueNumber"));
        dto.setPublicationMonthYear(textOrNull(attr, "publicationMonthYear"));


        // Contributors: repeatable component, in presentation order.
        // Rows without a person or a role (e.g. a deleted role) are skipped.
        List<ContributorDTO> contributors = new ArrayList<>();
        JsonNode contributorsNode = attr.path("contributors");
        if (contributorsNode.isArray()) {
            for (JsonNode row : contributorsNode) {
                PersonDTO person = personFromJson(row.path("person"));
                ContributorRoleDTO role = roleFromJson(row.path("role"));
                if (person != null && role != null) {
                    contributors.add(new ContributorDTO(person, role));
                }
            }
        }
        dto.setContributors(contributors);
        dto.setReviewed(attr.path("reviewed").asBoolean(false));

        // Parse publisher relation
        JsonNode publisherNode = attr.path("publisher");
        if (publisherNode.isObject()) {
            dto.setPublisher(publisherFromJson(publisherNode));
        }

        // Parse subjects relation
        JsonNode subjectsNode = attr.path("subjects");
        if (subjectsNode.isArray()) {
            List<SubjectDTO> subjects = new ArrayList<>();
            for (JsonNode subjectNode : subjectsNode) {
                subjects.add(subjectFromJson(subjectNode));
            }
            dto.setSubjects(subjects);
        }


        // Parse copies relation (count only for the current library)
        JsonNode copiesNode = attr.path("copies");
        if (copiesNode.isArray()) {
            int total = 0;
            int available = 0;
            for (JsonNode copyNode : copiesNode) {
                // Skip copies known to belong to another library
                if (currentLibraryDocumentId != null) {
                    String copyLibrary = textOrNull(copyNode.path("library"), "documentId");
                    if (copyLibrary != null && !copyLibrary.equals(currentLibraryDocumentId)) {
                        continue;
                    }
                }

                total++;
                if (copyNode.path("isAvailable").asBoolean(false)) {
                    available++;
                }
            }
            dto.setTotalCopies(total);
            dto.setAvailableCopies(available);
        }

        // Parse magazine relation (for periodicals)
        JsonNode magNode = attr.path("magazine");
        if (magNode.isObject()) {
            dto.setMagazineDocumentId(textOrNull(magNode, "documentId"));
            dto.setMagazineTitle(textOrNull(magNode, "title"));
        }

        return dto;
    }

    /**
     * Parse a Strapi list response into List<PublicationDTO>.
     */
    public static List<PublicationDTO> publicationsFromJson(JsonNode response) {
        List<PublicationDTO> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                PublicationDTO dto = publicationFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Copy (Αντίτυπα) — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static CopyDTO copyFromJson(JsonNode node) {
        if (node == null) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        CopyDTO dto = new CopyDTO();
        dto.setDocumentId(textOrNull(node, "documentId"));

        JsonNode attr = node;
        dto.setCopyNumber(attr.path("copyNumber").asInt(1));
        dto.setAvailable(attr.path("isAvailable").asBoolean(true));
        dto.setCondition(textOrNull(attr, "condition"));

        // Parse publication relation
        JsonNode pubAttr = attr.path("publication");
        if (pubAttr.isObject()) {
            dto.setPublicationDocumentId(textOrNull(pubAttr, "documentId"));
            String title = textOrNull(pubAttr, "title");
            if (PublicationDetailFormatter.PERIODICAL.equals(textOrNull(pubAttr, "type"))) {
                String label = PublicationDetailFormatter.issueLabel(
                        textOrNull(pubAttr, "issueNumber"), textOrNull(pubAttr, "publicationMonthYear"));
                if (!label.isEmpty()) title = title + " — " + label;
            }
            dto.setPublicationTitle(title);
        }

        // Parse library relation
        JsonNode libNode = attr.path("library");
        if (libNode.isObject()) {
            dto.setLibraryDocumentId(textOrNull(libNode, "documentId"));
            dto.setLibraryName(textOrNull(libNode, "name"));
        }

        return dto;
    }

    public static List<CopyDTO> copiesFromJson(JsonNode response) {
        List<CopyDTO> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                CopyDTO dto = copyFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Person & contributor role — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static PersonDTO personFromJson(JsonNode node) {
        node = unwrap(node);
        if (node == null) return null;

        PersonDTO dto = new PersonDTO();
        dto.setDocumentId(textOrNull(node, "documentId"));
        JsonNode attr = node;
        dto.setName(textOrNull(attr, "name"));
        dto.setQualifier(textOrNull(attr, "qualifier"));
        dto.setFirstname(textOrNull(attr, "firstname"));
        dto.setMiddlename(textOrNull(attr, "middlename"));
        dto.setLastname(textOrNull(attr, "lastname"));
        dto.setBornYear(textOrNull(attr, "bornYear"));
        dto.setDeathYear(textOrNull(attr, "deathYear"));
        dto.setBiography(textOrNull(attr, "biography"));
        dto.setBiblionetPersonId(textOrNull(attr, "biblionetPersonId"));
        dto.setReviewed(attr.path("reviewed").asBoolean(false));
        dto.setBookCount(attr.path("bookCount").asInt(0));
        return dto;
    }

    public static List<PersonDTO> personsFromJson(JsonNode response) {
        List<PersonDTO> result = new ArrayList<>();
        if (response == null) return result;
        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                PersonDTO dto = personFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    public static ContributorRoleDTO roleFromJson(JsonNode node) {
        node = unwrap(node);
        if (node == null) return null;
        JsonNode attr = node;
        return new ContributorRoleDTO(textOrNull(node, "documentId"), textOrNull(attr, "name"), textOrNull(attr, "biblionetTypeId"));
    }

    /** Accepts { "data": {...} }, {...}, null, NullNode or MissingNode; returns the inner node or null. */
    private static JsonNode unwrap(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        if (node.has("data") && !node.get("data").isArray()) node = node.get("data");
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        return node;
    }

    // ═══════════════════════════════════════════════════════
    // Publisher — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static PublisherDTO publisherFromJson(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        PublisherDTO dto = new PublisherDTO();
        dto.setDocumentId(textOrNull(node, "documentId"));

        JsonNode attr = node;
        dto.setName(textOrNull(attr, "name"));
        dto.setBiblionetCompanyId(textOrNull(attr, "biblionetCompanyId"));
        dto.setAddress(textOrNull(attr, "address"));
        dto.setPhone(textOrNull(attr, "phone"));
        dto.setEmail(textOrNull(attr, "email"));
        dto.setWebsite(textOrNull(attr, "website"));
        dto.setAlternativeName(textOrNull(attr, "alternativeName"));
        dto.setQualifier(textOrNull(attr, "qualifier"));
        dto.setReviewed(attr.path("reviewed").asBoolean(false));

        if (attr.has("bookCount")) {
            dto.setBookCount(attr.path("bookCount").asInt(0));
        } else {
            JsonNode booksNode = attr.path("books");
            if (booksNode.isArray()) {
                dto.setBookCount(booksNode.size());
            }
        }

        return dto;
    }

    public static List<PublisherDTO> publishersFromJson(JsonNode response) {
        List<PublisherDTO> result = new ArrayList<>();
        if (response == null) return result;
        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                PublisherDTO dto = publisherFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Magazine — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static MagazineDTO magazineFromJson(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        MagazineDTO dto = new MagazineDTO();
        dto.setDocumentId(textOrNull(node, "documentId"));

        JsonNode attr = node;
        dto.setTitle(textOrNull(attr, "title"));
        dto.setIssn(textOrNull(attr, "issn"));
        dto.setQualifier(textOrNull(attr, "qualifier"));
        dto.setPlace(textOrNull(attr, "place"));
        dto.setPeriodicity(textOrNull(attr, "periodicity"));
        dto.setNlgBiblionumber(textOrNull(attr, "nlgBiblionumber"));
        dto.setIssuesInLibrary(attr.path("issuesInLibrary").asInt(0));

        JsonNode pubNode = attr.path("publisher");
        if (pubNode.isObject()) {
            dto.setPublisher(publisherFromJson(pubNode));
        }

        return dto;
    }

    public static List<MagazineDTO> magazinesFromJson(JsonNode response) {
        List<MagazineDTO> result = new ArrayList<>();
        if (response == null) return result;
        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                MagazineDTO dto = magazineFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Borrow — from local H2 entity
    // ═══════════════════════════════════════════════════════

    /**
     * Convert local Borrow entity to BorrowDTO.
     * No Strapi call needed — all display fields are cached in Borrow.
     */
    public static BorrowDTO convertBorrowToDTO(Borrow borrow) {
        if (borrow == null) return null;

        BorrowDTO dto = new BorrowDTO();
        dto.setId(borrow.getId());
        dto.setStrapiCopyDocumentId(borrow.getStrapiCopyDocumentId());
        dto.setStrapiPublicationDocumentId(borrow.getStrapiPublicationDocumentId());
        dto.setCopyNumber(borrow.getCopyNumber());

        // Cached publication info
        dto.setPublicationTitle(borrow.getPublicationTitle());
        dto.setPublicationType(borrow.getPublicationType());
        dto.setIsbn(borrow.getIsbn());
        dto.setAuthorName(borrow.getAuthorName());

        // User info (local JPA)
        if (borrow.getUser() != null) {
            dto.setUserId(borrow.getUser().getUserId());
            dto.setUserFirstName(borrow.getUser().getFirstname());
            dto.setUserLastName(borrow.getUser().getLastname());
        }

        // Dates
        dto.setBorrowDate(borrow.getBorrowDate());
        dto.setDueDate(borrow.getDueDate());
        dto.setReturnDate(borrow.getReturnDate());
        dto.setReturned(borrow.isReturned());

        // Calculated status
        dto.setOverdue(calculateOverdue(borrow));
        dto.setStatus(calculateStatus(borrow));

        return dto;
    }

    private static boolean calculateOverdue(Borrow borrow) {
        if (borrow.isReturned()) return false;
        if (borrow.getDueDate() == null) return false;
        return LocalDate.now(ZoneId.systemDefault()).isAfter(borrow.getDueDate());
    }

    private static String calculateStatus(Borrow borrow) {
        if (borrow.isReturned()) return "Returned";
        if (calculateOverdue(borrow)) return "Overdue";
        return "Active";
    }

    // ═══════════════════════════════════════════════════════
    // Subject (Θέματα DDC) — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static SubjectDTO subjectFromJson(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        SubjectDTO dto = new SubjectDTO();
        dto.setDocumentId(textOrNull(node, "documentId"));

        JsonNode attr = node;
        dto.setSubjectTitle(textOrNull(attr, "subjectTitle"));
        dto.setSubjectDDC(textOrNull(attr, "subjectDDC"));
        dto.setBiblionetSubjectId(textOrNull(attr, "biblionetSubjectId"));

        return dto;
    }

    public static List<SubjectDTO> subjectsFromJson(JsonNode response) {
        List<SubjectDTO> result = new ArrayList<>();
        if (response == null) return result;
        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                SubjectDTO dto = subjectFromJson(item);
                if (dto != null) result.add(dto);
            }
        } else if (response.isArray()) {
            for (JsonNode item : response) {
                SubjectDTO dto = subjectFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════


    // Jackson 3 throws when a value cannot be converted (an array in a text field, "abc" in a number field);
    // one odd field must not fail a whole list, so these return null instead.

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        return value.asString(null);
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        java.util.OptionalInt number = value.asIntOpt();
        return number.isPresent() ? number.getAsInt() : null;
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        java.util.OptionalDouble number = value.asDoubleOpt();
        return number.isPresent() ? number.getAsDouble() : null;
    }

    // ═══════════════════════════════════════════════════════
    // Pagination — from Strapi meta
    // ═══════════════════════════════════════════════════════

    /**
     * Extract pagination metadata from a Strapi response.
     *
     * Expected format:
     * { "meta": { "pagination": { "page": 1, "pageSize": 15, "pageCount": 10, "total": 150 } } }
     *
     * @return int array: [page, pageSize, pageCount, total]
     */
    public static int[] extractPaginationMeta(JsonNode response) {
        if (response == null) return new int[]{1, 15, 1, 0};

        JsonNode pagination = response.path("meta").path("pagination");
        if (pagination.isMissingNode()) {
            return new int[]{1, 15, 1, 0};
        }

        int page = pagination.path("page").asInt(1);
        int pageSize = pagination.path("pageSize").asInt(15);
        int pageCount = pagination.path("pageCount").asInt(1);
        int total = pagination.path("total").asInt(0);

        return new int[]{page, pageSize, pageCount, total};
    }

    /**
     * Parse a paginated Strapi response into StrapiPageResponse of PublicationDTO.
     */
    public static StrapiPageResponse<PublicationDTO> publicationsPageFromJson(JsonNode response) {
        List<PublicationDTO> data = publicationsFromJson(response);
        int[] meta = extractPaginationMeta(response);
        return new StrapiPageResponse<>(data, meta[0], meta[1], meta[2], meta[3]);
    }

    /**
     * Parse a paginated Strapi response into StrapiPageResponse of PersonDTO.
     */
    public static StrapiPageResponse<PersonDTO> personsPageFromJson(JsonNode response) {
        List<PersonDTO> data = personsFromJson(response);
        int[] meta = extractPaginationMeta(response);
        return new StrapiPageResponse<>(data, meta[0], meta[1], meta[2], meta[3]);
    }

    /**
     * Parse a paginated Strapi response into StrapiPageResponse of PublisherDTO.
     */
    public static StrapiPageResponse<PublisherDTO> publishersPageFromJson(JsonNode response) {
        List<PublisherDTO> data = publishersFromJson(response);
        int[] meta = extractPaginationMeta(response);
        return new StrapiPageResponse<>(data, meta[0], meta[1], meta[2], meta[3]);
    }

    /**
     * Parse a paginated Strapi response into StrapiPageResponse of MagazineDTO.
     */
    public static StrapiPageResponse<MagazineDTO> magazinesPageFromJson(JsonNode response) {
        List<MagazineDTO> data = magazinesFromJson(response);
        int[] meta = extractPaginationMeta(response);
        return new StrapiPageResponse<>(data, meta[0], meta[1], meta[2], meta[3]);
    }
}
