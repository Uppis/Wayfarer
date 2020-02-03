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
public class SingleFileActionDisabled extends SimpleBooleanProperty {
    ObservableList selectedItems;
    
    public SingleFileActionDisabled(TableView table) {
        selectedItems = table.getSelectionModel().getSelectedItems();
        selectedItems.addListener(this::listChanged);
    }

    @Override
    public boolean get() {
        return selectedItems.size() != 1;
    }

    public void listChanged(Change<? extends Path> change) {
        fireValueChangedEvent();
    }
}
