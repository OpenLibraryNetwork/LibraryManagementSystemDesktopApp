package net.gizmolab.library.librarymanagementsystemdesktop.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

/**
 * Utility class for creating custom TableView cell factories.
 * Provides factories for text cells with ellipsis/tooltip support and numeric cells with right alignment.
 */
public final class TableCellFactory {

    private TableCellFactory() {}

    /**
     * Creates a cell factory that displays text with ellipsis when truncated
     * and shows full text in tooltip on hover.
     * 
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the content in all cells in this TableColumn
     * @return A callback that creates TableCell instances with ellipsis and tooltip support
     */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> createTextCellFactory() {
        return column -> new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    try {
                        String text = item.toString();
                        setText(text);
                        
                        // Set ellipsis style for text overflow
                        setStyle("-fx-text-overrun: ellipsis;");
                        
                        // Add tooltip with full text
                        Tooltip tooltip = new Tooltip(text);
                        tooltip.setWrapText(true);
                        tooltip.setMaxWidth(400);
                        setTooltip(tooltip);
                    } catch (Exception _) {
                        // Fallback to simple text rendering on error
                        setText(item.toString());
                        setTooltip(null);
                    }
                }
            }
        };
    }

    /**
     * Creates a cell factory for numeric values with right alignment.
     * 
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the numeric content (must extend Number)
     * @return A callback that creates TableCell instances with right-aligned numeric values
     */
    public static <S, T extends Number> Callback<TableColumn<S, T>, TableCell<S, T>> createNumericCellFactory() {
        return column -> new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        setText(item.toString());
                        setStyle("-fx-alignment: CENTER-RIGHT;");
                    } catch (Exception _) {
                        // Fallback to simple text rendering on error
                        setText(item.toString());
                    }
                }
            }
        };
    }
}
