package com.vajasoft.wayfarer;

import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;


public class Util {


    public static Window findWindow(Node ofNode) {
        Scene scene = ofNode != null ? ofNode.getScene() : null;
        return scene != null ? scene.getWindow() : null;
    }

    public static void closeWindow(Node ofNode) {
        Window window = findWindow(ofNode);
        if(window != null) {
            window.hide();
        }
    }

    private Util() {
    }

}
