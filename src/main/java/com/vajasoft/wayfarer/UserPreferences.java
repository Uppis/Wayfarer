package com.vajasoft.wayfarer;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.util.StringConverter;


public class UserPreferences {
    private final Preferences userPrefs;

    public UserPreferences(Class forClass) {
        userPrefs = Preferences.userNodeForPackage(forClass);
    }

    public String getPreference(String fromNode, String key, String defVal) {
        Preferences node = userPrefs.node(fromNode);
        return node.get(key, defVal);
    }

    public void putPreference(String toNode, String key, String val) throws BackingStoreException {
        Preferences node = userPrefs.node(toNode);
        node.put(key, val);
        node.flush();
    }

    public <T> List<T> loadRecentItems(String fromNode, StringConverter<T> converter) throws BackingStoreException {
        Preferences node = userPrefs.node(fromNode);
        String[] keys = node.keys();
        List<T> values = new ArrayList<>(keys.length);
        for (String key : keys) {
            String val = node.get(key, null);
            if (val != null) {
                values.add(converter.fromString(val));
            }
        }
//        Collections.sort(values);
        return values;
    }

    public <T> void saveRecentItems(List<T> items, String toNode, StringConverter<T> converter) throws BackingStoreException {
        Preferences node = userPrefs.node(toNode);
        node.clear();
        for (int i = 0; i < items.size(); i++) {
            node.put("item" + i, converter.toString(items.get(i)));
        }
        node.flush();
    }
}
