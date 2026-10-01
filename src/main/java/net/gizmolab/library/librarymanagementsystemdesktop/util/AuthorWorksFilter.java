package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;

import java.util.List;

/**
 * Strapi does not guarantee that two conditions on the same repeatable component
 * (person AND role) match the same row, so the "author" part is checked here.
 */
public final class AuthorWorksFilter {

    private AuthorWorksFilter() {}

    public static List<PublicationDTO> authoredBy(List<PublicationDTO> publications, Long personId) {
        return publications.stream()
                .filter(pub -> pub.getContributors().stream().anyMatch(c ->
                        personId.equals(c.getPerson().getId()) && c.getRole().isAuthor()))
                .toList();
    }
}
