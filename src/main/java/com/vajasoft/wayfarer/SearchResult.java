package com.vajasoft.wayfarer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Z705692
 */
public class SearchResult {
    private final Map<Path, List<String>> filesHit;
    private final long searchStarted;
    private int nbrofFilesChecked;
    private int nbrofFilesSearched;

    public SearchResult() {
        searchStarted = System.currentTimeMillis();
        filesHit  = new HashMap<>();
    }

    public void storeMatch(Path file, int lineNbr, String line) {
        List<String> lines = filesHit.get(file);
        if (lines == null) {
            lines = new ArrayList<>();
            filesHit.put(file, lines);
        }
        lines.add(lineNbr + "\t" + line);
    }

    public int incrementNbrofFilesChecked() {
        return ++nbrofFilesChecked;
    }

    public int incrementNbrofFilesSearched() {
        return ++nbrofFilesSearched;
    }

    public Map<Path, List<String>> getFilesHit() {
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
        for (List<String> list : filesHit.values()) {
            ret += list.size();
        }
        return ret;
    }

    public static class Match {
        private final String line;
        private final List<int[]> matches;

        Match(String line) {
            this.line = line;
            matches = new ArrayList();
        }

        void addMatch(int start, int end) {
            matches.add(new int[]{start, end});
        }

        public String getLine() {
            return line;
        }

        public List<int[]> getMatches() {
            return matches;
        }
    }
}
