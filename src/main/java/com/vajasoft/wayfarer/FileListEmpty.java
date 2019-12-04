package com.vajasoft.wayfarer;

import java.nio.file.Path;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/**
 *
 * @author Z705692
 */
public class FileListEmpty extends SimpleBooleanProperty {
    boolean isEmpty;
    
    public FileListEmpty(ObservableList list) {
        isEmpty = list.isEmpty();
        list.addListener(this::listChanged);
    }

    @Override
    public boolean get() {
        return isEmpty;
    }

    public void listChanged(Change<? extends Path> change) {
        isEmpty = change.getList().isEmpty();
        fireValueChangedEvent();
    }
}
