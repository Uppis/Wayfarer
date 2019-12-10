package com.vajasoft.wayfarer;

import java.nio.file.Files;
import java.nio.file.Path;
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

    public SearchTask(SearchCriteria crit, SearchResult searchResults) {
        super();
        this.rootFolder = crit.getRootFolder();
        this.walker = new FileTreeWalker(crit, searchResults);
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
