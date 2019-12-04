package com.vajasoft.wayfarer;

import static com.vajasoft.wayfarer.Logger.LOG;
import static com.vajasoft.wayfarer.Logger.supply;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.collections.ObservableList;

public class FileTreeWalker extends SimpleFileVisitor<Path> {
    private static final Charset CHARSET = Charset.forName(System.getProperty("file.encoding"));
    private static final CharsetDecoder DECODER = CHARSET.newDecoder().onUnmappableCharacter(CodingErrorAction.IGNORE).onMalformedInput(CodingErrorAction.IGNORE);

    private final List<PathMatcher> fileMatchers;
    private final Pattern txtToSearch;
    private final SearchResult searchResults;
    private final ObservableList<Path> output;
    private volatile FileVisitResult result = FileVisitResult.CONTINUE;

    public FileTreeWalker(String filemask, String txtToSearch, boolean isSearchTextCaseSensitive, SearchResult searchResults, ObservableList<Path> output) {
        this.fileMatchers = getMatchers(filemask);
        if (txtToSearch == null || txtToSearch.isEmpty()) {
            this.txtToSearch = null;
        } else {
            this.txtToSearch = isSearchTextCaseSensitive ? Pattern.compile(txtToSearch) : Pattern.compile(txtToSearch, Pattern.CASE_INSENSITIVE);
        }
        this.searchResults = searchResults;
        this.output = output;
    }

    public void cancelSearch() {
        result = FileVisitResult.TERMINATE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) {
        LOG.fine(supply("Visiting file {0}", file.toString()));
        searchResults.incrementNbrofFilesChecked();
        try {
            FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
            LineNumberReader reader = new LineNumberReader(Channels.newReader(channel, DECODER, -1));
            for (PathMatcher m : fileMatchers) {
                if (m.matches(file)) {
                    String line = reader.readLine();
                    while (line != null && result != FileVisitResult.TERMINATE) {
                        if (txtToSearch == null || txtToSearch.matcher(line).find()) {
                            searchResults.storeMatch(file, reader.getLineNumber(), line);
                        }
                        line = reader.readLine();
                    }
                    searchResults.incrementNbrofFilesSearched();

                    if (searchResults.containsFile(file)) {
                        Platform.runLater(() -> {
                            output.add(file);
                        });
                    }
                    break;  // One match is enough
                }
            }
        } catch (IOException ex) {
            LOG.warning(supply("Exception in visiting {0}: {1}", file.toString(), ex));
        }
        return result;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException ex) throws IOException {
        searchResults.incrementNbrofFilesChecked();
        if (ex instanceof AccessDeniedException) {
            LOG.log(Level.INFO, "Access Denied", ex);
        } else {
            LOG.log(Level.SEVERE, "VisitFile Failed", ex);
        }
        return result;
    }

    private List<PathMatcher> getMatchers(String filemask) {
        List<PathMatcher> ret = new ArrayList<>();
        for (String mask : filemask.split("\\s*;\\s*")) {
            ret.add(FileSystems.getDefault().getPathMatcher("glob:**/" + mask));
        }
        return ret;
    }
}
