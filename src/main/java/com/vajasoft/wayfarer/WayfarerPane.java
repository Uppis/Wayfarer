package com.vajasoft.wayfarer;

import static com.vajasoft.wayfarer.Logger.LOG;
import static com.vajasoft.wayfarer.Logger.supply;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

/**
 *
 * @author Pertti Uppala
 */
public class WayfarerPane implements Initializable {

    private static final String PREF_NODE_SETTINGS = "settings";
    private static final String PREF_NODE_RECENT_ROOTS = "recentroots";
    private static final String PREF_NODE_RECENT_FILE_MASKS = "recentmasks";
    private static final String PREF_NODE_RECENT_SEARCH_TEXTS = "recentsearches";

    public static final KeyCodeCombination COPYKEY_CODE_COMBINATION = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

    private static final StringConverter<File> FILE_TO_STRING_CONVERTER = new FileToStringConverter();
    private static final StringConverter<String> DUMMY_STRING_CONVERTER = new DummyStringConverter();
    private static final File CURRENT_FOLDER = new File(".").getAbsoluteFile();

    private Window mainWindow;
    private AboutBox aboutBox;

    private final UserPreferences userPrefs = new UserPreferences(WayfarerPane.class);
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private final EventHandler<KeyEvent> keyEventHandler = this::handleKeyEvents;
    private AppContext ctx;
    private DirectoryChooser dirChooser;
    private SearchTask searcher;
    private SearchResult searchResults;
    private int maxItemsInCombos;
    private final SearchHistory history = new SearchHistory();

    @FXML
    private Pane pnlWayfarer;
    @FXML
    private ToolBar toolbar;
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
    private Button cmdBrowseRoot;
    @FXML
    private CheckBox optSearchTextCaseSensitive;
    @FXML
    private CheckBox optSearchWholeWords;
    @FXML
    private CheckBox optSearchTextRegex;
    @FXML
    private TableView<MatchedFile> lstFilesFound;
    @FXML
    private MenuItem mnuCopySelected;
    @FXML
    private MenuItem mnuCopyMatchedFilePath;
    @FXML
    private MenuItem mnuOpenMatchedFile;
    @FXML
    private MenuItem mnuOpenMatchedFileFolder;
    @FXML
    private WebView fldHits;
    @FXML
    private TextArea fldSummary;
    @FXML
    private Label fldNbrOfFiles;
    @FXML
    private Label fldStatus;
    @FXML
    private Button cmdBack;
    @FXML
    private Button cmdForward;

    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        aboutBox = new AboutBox(pnlWayfarer, getClass());
        dirChooser = new DirectoryChooser();
        fldRoot.setConverter(FILE_TO_STRING_CONVERTER);
        loadStateFromPrefs();
        lstFilesFound.getColumns().forEach((TableColumn<MatchedFile, ?> t) -> {
            ((TableColumn<MatchedFile, String>) t).setCellValueFactory(this::matchedFileCellValueFactory);
        });
        cmdStart.defaultButtonProperty().bind(new DefaultButtonBinding(lstFilesFound));
        lstFilesFound.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lstFilesFound.getSelectionModel().selectedItemProperty().addListener(this::selectHittedFile);
        lstFilesFound.setOnKeyPressed(keyEventHandler);

