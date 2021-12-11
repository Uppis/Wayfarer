package com.vajasoft.wayfarer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Window;

public class Util {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getVersion(Class cls) {
        String ret = "?";
        Package pkg = cls.getPackage();
        if (pkg != null) {
            String implVersion = pkg.getImplementationVersion();
            if (implVersion != null) {
                ret = implVersion;
            }
        }
        return ret;
    }

    public static Window findWindow(Node ofNode) {
        Scene scene = ofNode != null ? ofNode.getScene() : null;
        return scene != null ? scene.getWindow() : null;
    }

    public static void closeWindow(Node ofNode) {
        Window window = findWindow(ofNode);
        if (window != null) {
            window.hide();
        }
    }

    public static String getDateTimeFormatted(Instant dateTime) {
        return LocalDateTime.ofInstant(dateTime, ZoneId.systemDefault()).format(Util.DATE_TIME_FORMATTER);
    }

    static <T> void initCombo(ComboBox<T> combo, List<T> from) throws BackingStoreException {
        ObservableList<T> content = FXCollections.observableArrayList(from);
        combo.setItems(content);
        if (!content.isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    static <T> void rememberValue(ComboBox<T> combo, int maxItems) {
        T value = combo.getValue();
        if (value != null) {
            ObservableList<T> items = combo.getItems();
            int ix = items.indexOf(value);
            if (ix != 0) {
                // no need to do anything, if value already in the beginning of the list
                if (ix > 0) {
                    // value found further down the list => move up to first
                    items.remove(value);
                } else if (items.size() == maxItems) {
                    items.remove(items.size() - 1); // remove last
                }
                items.add(0, value);
                combo.getSelectionModel().select(value);
            }
        }
    }

    static String matchedLinesToRichText(List<MatchedLine> lines) {
        StringBuilder buf = new StringBuilder();
        if (!lines.isEmpty()) {
            String hiliteStyle = "hilite";
            buf.append("<style>").append(hiliteStyle).append("{font-style:normal; font-weight:900; color:red;}</style>");
            buf.append("<pre>");
            for (MatchedLine line : lines) {
                buf.append(Util.lineToText(line, hiliteStyle));
            }
            buf.append("</pre>");
        }
        return buf.toString();
    }

    private static String lineToText(MatchedLine line, String hilite) {
        StringBuilder buf = new StringBuilder();
        buf.append(String.valueOf(line.getLineNbr()));
        buf.append("\t");
        Iterator<Integer[]> matches = line.getMatches().iterator();
        Integer[] match = matches.hasNext() ? matches.next() : null;
        String li = line.getLine();
        for (int i = 0; i < li.length(); i++) {
            if (match != null && i == match[0]) {
                buf.append("<").append(hilite).append(">");
            }
            char ch = li.charAt(i);
            if (ch == '&') {
                buf.append("&amp;");
            } else if (ch == '<') {
                buf.append("&lt;");
            } else if (ch == '>') {
                buf.append("&gt;");
            } else {
                buf.append(ch);
            }
            if (match != null && i == match[1]) {
                buf.append("</").append(hilite).append(">");
                match = matches.hasNext() ? matches.next() : null;
            }
        }
        buf.append("\n");
        return buf.toString();
    }

    private Util() {
    }
}
