package com.vajasoft.wayfarer;

import java.nio.file.Path;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

/**
 *
 * @author Z705692
 */
public class FileActionDisabled extends SimpleBooleanProperty {
    ObservableList selectedItems;
    
    public FileActionDisabled(TableView table) {
        selectedItems = table.getSelectionModel().getSelectedItems();
        selectedItems.addListener(this::listChanged);
    }

    @Override
    public boolean get() {
        return selectedItems.isEmpty();
    }

    public void listChanged(Change<? extends Path> change) {
        fireValueChangedEvent();
    }
}
