package com.vajasoft.wayfarer;

import static com.vajasoft.wayfarer.Logger.LOG;
import static com.vajasoft.wayfarer.Logger.supply;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

/**
 *
 * @author Pertti Uppala
 */
public class WayfarerPane implements Initializable {

    private static final String PREF_NODE_RECENT_ROOTS = "recentroots";
    private static final String PREF_NODE_RECENT_FILE_MASKS = "recentmasks";
    private static final String PREF_NODE_RECENT_SEARCH_TEXTS = "recentsearches";

    private static final StringConverter<File> FILE_TO_STRING_CONVERTER = new FileToStringConverter();
    private static final StringConverter<String> DUMMY_STRING_CONVERTER = new DummyStringConverter();
    private static final File CURRENT_FOLDER = new File(".");

    private Window mainWindow;
    private AboutBox aboutBox;

    private final NumberFormat sizeFormatter = NumberFormat.getNumberInstance();
    private final UserPreferences userPrefs = new UserPreferences(WayfarerPane.class);
    private AppContext ctx;
    private DirectoryChooser dirChooser;
    private SearchTask searcher;
    private SearchResult searchResults;

    @FXML
    private Pane pnlWayfarer;
    @FXML
    private ComboBox<String> fldFileMask;
    @FXML
    private ComboBox<String> fldSearchText;
    @FXML
    private ComboBox<File> fldRoot;
    @FXML
    private Button cmdStart;
    @FXML
    private Button cmdStop;
    @FXML
    private CheckBox optSearchTextCaseSensitive;
    @FXML
    private TableView<Path> lstFilesFound;
    @FXML
    private MenuItem mnuOpenMatchedFile;
    @FXML
    private TextArea fldHits;
    @FXML
    private TextArea fldSummary;
    @FXML
    private HBox statusBar;
    @FXML
    private Label fldNbrOfFiles;
    @FXML
    private Label fldStatus;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        aboutBox = new AboutBox(pnlWayfarer, getClass());
        sizeFormatter.setMaximumFractionDigits(1);
        dirChooser = new DirectoryChooser();
//        searchResults = new HashMap<>();
        fldRoot.setConverter(FILE_TO_STRING_CONVERTER);
        getRecentCriteria();
        lstFilesFound.getColumns().forEach((TableColumn<Path, ?> t) -> {
            ((TableColumn<Path, String>) t).setCellValueFactory(WayfarerPane::getCellValue);
        });
        lstFilesFound.getSelectionModel().selectedItemProperty().addListener(this::showHits);
        mnuOpenMatchedFile.disableProperty().bind(new FileListEmpty(lstFilesFound.getItems()));
    }

    void setAppContext(AppContext ctx) {
        this.ctx = ctx;
        Application.Parameters params = ctx.getParameters();
        if (params != null && !params.getUnnamed().isEmpty()) {
            File arg = new File(params.getUnnamed().get(0));
            if (arg.exists() && arg.isDirectory()) {
                fldRoot.setValue(arg);
            }
        }
    }

    private static ObservableValue<String> getCellValue(TableColumn.CellDataFeatures<Path, String> cellData) {
        Path path = cellData.getValue();
        switch (cellData.getTableColumn().getId()) {
            case "filename":
                return new SimpleStringProperty(path.getFileName().toString());
            case "folder":
                return new SimpleStringProperty(path.getParent().toString());
        }
        return null;
    }

    private void showHits(ObservableValue<? extends Path> ov, Path oldValue, Path newValue) {
        fldHits.clear();
        List<String> hits = searchResults.getFilesHit().get(newValue);
        if (hits != null) {
            hits.forEach((String t) -> fldHits.appendText(t + "\n"));
        }
    }

    private void showSummary() {
        fldSummary.clear();
        fldSummary.appendText("Search Statistics" + "\n");
        fldSummary.appendText("\n");
        fldSummary.appendText("Visited\t" + searchResults.getNbrofFilesVisited() + " files\n");
        fldSummary.appendText("Searched\t" + searchResults.getNbrofFilesSearched() + " files\n");
        fldSummary.appendText("Hits in:\t" + searchResults.getNbrofFilesHit() + " files\n");
        fldSummary.appendText("Total\t" + searchResults.getTotalNbrofHits() + " hits\n");
        fldSummary.appendText("\n");
        fldSummary.appendText("Search time:\t" + searchResults.getSearchTime() + " ms\n");
    }

    @FXML
    private void onExit(ActionEvent ev) {
        doExit();
    }

    @FXML
    private void onAbout(ActionEvent event) {
        aboutBox.showAndWait();
    }

    @FXML
    private void onMnuOpenMatchedFile(ActionEvent event) {
        openSelectedFile();
    }

    @FXML
    private void onCmdBrowseRoot(ActionEvent event) {
        File root = getRootFolder();
        if (root != dirChooser.getInitialDirectory()) {
            dirChooser.setInitialDirectory(root);
        }
        File choice = dirChooser.showDialog(ctx.getMainWindow());
        if (choice != null) {
            fldRoot.setValue(choice);
        }
    }

    @FXML
    private void onCmdStart(ActionEvent event) {
//        cmdStart.setDisable(true);
        fldSearchText.setValue(fldSearchText.getEditor().getText());
        searchResults = new SearchResult();
        lstFilesFound.getItems().clear();
        searcher = new SearchTask(
                getRootFolder().toPath(),
                getFileMask(),
                fldSearchText.getValue(),
                optSearchTextCaseSensitive.isSelected(),
                searchResults,
                lstFilesFound.getItems());
        searcher.setWorkerStateEventHandler(this::handleWorkerStateEvent);
        Thread th = new Thread(searcher);
        th.setDaemon(true);
        th.start();
        storeCriteria(fldRoot);
        storeCriteria(fldFileMask);
        storeCriteria(fldSearchText);
//        cmdStart.setDisable(false);
    }

    @FXML
    private void onCmdStop(ActionEvent event) {
        searcher.cancelSearch();
    }

    @FXML
    private void onLstFilesFoundDblClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            openSelectedFile();
        }
    }

    private <T> void storeCriteria(ComboBox<T> combo) {
        T value = combo.getValue();
        ObservableList<T> items = combo.getItems();
        int ix = items.indexOf(value);
        if (ix > 0) {
            items.remove(value);
        }
        if (ix != 0) {
            items.add(0, value);
            combo.getSelectionModel().select(value);
        }
    }

    private File getRootFolder() {
        File ret = fldRoot.getValue();
        if (ret == null) {
            ret = CURRENT_FOLDER;
        }
        return ret;
    }

    private String getFileMask() {
        String ret = fldFileMask.getValue();
        if (ret == null) {
            ret = "*.*";
        }
        return ret;
    }

    private void getRecentCriteria() {
        try {
            initCombo(fldFileMask, PREF_NODE_RECENT_FILE_MASKS, DUMMY_STRING_CONVERTER);
            initCombo(fldSearchText, PREF_NODE_RECENT_SEARCH_TEXTS, DUMMY_STRING_CONVERTER);
            initCombo(fldRoot, PREF_NODE_RECENT_ROOTS, FILE_TO_STRING_CONVERTER);
        } catch (BackingStoreException ex) {
            LOG.warning(supply("Failed to load preferences: {0}", ex));
        }
    }

    private void saveRecentCriteria() {
        try {
            userPrefs.saveRecentItems(fldFileMask.getItems(), PREF_NODE_RECENT_FILE_MASKS, DUMMY_STRING_CONVERTER);
            userPrefs.saveRecentItems(fldSearchText.getItems(), PREF_NODE_RECENT_SEARCH_TEXTS, DUMMY_STRING_CONVERTER);
            userPrefs.saveRecentItems(fldRoot.getItems(), PREF_NODE_RECENT_ROOTS, FILE_TO_STRING_CONVERTER);
        } catch (BackingStoreException ex) {
            String msg = "Failed to save preferences: " + ex;
            new Alert(Alert.AlertType.WARNING, msg).showAndWait();
            LOG.warning(msg);
        }
    }

    private <T> void initCombo(ComboBox<T> combo, String prefName, StringConverter<T> prefConverter) throws BackingStoreException {
        ObservableList<T> content = FXCollections.observableArrayList(userPrefs.loadRecentItems(prefName, prefConverter));
        combo.setItems(content);
        if (!content.isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    private void doExit() {
        saveRecentCriteria();
        Util.closeWindow(pnlWayfarer);
    }

    void handleWindowEvent(WindowEvent t) {
        if (WindowEvent.WINDOW_SHOWING.equals(t.getEventType())) {
            mainWindow = Util.findWindow(pnlWayfarer);
            fldFileMask.requestFocus();
        } else if (WindowEvent.WINDOW_CLOSE_REQUEST.equals(t.getEventType())) {
            doExit();
        }
    }

    void handleWorkerStateEvent(WorkerStateEvent t) {
        EventType etype = t.getEventType();
        if (WorkerStateEvent.WORKER_STATE_RUNNING.equals(etype)) {
            LOG.log(Level.INFO, "Worker started");
            setSearchState(true);
        } else if (WorkerStateEvent.WORKER_STATE_SUCCEEDED.equals(etype)) {
            LOG.log(Level.INFO, "Worker succeeded");
            setSearchState(false);
        } else if (WorkerStateEvent.WORKER_STATE_FAILED.equals(etype)) {
            Throwable ex = t.getSource().getException();
            if (ex != null) {
                LOG.log(Level.WARNING, "Worker failed", ex);
            } else {
                LOG.log(Level.WARNING, "Worker failed");
            }
            setSearchState(false);
        } else if (WorkerStateEvent.WORKER_STATE_CANCELLED.equals(etype)) {
            LOG.log(Level.INFO, "Worker cancelled");
//            searcher.cancelSearch();
            setSearchState(false);
        }
    }

    private void setSearchState(boolean searching) {
        mainWindow.getScene().setCursor(searching ? Cursor.WAIT : Cursor.DEFAULT);
        cmdStart.setDisable(searching);
        cmdStop.setDisable(!searching);
        if (!searching) {
            showSummary();
            if (!lstFilesFound.getItems().isEmpty()) {
                lstFilesFound.getSelectionModel().selectFirst();
                lstFilesFound.requestFocus();
            }
        }
    }

    private void openSelectedFile() {
        Path selectedItem = lstFilesFound.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            try {
                Desktop.getDesktop().open(selectedItem.toFile());
            } catch (IOException ex) {
                Logger.LOG.warning(supply("Open file failed: ", ex));
            }
        }
    }
}
