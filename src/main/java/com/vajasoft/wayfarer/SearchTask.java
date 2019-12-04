package com.vajasoft.wayfarer;

import java.nio.file.Files;
import java.nio.file.Path;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

/**
 *
 * @author z705692
 */
public class SearchTask extends Task<Void> {
    private final Path rootFolder;
    private final FileTreeWalker walker;

    public SearchTask(Path rootFolder, String filemask, String txtToSearch, boolean isSearchTextCaseSensitive, SearchResult searchResults, ObservableList<Path> output) {
        super();
        this.rootFolder = rootFolder;
        this.walker = new FileTreeWalker(filemask, txtToSearch, isSearchTextCaseSensitive, searchResults, output);
    }

    public void cancelSearch() {
        walker.cancelSearch();
    }

    public void setWorkerStateEventHandler(EventHandler<WorkerStateEvent> eventHndlr)  {
        setEventHandler(WorkerStateEvent.ANY, eventHndlr);
    }
    
    @Override
    protected Void call() throws Exception {
        Files.walkFileTree(rootFolder, walker);
        return null;
    }
}
