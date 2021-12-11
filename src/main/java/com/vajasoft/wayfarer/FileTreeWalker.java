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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileTreeWalker extends SimpleFileVisitor<Path> {

    private static final String PATH_MATCH_PREFIX_GLOB = "glob:**/";
    private static final String PATH_MATCH_PREFIX_REGEX = "regex:";
    private static final String EXTRA_WORD_CHARS = "_";
    private static final Charset CHARSET = Charset.forName(System.getProperty("file.encoding"));
    private static final CharsetDecoder DECODER = CHARSET.newDecoder().onUnmappableCharacter(CodingErrorAction.IGNORE).onMalformedInput(CodingErrorAction.IGNORE);

    private final List<PathMatcher> fileMatchers;
    private final boolean ignoreCase;
    private final boolean wholeWordSearch;
    private final String txtToSearchPlain;
    private final Pattern txtToSearchRegex;
    private final SearchResult searchResults;
    private volatile FileVisitResult result = FileVisitResult.CONTINUE;

    public FileTreeWalker(SearchCriteria crit, SearchResult searchRes) {
        fileMatchers = getMatchers(crit);
        ignoreCase = !crit.isSearchTextCaseSensitive();
        wholeWordSearch = crit.isWholeWordSearch();
        String searchText = crit.getTxtToSearch();
        if (searchText == null || searchText.isEmpty()) {
            txtToSearchPlain = null;
            txtToSearchRegex = null;
        } else {
            if (crit.isSearchTextRegex()) {
                txtToSearchPlain = null;
                txtToSearchRegex =  ignoreCase ? Pattern.compile(searchText, Pattern.CASE_INSENSITIVE) : Pattern.compile(searchText);
            } else {
                txtToSearchPlain = searchText;
                txtToSearchRegex = null;
            }
        }
        searchResults = searchRes;
    }

    public void cancelSearch() {
        result = FileVisitResult.TERMINATE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        LOG.finest(supply("Visiting folder {0}", dir.toString()));
        searchResults.incrementNbrofFilesChecked();
        return result;
    }

    @Override
    public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) {
        LOG.fine(supply("Visiting file {0}", file.toString()));
        searchResults.report(file);
        searchResults.incrementNbrofFilesChecked();
        if (pathMatches(file)) {
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                LineNumberReader reader = new LineNumberReader(Channels.newReader(channel, DECODER, -1));
                String line = reader.readLine();
                while (line != null && result != FileVisitResult.TERMINATE) {
                    if (txtToSearchPlain == null && txtToSearchRegex == null) { // Everything is a match
                        MatchedFile match = searchResults.matchFoundInFile(file, attrs);
                        match.matchingLineFound(line, reader.getLineNumber());
                    } else {
                        if (txtToSearchPlain != null) { 
                            doPlainTextSearch(line, file, attrs, reader.getLineNumber());
                        } else {
                            doRegexSearch(line, file, attrs, reader.getLineNumber());
                        }
                    }
                    line = reader.readLine();
                }
                searchResults.incrementNbrofFilesSearched();
            } catch (IOException ex) {
                LOG.warning(supply("Exception in visiting {0}: {1}", file.toString(), ex));
            }
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

    private List<PathMatcher> getMatchers(SearchCriteria crit) {
        List<PathMatcher> ret = new ArrayList<>();
        for (String mask : crit.getFilemask().split("\\s*;\\s*")) {
            ret.add(FileSystems.getDefault().getPathMatcher((crit.isFileMaskRegex() ? PATH_MATCH_PREFIX_REGEX : PATH_MATCH_PREFIX_GLOB) + mask));
        }
        return ret;
    }

    private boolean pathMatches(Path file) {
        for (PathMatcher m : fileMatchers) {
            if (m.matches(file)) {
                return true;
            }
        }
        return false;
    }

    private void doRegexSearch(String line, Path file, BasicFileAttributes attrs, int lineNumber) {
        Matcher matcher = txtToSearchRegex.matcher(line);
        if (matcher.find()) {
            MatchedFile match = searchResults.matchFoundInFile(file, attrs);
            MatchedLine matchingLine = match.matchingLineFound(line, lineNumber);
            do {
                matchingLine.addMatch(matcher.start(), matcher.end() - 1);  // end == offset AFTER the last character matched
            } while (matcher.find());
        }
    }

    private void doPlainTextSearch(String line, Path file, BasicFileAttributes attrs, int lineNumber) {
        MatchedLine matchingLine = null;
        int pos = 0;
        int len = txtToSearchPlain.length();
        do {
            if (isPlainMatch(line, pos, len)) {
                if (matchingLine == null) { // First match in this line of text
                    MatchedFile match = searchResults.matchFoundInFile(file, attrs);
                    matchingLine = match.matchingLineFound(line, lineNumber);
                }
                matchingLine.addMatch(pos, (pos += len) - 1);
            } else {
                pos++;
            }
        } while (pos + len <= line.length());
    }

    private boolean isPlainMatch(String line, int pos, int len) {
        boolean ret = line.regionMatches(ignoreCase, pos, txtToSearchPlain, 0, len);
        if (ret && wholeWordSearch) {
            boolean isWordBeg = pos == 0 || !isWordChar(line.charAt(pos - 1));
            boolean isWordEnd = pos + len == line.length() || !isWordChar(line.charAt(pos + len));
            ret = isWordBeg && isWordEnd;
        }
        return ret;
    }

    private static boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || EXTRA_WORD_CHARS.indexOf(ch) >= 0;
    }
}
