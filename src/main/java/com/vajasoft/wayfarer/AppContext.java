package com.vajasoft.wayfarer;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 *
 * @author z705692
 */
public class AppContext {
    private final Application.Parameters parameters;
    private final Stage mainWindow;

    public AppContext(Stage mainWindow, Application.Parameters parameters) {
        this.parameters = parameters;
        this.mainWindow = mainWindow;
    }

    public Stage getMainWindow() {
        return mainWindow;
    }

    public Application.Parameters getParameters() {
        return parameters;
    }
}
