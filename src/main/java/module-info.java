module Wayfarer {
    requires java.desktop;
    requires java.logging;
    requires java.prefs;
    requires javafx.controls;
    requires javafx.base;
    requires javafx.fxml;
    requires javafx.web;
    exports com.vajasoft.wayfarer to javafx.graphics;
    opens com.vajasoft.wayfarer to javafx.fxml;
}
