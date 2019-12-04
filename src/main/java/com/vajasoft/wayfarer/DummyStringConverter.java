package com.vajasoft.wayfarer;

import javafx.util.StringConverter;

/**
 *
 * @author Z705692
 */
class DummyStringConverter extends StringConverter<String> {
    @Override
    public String toString(String t) {
        return t;
    }

    @Override
    public String fromString(String string) {
        return string;
    }
}
