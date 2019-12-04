package com.vajasoft.wayfarer;

import static com.vajasoft.wayfarer.Logger.LOG;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Pertti Uppala
 */
public class Wayfarer extends Application {
    AppContext ctx;

    static {
        System.setProperty("java.util.logging.config.file", "logging.properties");
    }

    @Override
    public void start(Stage stage) throws Exception {
        ctx = new AppContext(stage, getParameters());
        WayfarerPane controller = (WayfarerPane) setSceneContent(Wayfarer.class.getResource("WayfarerPane.fxml"), stage, 900, 600);
        setIcons(stage);
        controller.setAppContext(ctx);
        stage.addEventHandler(WindowEvent.ANY, controller::handleWindowEvent);
        stage.show();
    }

    private Initializable setSceneContent(URL fromFxml, Stage toStage, double width, double height) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(fromFxml);
        Parent root = (Parent) loader.load();
        Scene scene = new Scene(root, width, height);
        toStage.setScene(scene);
        return (Initializable) loader.getController();
    }

    private void setIcons(Stage stage) {
        String[] iconFileNames = {"img/duke_26x48.png", "img/duke_44x80.png", "img/duke_48x48.png", "img/duke_80x80.png"};
        for (String fn : iconFileNames) {
            InputStream is = getClass().getResourceAsStream(fn);
            if (is != null) {
                stage.getIcons().add(new Image(is));
            }
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the
     * application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans
     * ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        LOG.info("Default Uncaught Exception Handler Set");
        launch(args);
    }
    
    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LOG.log(Level.SEVERE, String.valueOf(t), e);
        }
    }
}
