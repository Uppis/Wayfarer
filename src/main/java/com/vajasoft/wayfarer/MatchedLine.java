package com.vajasoft.wayfarer;

import java.util.ArrayList;
import java.util.List;


public class MatchedLine {
    private final String line;
    private final int lineNbr;
    private final List<Integer[]> matches;

    public MatchedLine(String line, int lineNbr) {
        this.line = line;
        this.lineNbr = lineNbr;
        this.matches = new ArrayList<>();
    }

    public void addMatch(int begin, int end) {
        matches.add(new Integer[]{begin, end});
    }

    public String getLine() {
        return line;
    }

    public int getLineNbr() {
        return lineNbr;
    }

    public List<Integer[]> getMatches() {
        return matches;
    }

    public int getTotalNbrofHits() {
        return matches.isEmpty() ? 1 : matches.size();
    }
}
