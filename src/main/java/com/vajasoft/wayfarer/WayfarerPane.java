package com.vajasoft.wayfarer;

import static com.vajasoft.wayfarer.Logger.LOG;
import static com.vajasoft.wayfarer.Logger.supply;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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

    private static final StringConverter<File> FILE_TO_STRING_CONVERTER = new FileToStringConverter();
    private static final StringConverter<String> DUMMY_STRING_CONVERTER = new DummyStringConverter();
    private static final File CURRENT_FOLDER = new File(".").getAbsoluteFile();

    private Window mainWindow;
    private AboutBox aboutBox;

    private final NumberFormat sizeFormatter = NumberFormat.getNumberInstance();
    private final UserPreferences userPrefs = new UserPreferences(WayfarerPane.class);
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
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
    private Button cmdBrowseRoot;
    @FXML
    private CheckBox optSearchTextCaseSensitive;
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
    private TableColumn<MatchedFile, String> colFilename;
    @FXML
    private TableColumn<MatchedFile, String> colFolder;
    @FXML
    private TableColumn<MatchedFile, String> colLastModified;
    private int maxItemsInCombos;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        aboutBox = new AboutBox(pnlWayfarer, getClass());
        sizeFormatter.setMaximumFractionDigits(1);
        dirChooser = new DirectoryChooser();
//        searchResults = new HashMap<>();
        fldRoot.setConverter(FILE_TO_STRING_CONVERTER);
        loadStateFromPrefs();
//        lstFilesFound.getColumns().forEach((TableColumn<MatchedFile, ?> t) -> {
//            ((TableColumn<MatchedFile, String>) t).setCellValueFactory(WayfarerPane::getCellValue);
//        });
        colFilename.setCellValueFactory(this::matchedFileCellValueFactory);
        colFolder.setCellValueFactory(this::matchedFileCellValueFactory);
        colLastModified.setCellValueFactory(this::matchedFileCellValueFactory);
