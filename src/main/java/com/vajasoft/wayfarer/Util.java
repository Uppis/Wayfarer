package com.vajasoft.wayfarer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.scene.Node;
import javafx.scene.Scene;
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

    private Util() {
    }

}
