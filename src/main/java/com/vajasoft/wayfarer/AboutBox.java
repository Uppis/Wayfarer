package com.vajasoft.wayfarer;

//import com.sun.glass.ui.Screen;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeSet;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 *
 * @author Z705692
 */
public class AboutBox extends Dialog<Void> {

    private final ResourceBundle bundle = ResourceBundle.getBundle("com/vajasoft/wayfarer/res/AboutBox");
    private final Node parent;
    private final Package applPackage;
    private Window owner;

    public AboutBox(Node parent, Class application) {
        super();
        this.parent = parent;
        this.applPackage = application.getPackage();
        setOnShowing(this::onShowing);
        setResizable(true);
    }

    private void onShowing(DialogEvent e) {
        if (owner == null) {
            owner = Util.findWindow(parent);
            initOwner(owner);
        }
        setTitle(bundle.getString("title." + getClass().getSimpleName()));
        ButtonBar.ButtonData bd = ButtonBar.ButtonData.FINISH;
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().clear();
        dialogPane.getButtonTypes().add(new ButtonType(bundle.getString("button." + bd), bd));
        dialogPane.setContent(createContent());
    }

    private Node createContent() {
        Tab tab1 = createTab1();
        Tab tab2 = createTab2();
        Tab tab4 = createTab4();
        Tab tab5 = createTab5();
        TabPane ret = new TabPane(tab1, tab2, tab4, tab5);
        return ret;
    }

    private Tab createTab1() {
        ProcessHandle current = ProcessHandle.current();
        ProcessHandle.Info info = current.info();
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(24);
        grid.setVgap(8);
        int row = 0;
        String implVersion = applPackage.getImplementationVersion();
        addRow(grid, row++, "label.application.version", new Label(implVersion != null ? implVersion : "?"));
        addRow(grid, row++, "label.application.folder", new Label(new File(".").getAbsolutePath()));
        addRow(grid, row++, "label.process.pid", new Label(String.valueOf(current.pid())));
        addRow(grid, row++, "label.process.starttime", new Label(getStartTimeFormatted(info)));
        addRow(grid, row++, "label.java.vm", new Label(System.getProperty("java.vm.name")));
        addRow(grid, row++, "label.java.version", new Label(System.getProperty("java.version")));
        addRow(grid, row++, "label.javafx.version", new Label(System.getProperty("javafx.version")));
        addRow(grid, row++, "label.java.home", new Label(System.getProperty("java.home")));
        addRow(grid, row++, "label.file.encoding", new Label(System.getProperty("file.encoding")));
        addRow(grid, row++, "label.user.language", new Label(System.getProperty("user.language")));
        addRow(grid, row++, "label.user.language.format", new Label(System.getProperty("user.language.format")));
        addRow(grid, row++, "label.locale.default", new Label(Locale.getDefault().toString()));
        Tab ret = new Tab(bundle.getString("label.tab1"), grid);
        ret.setClosable(false);
        return ret;
    }

    private String getStartTimeFormatted(ProcessHandle.Info info) {
        return info.startInstant().isPresent() ? Util.getDateTimeFormatted(info.startInstant().get()) : "";
    }

    private Tab createTab2() {
//        TextArea text = new TextArea(String.valueOf(Screen.getMainScreen()));
        Screen primary = Screen.getPrimary();
        Rectangle2D bounds = primary.getBounds();
        StringBuilder buf = new StringBuilder();
        buf.append("\nBounds:\t").append(bounds.getWidth()).append(" x ").append(bounds.getHeight());
        buf.append('\n').append("Dpi:\t\t").append(primary.getDpi());
        addScale(primary, buf);
        TextArea text = new TextArea(buf.toString());
        Tab ret = new Tab(bundle.getString("label.tab2"), text);
        ret.setClosable(false);
        return ret;
    }

    private Tab createTab4() {
        StringBuilder buf = new StringBuilder();
        Properties sysProps = System.getProperties();
        for (String prop : new TreeSet<>(sysProps.stringPropertyNames())) {
            buf.append(prop).append(" = ").append(sysProps.getProperty(prop)).append('\n');
        }
        TextArea text = new TextArea(buf.toString());
        Tab ret = new Tab(bundle.getString("label.tab4"), text);
        ret.setClosable(false);
        return ret;
    }

    private Tab createTab5() {
        StringBuilder buf = new StringBuilder();
        Map<String, String> env = System.getenv();
        for (String envVar : new TreeSet<>(env.keySet())) {
            buf.append(envVar).append(" = ").append(env.get(envVar)).append('\n');
        }
        TextArea text = new TextArea(buf.toString());
        Tab ret = new Tab(bundle.getString("label.tab5"), text);
        ret.setClosable(false);
        return ret;
    }

    private void addRow(GridPane to, int row, String labelKey, Node data) {
        Label label = new Label(bundle.getString(labelKey));
        Font orig = label.getFont();
        Font bold = Font.font(orig.getFamily(), FontWeight.BOLD, orig.getSize());
        label.setFont(bold);
        to.addRow(row, label, data);
    }

    private void addScale(Screen screen, StringBuilder to) {
        try {
            Method method = Screen.class.getDeclaredMethod("getOutputScaleX");
            double scale = (Double) method.invoke(screen);
            to.append('\n').append("Scale X:\t").append(scale);
            method = Screen.class.getDeclaredMethod("getOutputScaleY");
            scale = (Double) method.invoke(screen);
            to.append('\n').append("Scale Y:\t").append(scale);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
        }
    }
}
