package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.SubjectDTO;

import java.util.List;

/** Client-side filtering of the (small) DDC subject list. */
public final class SubjectFilter {

    private SubjectFilter() {}

    public static String label(SubjectDTO s) {
        String ddc = s.getSubjectDDC();
        return (ddc == null || ddc.isBlank()) ? s.getSubjectTitle() : ddc + " " + s.getSubjectTitle();
    }

    public static List<SubjectDTO> filter(List<SubjectDTO> all, String query) {
        return all.stream().filter(s -> SearchText.matches(label(s), query)).toList();
    }
}