        mnuCopySelected.disableProperty().bind(new FileActionDisabled(lstFilesFound));
        SingleFileActionDisabled fileActionDisabled = new SingleFileActionDisabled(lstFilesFound);
        mnuCopyMatchedFilePath.disableProperty().bind(fileActionDisabled);
        mnuOpenMatchedFile.disableProperty().bind(fileActionDisabled);
        mnuOpenMatchedFileFolder.disableProperty().bind(fileActionDisabled);
//        optSearchTextRegex.disableProperty().bind(optSearchWholeWords.selectedProperty());
        optSearchWholeWords.selectedProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean oldV, Boolean newV) -> {
            setDependentOption(optSearchTextRegex, newV);
        });
        optSearchTextRegex.selectedProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean oldV, Boolean newV) -> {
            setDependentOption(optSearchWholeWords, newV);
        });
        cmdBack.disableProperty().bind(history.bohProperty());
        cmdForward.disableProperty().bind(history.eohProperty());
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

    private void handleKeyEvents(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            onOpenMatchedFile(new ActionEvent());
        } else if (COPYKEY_CODE_COMBINATION.match(e)) {
            onMnuCopySelected(null);
            e.consume();
        }
    }

    private ObservableValue<String> matchedFileCellValueFactory(TableColumn.CellDataFeatures<MatchedFile, String> cellData) {
        MatchedFile row = cellData.getValue();
        TableColumn col = cellData.getTableColumn();
        return new SimpleStringProperty(matchedFileToColumns(row, col));
    }

    private String matchedFileToColumns(MatchedFile row, TableColumn col) {
        switch (col.getId()) {
            case "filename":
                return row.getFile().getFileName().toString();
            case "folder":
                return row.getFile().getParent().toString();
            case "lastModified":
                return Util.getDateTimeFormatted(row.getAttrs().lastModifiedTime().toInstant());
            default:
                return "";
        }
    }

    private void selectHittedFile(ObservableValue<? extends MatchedFile> ov, MatchedFile oldValue, MatchedFile newValue) {
        fldHits.getEngine().loadContent("");
        fldStatus.setText("");
        if (newValue != null) {
            fldStatus.setText("Selected item " + newValue.getAttrs().size() + " bytes, " + newValue.getTotalNbrofHits() + " hits");
            fldHits.getEngine().loadContent(Util.matchedLinesToRichText(newValue.getLines()));
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
    private void onOpenMatchedFile(Event event) {
        if (!mnuOpenMatchedFile.isDisable()) {
            if (event instanceof ActionEvent || event instanceof MouseEvent && ((MouseEvent) event).getClickCount() == 2) {
                handleFileAction((f) -> {
                    Desktop.getDesktop().open(f);
                });
            }
        }
    }

    @FXML
    private void onMnuCopySelected(ActionEvent event) {
        List<TableColumn<MatchedFile, ?>> columns = lstFilesFound.getColumns();
        StringBuilder buf = new StringBuilder();
        boolean lineIsEmpty = true;
        for (MatchedFile f : lstFilesFound.getSelectionModel().getSelectedItems()) {
            for (TableColumn col : columns) {
                if (!lineIsEmpty) {
                    buf.append('\t');
                }
                buf.append(matchedFileToColumns(f, col));
                lineIsEmpty = false;
            }
            buf.append('\n');
            lineIsEmpty = true;
        }
        if (buf.length() > 0) {
            ClipboardContent content = new ClipboardContent();
            content.putString(buf.toString());
            clipboard.setContent(content);
        }
    }

    @FXML
    private void onMnuCopyMatchedFilePath(ActionEvent event) {
        handleFileAction((f) -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getAbsolutePath());
            clipboard.setContent(content);
        });
    }

    @FXML
    private void onMnuOpenMatchedFileFolder(ActionEvent event) {
        handleFileAction((f) -> {
            File parent = f.getParentFile();
            if (parent != null) {
                Desktop.getDesktop().open(parent);
            }
        });
    }

    @FXML
    private void onCmdBrowseRoot(ActionEvent event) {
        File root = getRootFolder();
        if (root != null && root != dirChooser.getInitialDirectory()) {
            dirChooser.setInitialDirectory(root);
        }
        File choice = dirChooser.showDialog(ctx.getMainWindow());
        if (choice != null) {
            fldRoot.setValue(choice);
        }
    }

    @FXML
    private void onCmdStart(ActionEvent event) {
        fldFileMask.commitValue();
        fldSearchText.commitValue();
        fldRoot.commitValue();
        searchResults = new SearchResult(this::showProgress);
        resetOutputs();
        SearchCriteria crit = new SearchCriteria(
                getRootFolder().toPath(),
                getFileMask(),
                false,
                fldSearchText.getValue(),
                optSearchTextCaseSensitive.isSelected(),
                optSearchWholeWords.isSelected(),
                optSearchTextRegex.isSelected()
        );
        searcher = new SearchTask(crit, searchResults);
        searcher.setWorkerStateEventHandler(this::handleWorkerStateEvent);
        Thread th = new Thread(searcher);
        th.setDaemon(true);
        th.start();
        Util.rememberValue(fldRoot, maxItemsInCombos);
        Util.rememberValue(fldFileMask, maxItemsInCombos);
        Util.rememberValue(fldSearchText, maxItemsInCombos);
        history.maybeAddSearch(crit);
    }

    private void resetOutputs() {
        lstFilesFound.getItems().clear();
        fldHits.getEngine().loadContent("");
        fldSummary.clear();
        fldStatus.setText("");
        fldNbrOfFiles.setText("");
    }

    @FXML
    private void onCmdStop(ActionEvent event) {
        searcher.cancelSearch();
    }

    @FXML
    private void onCmdBack(ActionEvent event) {
        if (history.hasPrev()) {
            SearchCriteria prev = history.getPrev();
            setSearchCriteria(prev);
        }
    }

    @FXML
    private void onCmdForward(ActionEvent event) {
        if (history.hasNext()) {
            SearchCriteria next = history.getNext();
            setSearchCriteria(next);
        }
    }

    private void showProgress(Path path, MatchedFile matchedFile) {
        Platform.runLater(() -> {
            if (matchedFile != null) {
                lstFilesFound.getItems().add(matchedFile);
                fldNbrOfFiles.setText(lstFilesFound.getItems().size() + " items");
            } else {
                fldStatus.setText("Visiting " + path.toString());
            }
        });
    }

    private File getRootFolder() {
        File ret = fldRoot.getValue();
        return ret != null ? ret : CURRENT_FOLDER;
    }

    private String getFileMask() {
        String ret = fldFileMask.getValue();
        return ret != null ? ret : "*.*";
    }

    private void loadStateFromPrefs() {
        try {
            maxItemsInCombos = Integer.parseInt(userPrefs.getPreference(PREF_NODE_SETTINGS, "maxItemsInCombos", "12"));
            pnlWayfarer.setPrefHeight(Double.parseDouble(userPrefs.getPreference(PREF_NODE_SETTINGS, "stageheight", "600.0")));
            pnlWayfarer.setPrefWidth(Double.parseDouble(userPrefs.getPreference(PREF_NODE_SETTINGS, "stagewidth", "1000.0")));
            Util.initCombo(fldFileMask, userPrefs.loadRecentItems(PREF_NODE_RECENT_FILE_MASKS, DUMMY_STRING_CONVERTER));
            Util.initCombo(fldSearchText, userPrefs.loadRecentItems(PREF_NODE_RECENT_SEARCH_TEXTS, DUMMY_STRING_CONVERTER));
            Util.initCombo(fldRoot, userPrefs.loadRecentItems(PREF_NODE_RECENT_ROOTS, FILE_TO_STRING_CONVERTER));
        } catch (BackingStoreException ex) {
            LOG.warning(supply("Failed to load preferences: {0}", ex));
        }
    }

    private void saveStateToPrefs() {
        try {
            userPrefs.putPreference(PREF_NODE_SETTINGS, "maxItemsInCombos", String.valueOf(maxItemsInCombos));
            userPrefs.putPreference(PREF_NODE_SETTINGS, "stageheight", String.valueOf(pnlWayfarer.getHeight()));
            userPrefs.putPreference(PREF_NODE_SETTINGS, "stagewidth", String.valueOf(pnlWayfarer.getWidth()));
            userPrefs.saveRecentItems(fldFileMask.getItems(), PREF_NODE_RECENT_FILE_MASKS, DUMMY_STRING_CONVERTER);
            userPrefs.saveRecentItems(fldSearchText.getItems(), PREF_NODE_RECENT_SEARCH_TEXTS, DUMMY_STRING_CONVERTER);
            userPrefs.saveRecentItems(fldRoot.getItems(), PREF_NODE_RECENT_ROOTS, FILE_TO_STRING_CONVERTER);
        } catch (BackingStoreException ex) {
            String msg = "Failed to save preferences: " + ex;
            new Alert(Alert.AlertType.WARNING, msg).showAndWait();
            LOG.warning(msg);
        }
    }

    private void doExit() {
        saveStateToPrefs();
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
            setSearchState(false);
        }
    }

    private void setSearchState(boolean searching) {
        mainWindow.getScene().setCursor(searching ? Cursor.WAIT : Cursor.DEFAULT);
        toolbar.setDisable(searching);
        cmdStart.setDisable(searching);
        cmdStop.setDisable(!searching);
        cmdBrowseRoot.setDisable(searching);
        fldFileMask.setDisable(searching);
        fldSearchText.setDisable(searching);
        fldRoot.setDisable(searching);
        optSearchTextCaseSensitive.setDisable(searching);
        if (searching) {
            optSearchWholeWords.setDisable(true);
            optSearchTextRegex.setDisable(true);
        } else {
            setDependentOption(optSearchWholeWords, optSearchTextRegex.isSelected());
            setDependentOption(optSearchTextRegex, optSearchWholeWords.isSelected());
            fldStatus.setText("");
            showSummary();
            if (!lstFilesFound.getItems().isEmpty()) {
                lstFilesFound.requestFocus();
                if (lstFilesFound.getSelectionModel().getSelectedItems().isEmpty()) {
                    lstFilesFound.getSelectionModel().selectFirst();
                }
            } else {
                fldFileMask.requestFocus();
            }
        }
    }

    private void handleFileAction(FileActionHandler handler) {
        MatchedFile selectedItem = lstFilesFound.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            try {
                handler.handle(selectedItem.getFile().toFile());
            } catch (IOException ex) {
                Logger.LOG.warning(supply("File operation failed: ", ex));
            }
        }
    }

    private void setSearchCriteria(SearchCriteria from) {
        fldRoot.setValue(from.getRootFolder().toFile());
        fldFileMask.setValue(from.getFilemask());
        fldSearchText.setValue(from.getTxtToSearch());
        optSearchTextCaseSensitive.setSelected(from.isSearchTextCaseSensitive());
        optSearchWholeWords.setSelected(from.isWholeWordSearch());
        optSearchTextRegex.setSelected(from.isSearchTextRegex());
        fldFileMask.requestFocus();
    }

    private void setDependentOption(CheckBox forOpt, boolean isOtherSelected) {
        if (isOtherSelected) {
            forOpt.setSelected(false);
        }
        forOpt.setDisable(isOtherSelected);
    }
}