//        colLastModified.setComparator((FileTime arg0, FileTime arg1) -> {
//            return 0; //To change body of generated lambdas, choose Tools | Templates.
//        });
        lstFilesFound.getSelectionModel().selectedItemProperty().addListener(this::selectHittedFile);
        lstFilesFound.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mnuCopySelected.disableProperty().bind(new FileActionDisabled(lstFilesFound));
        SingleFileActionDisabled fileActionDisabled = new SingleFileActionDisabled(lstFilesFound);
        mnuCopyMatchedFilePath.disableProperty().bind(fileActionDisabled);
        mnuOpenMatchedFile.disableProperty().bind(fileActionDisabled);
        mnuOpenMatchedFileFolder.disableProperty().bind(fileActionDisabled);
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

    private ObservableValue<String> matchedFileCellValueFactory(TableColumn.CellDataFeatures<MatchedFile, String> cellData) {
        MatchedFile row = cellData.getValue();
        TableColumn col = cellData.getTableColumn();
        return new SimpleStringProperty(matchedFileToColumns(row, col));
    }

    private String matchedFileToColumns(MatchedFile row, TableColumn col) {
        String ret = "";
        if (col == colFilename) {
            ret = row.getFile().getFileName().toString();
        } else if (col == colFolder) {
            ret = row.getFile().getParent().toString();
        } else if (col == colLastModified) {
            ret = String.valueOf(row.getAttrs().lastModifiedTime());
        }
        return ret;
    }

    private void selectHittedFile(ObservableValue<? extends MatchedFile> ov, MatchedFile oldValue, MatchedFile newValue) {
        fldHits.getEngine().loadContent("");
        fldStatus.setText("");
        if (newValue != null) {
            fldStatus.setText("Selected item " + newValue.getAttrs().size() + " bytes, " + newValue.getTotalNbrofHits() + " hits");
            showHits(newValue.getLines());
        }
    }

    private void showHits(List<MatchedLine> lines) {
        Optional<String> fullContent = lines.stream().map(this::lineToText).reduce((contentSoFar, line) -> {
            return contentSoFar + line;
        });
        if (fullContent.isPresent()) {
            StringBuilder buf = new StringBuilder();
            buf.append("<style> em { font-style: normal; font-weight: 900; color: red; } </style>");
            buf.append("<pre>");
            buf.append(fullContent.get());
            buf.append("</pre>");
            fldHits.getEngine().loadContent(buf.toString());
            buf.append("</pre>");
        }
    }

    private String lineToText(MatchedLine line) {
        StringBuilder buf = new StringBuilder();
        buf.append(String.valueOf(line.getLineNbr()));
        buf.append("\t");
        Iterator<Integer[]> matches = line.getMatches().iterator();
        Integer[] match = matches.hasNext() ? matches.next() : null;
        String li = line.getLine();
        for (int i = 0; i < li.length(); i++) {
            if (match != null && i == match[0]) {
                buf.append("<em>");
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
                buf.append("</em>");
                match = matches.hasNext() ? matches.next() : null;
            }
        }
        buf.append("\n");
        return buf.toString();
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
        ObservableList<MatchedFile> selectedItems = lstFilesFound.getSelectionModel().getSelectedItems();
        List<TableColumn<MatchedFile, ?>> columns = lstFilesFound.getColumns();
        StringBuilder buf = new StringBuilder();
        boolean newLine = true;
        for (MatchedFile f : selectedItems) {
            for (TableColumn col : columns) {
                if (!newLine) {
                    buf.append('\t');
                }
                buf.append(matchedFileToColumns(f, col));
                newLine = false;
            }
            buf.append('\n');
            newLine = true;
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
                optSearchTextRegex.isSelected()
        );
        searcher = new SearchTask(
                crit,
                searchResults);
        searcher.setWorkerStateEventHandler(this::handleWorkerStateEvent);
        Thread th = new Thread(searcher);
        th.setDaemon(true);
        th.start();
        rememberValue(fldRoot);
        rememberValue(fldFileMask);
        rememberValue(fldSearchText);
//        cmdStart.setDisable(false);
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

    private <T> void rememberValue(ComboBox<T> combo) {
        T value = combo.getValue();
        if (value != null) {
            ObservableList<T> items = combo.getItems();
            int ix = items.indexOf(value);
            if (ix != 0) {  // no need to do anything, if value already in the beginning of the list
                if (ix > 0) {   // value found further down the list => move up to first
                    items.remove(value);
                } else if (items.size() == maxItemsInCombos) {
                    items.remove(items.size() - 1); // remove last
                }
                items.add(0, value);
                combo.getSelectionModel().select(value);
            }
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

    private void loadStateFromPrefs() {
        try {
            maxItemsInCombos = Integer.parseInt(userPrefs.getPreference(PREF_NODE_SETTINGS, "maxItemsInCombos", "12"));
            pnlWayfarer.setPrefHeight(Double.parseDouble(userPrefs.getPreference(PREF_NODE_SETTINGS, "stageheight", "600.0")));
            pnlWayfarer.setPrefWidth(Double.parseDouble(userPrefs.getPreference(PREF_NODE_SETTINGS, "stagewidth", "1000.0")));
            initCombo(fldFileMask, PREF_NODE_RECENT_FILE_MASKS, DUMMY_STRING_CONVERTER);
            initCombo(fldSearchText, PREF_NODE_RECENT_SEARCH_TEXTS, DUMMY_STRING_CONVERTER);
            initCombo(fldRoot, PREF_NODE_RECENT_ROOTS, FILE_TO_STRING_CONVERTER);
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

    private <T> void initCombo(ComboBox<T> combo, String prefName, StringConverter<T> prefConverter) throws BackingStoreException {
        ObservableList<T> content = FXCollections.observableArrayList(userPrefs.loadRecentItems(prefName, prefConverter));
        combo.setItems(content);
        if (!content.isEmpty()) {
            combo.getSelectionModel().selectFirst();
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
//            searcher.cancelSearch();
            setSearchState(false);
        }
    }

    private void setSearchState(boolean searching) {
        mainWindow.getScene().setCursor(searching ? Cursor.WAIT : Cursor.DEFAULT);
        cmdStart.setDisable(searching);
        cmdStop.setDisable(!searching);
        cmdBrowseRoot.setDisable(searching);
        fldFileMask.setDisable(searching);
        fldSearchText.setDisable(searching);
        fldRoot.setDisable(searching);
        optSearchTextCaseSensitive.setDisable(searching);
        optSearchTextRegex.setDisable(searching);
        if (!searching) {
            fldStatus.setText("");
            showSummary();
            if (!lstFilesFound.getItems().isEmpty()) {
                lstFilesFound.requestFocus();
//                if (lstFilesFound.getSelectionModel().getSelectedItem() == null) {
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
}
