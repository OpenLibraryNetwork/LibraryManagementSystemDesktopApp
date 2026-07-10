package net.gizmolab.library.librarymanagementsystemdesktop.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class που παρέχει searchable (φιλτραρίσιμο) ComboBox functionality.
 */
public class SearchableComboBoxHelper {

    /**
     * Κάνει ένα ComboBox searchable με case-insensitive φιλτράρισμα.
     *
     * @param comboBox      Το ComboBox που θα γίνει searchable
     * @param displayMapper Συνάρτηση που μετατρέπει T σε String για σύγκριση
     * @param <T>           Ο τύπος των στοιχείων του ComboBox
     */
    public static <T> void makeSearchable(ComboBox<T> comboBox,
                                          Function<T, String> displayMapper) {
        comboBox.setEditable(true);

        // Η πλήρης λίστα όλων των στοιχείων
        List<T> allItems = new ArrayList<>();

        boolean[] filtering = {false};

        // Helper: sync allItems από το τρέχον items list
        Runnable syncAllItems = () -> {
            allItems.clear();
            allItems.addAll(comboBox.getItems());
        };

        // Helper: αντικαθιστά τα items ΧΩΡΙΣ να χάσει το value ή το κείμενο του χρήστη
        java.util.function.Consumer<List<T>> replaceItems = (newItems) -> {
            T savedValue = comboBox.getValue();
            String savedText = comboBox.getEditor().getText();
            int caretPosition = comboBox.getEditor().getCaretPosition();
            filtering[0] = true;
            try {
                comboBox.getItems().setAll(newItems);
                if (savedValue != null && comboBox.getValue() == null) {
                    comboBox.setValue(savedValue);
                }
                // Restore the text typed by user and their cursor position
                if (savedText != null && !savedText.equals(comboBox.getEditor().getText())) {
                    comboBox.getEditor().setText(savedText);
                    comboBox.getEditor().positionCaret(caretPosition);
                }
            } finally {
                filtering[0] = false;
            }
        };

        // Ακούμε εξωτερικές αλλαγές στο items list (async loads)
        comboBox.getItems().addListener((ListChangeListener<T>) change -> {
            if (!filtering[0]) {
                syncAllItems.run();
            }
        });

        // Ακούμε και αλλαγές στο items reference (setItems)
        comboBox.itemsProperty().addListener((obs, oldList, newList) -> {
            if (newList != null) {
                newList.addListener((ListChangeListener<T>) change -> {
                    if (!filtering[0]) {
                        syncAllItems.run();
                    }
                });
                if (!filtering[0]) {
                    syncAllItems.run();
                }
            }
        });

        // Seed αρχικά
        syncAllItems.run();

        // ΚΡΙΣΙΜΟ: Θέτουμε StringConverter που κάνει σωστό fromString().
        // Σε editable ComboBox, όταν ο editor χάνει focus, το JavaFX καλεί
        // fromString(editorText) για να μετατρέψει το text σε value.
        // Αν fromString() επιστρέψει null, το value μηδενίζεται.
        javafx.util.StringConverter<T> existingConverter = comboBox.getConverter();

        // Η display function: χρησιμοποιεί τον υπάρχοντα converter αν υπάρχει,
        // αλλιώς τον displayMapper. Αυτή χρησιμοποιείται για toString/fromString/guard.
        Function<T, String> toDisplayString = (item) -> {
            if (item == null) return "";
            if (existingConverter != null) {
                String s = existingConverter.toString(item);
                return s != null ? s : "";
            }
            return displayMapper.apply(item);
        };

        comboBox.setConverter(new javafx.util.StringConverter<T>() {
            @Override
            public String toString(T item) {
                return toDisplayString.apply(item);
            }

            @Override
            public T fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                // Πρώτα ελέγχουμε αν το τρέχον value ταιριάζει
                T current = comboBox.getValue();
                if (current != null && toDisplayString.apply(current).equals(string)) {
                    return current;
                }
                // Ψάχνουμε στο allItems για exact match βάσει display string
                for (T item : allItems) {
                    if (toDisplayString.apply(item).equals(string)) {
                        return item;
                    }
                }
                // Αν δεν βρεθεί, επιστρέφουμε το τρέχον value (μην μηδενίσεις)
                return current;
            }
        });

        // Listener στο editor για φιλτράρισμα
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (filtering[0]) return;
            T selected = comboBox.getValue();
            // Guard: αν το text ταιριάζει με το display string του επιλεγμένου
            if (selected != null && toDisplayString.apply(selected).equals(newText)) {
                return;
            }

            List<T> toShow;
            if (newText == null || newText.isEmpty()) {
                toShow = new ArrayList<>(allItems);
            } else {
                // Φιλτράρισμα: χρησιμοποιούμε τον displayMapper (μπορεί να περιέχει
                // επιπλέον πεδία για search, π.χ. τηλέφωνο)
                String lower = newText.toLowerCase();
                toShow = allItems.stream()
                        .filter(item -> displayMapper.apply(item).toLowerCase().contains(lower))
                        .collect(Collectors.toList());
            }
            replaceItems.accept(toShow);

            if (!comboBox.isShowing()) {
                comboBox.show();
            }
        });

        // Όταν επιλέγεται στοιχείο, επαναφέρουμε την πλήρη λίστα και γράφουμε το display text
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (filtering[0]) return;
            if (newVal != null) {
                replaceItems.accept(new ArrayList<>(allItems));
                comboBox.getEditor().setText(toDisplayString.apply(newVal));
            }
        });
    }

    /**
     * Φιλτράρει μια λίστα στοιχείων βάσει query (case-insensitive contains).
     * Χρησιμοποιείται από property tests.
     *
     * @param items         Η λίστα στοιχείων προς φιλτράρισμα
     * @param displayMapper Συνάρτηση που μετατρέπει T σε String για σύγκριση
     * @param query         Το κείμενο αναζήτησης
     * @param <T>           Ο τύπος των στοιχείων
     * @return Φιλτραρισμένη λίστα στοιχείων
     */
    public static <T> List<T> filterItems(List<T> items, Function<T, String> displayMapper, String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(items);
        }
        String lower = query.toLowerCase();
        return items.stream()
                .filter(item -> displayMapper.apply(item).toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }
}
