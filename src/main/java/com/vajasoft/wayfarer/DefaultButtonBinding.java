package com.vajasoft.wayfarer;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.TableView;

/**
 *
 * @author Z705692
 */
class DefaultButtonBinding extends BooleanBinding {
    
    private final TableView table;

    DefaultButtonBinding(TableView table) {
        this.table = table;
        bind(table.focusedProperty());
        bind(table.getItems());
    }

    @Override
    protected boolean computeValue() {
        return !table.isFocused() || table.getItems().isEmpty();
    }
    
}
