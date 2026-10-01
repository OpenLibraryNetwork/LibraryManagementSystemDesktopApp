package net.gizmolab.library.librarymanagementsystemdesktop.controller.picker;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Function;

/** ListView cells that show a text derived from the item. */
public final class Cells {

    private Cells() {}

    public static <T> Callback<ListView<T>, ListCell<T>> text(Function<T, String> toText) {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : toText.apply(item));
            }
        };
    }
}
