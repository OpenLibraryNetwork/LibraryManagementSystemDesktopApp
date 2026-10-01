package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** "Πιο δημοφιλή" on the dashboard: borrow counts per publication (two books may share a title). */
public final class PopularPublications {

    /** publicationId is null only for old borrows that know just the title. */
    public record Entry(Long publicationId, String title, long count) {}

    private PopularPublications() {}

    public static List<Entry> top(List<BorrowDTO> borrows, int limit) {
        Map<String, Entry> byPublication = new LinkedHashMap<>();
        for (BorrowDTO b : borrows) {
            Long id = b.getStrapiPublicationId();
            String title = b.getPublicationTitle();
            if (id == null && title == null) continue;
            // id and title together: an old borrow may carry an id that today belongs to another book
            String key = id + "|" + (title == null ? "" : SearchText.normalize(title));
            Entry seen = byPublication.get(key);
            byPublication.put(key, new Entry(id, seen != null && seen.title() != null ? seen.title() : title,
                    seen == null ? 1 : seen.count() + 1));
        }
        List<Entry> ranked = new ArrayList<>(byPublication.values());
        ranked.sort(Comparator.comparingLong(Entry::count).reversed()
                .thenComparing(e -> SearchText.normalize(e.title() == null ? "" : e.title()))
                .thenComparing(e -> e.publicationId() == null ? Long.MAX_VALUE : e.publicationId()));
        return ranked.subList(0, Math.min(limit, ranked.size()));
    }

    /**
     * A borrow keeps the title it had when it was made; if today's catalog record with that id has another
     * title (e.g. borrows from before the catalog was rebuilt), its cover belongs to another book.
     */
    public static boolean coverBelongsTo(String borrowedTitle, String catalogTitle) {
        return borrowedTitle != null && catalogTitle != null
                && SearchText.normalize(borrowedTitle).equals(SearchText.normalize(catalogTitle));
    }
}
