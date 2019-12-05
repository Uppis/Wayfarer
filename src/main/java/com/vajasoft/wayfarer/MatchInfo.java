package com.vajasoft.wayfarer;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


public class MatchInfo {
    private final BasicFileAttributes attrs;
    private final String line;
    private final int lineNbr;
    private final List<Integer[]> matches;

    public MatchInfo(BasicFileAttributes attrs, String line, int lineNbr) {
        this.attrs = attrs;
        this.line = line;
        this.lineNbr = lineNbr;
        this.matches = new ArrayList<>();
    }

    public void addMatch(int begin, int end) {
        matches.add(new Integer[]{begin, end});
    }

    public BasicFileAttributes getAttrs() {
        return attrs;
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
}
