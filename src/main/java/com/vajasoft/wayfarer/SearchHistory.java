package com.vajasoft.wayfarer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;

/**
 *
 * @author Z705692
 */
public class SearchHistory {
    private final List<SearchCriteria> history = new ArrayList<>();
    private int cursor = -1;
    private final NavigationProperty eohProperty = new NavigationProperty(this, "eohProperty", () -> {
        return cursor >= history.size() - 1;
    });
    private final NavigationProperty bohProperty =  new NavigationProperty(this, "bohProperty", () -> {
        return cursor <= 0;
    });

    public ReadOnlyBooleanProperty eohProperty() {
        return eohProperty;
    }

    public ReadOnlyBooleanProperty bohProperty() {
        return bohProperty;
    }

    public void clear() {
        history.clear();
        cursor = -1;
    }

    public void maybeAddSearch(SearchCriteria crit) {
        if (history.isEmpty() || !history.get(cursor).equals(crit)) {
            history.add(crit);
            cursor = history.size() - 1;
            eohProperty.invalidate();
            bohProperty.invalidate();
        }
    }

    public boolean hasPrev() {
        return !bohProperty.get();
    }

    public SearchCriteria getPrev() {
        if (hasPrev()) {
            SearchCriteria crit = history.get(--cursor);
            eohProperty.invalidate();
            bohProperty.invalidate();
            return crit;
        }
        throw new IndexOutOfBoundsException("Beginning of history");
    }

    public boolean hasNext() {
        return !eohProperty.get();
    }

    public SearchCriteria getNext() {
        if (hasNext()) {
            SearchCriteria crit = history.get(++cursor);
            eohProperty.invalidate();
            bohProperty.invalidate();
            return crit;
        }
        throw new IndexOutOfBoundsException("End of history");
    }

    private static class NavigationProperty extends ReadOnlyBooleanPropertyBase {
        private final Object bean;
        private final Supplier<Boolean> supplier;
        private final String name;

        public NavigationProperty(Object bean, String name, Supplier<Boolean> supplier) {
            this.bean = bean;
            this.supplier = supplier;
            this.name = name;
        }

        @Override
        public boolean get() {
            return supplier.get();
        }

        @Override
        public Object getBean() {
            return bean;
        }

        @Override
        public String getName() {
            return name;
        }
        
        void invalidate() {
            fireValueChangedEvent();
        }
    }
}
