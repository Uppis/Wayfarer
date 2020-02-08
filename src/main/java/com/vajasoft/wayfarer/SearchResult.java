package com.vajasoft.wayfarer;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 *
 * @author Z705692
 */
public class SearchResult {
    private final BiConsumer<Path, MatchedFile> progressHandler;
    private final Map<Path, MatchedFile> filesHit;
    private final long searchStarted;
    private int nbrofFilesChecked;
    private int nbrofFilesSearched;

    public SearchResult(BiConsumer<Path, MatchedFile> progressHandler) {
        this.searchStarted = System.currentTimeMillis();
        this.filesHit  = new HashMap<>();
        this.progressHandler = progressHandler;
    }

    public void report(Path file) {
        progressHandler.accept(file, null);
    }

    public MatchedFile matchFoundInFile(Path file, BasicFileAttributes attrs) {
        MatchedFile mf = filesHit.get(file);
        if (mf == null) {
            mf = new MatchedFile(file, attrs);
            filesHit.put(file, mf);
            progressHandler.accept(file, mf);
        }
        return mf;
    }

    public int incrementNbrofFilesChecked() {
        return ++nbrofFilesChecked;
    }

    public int incrementNbrofFilesSearched() {
        return ++nbrofFilesSearched;
    }

    public Map<Path, MatchedFile> getFilesHit() {
        return filesHit;
    }

    public boolean containsFile(Path file) {
        return filesHit.containsKey(file);
    }

    public int getNbrofFilesVisited() {
        return nbrofFilesChecked;
    }

    public int getNbrofFilesSearched() {
        return nbrofFilesSearched;
    }

    public int getNbrofFilesHit() {
        return filesHit.size();
    }

    public long getSearchTime() {
        return System.currentTimeMillis() - searchStarted;
    }

    public int getTotalNbrofHits() {
        int ret = 0;
        for (MatchedFile mf : filesHit.values()) {
            ret += mf.getTotalNbrofHits();
        }
        return ret;
    }
}
