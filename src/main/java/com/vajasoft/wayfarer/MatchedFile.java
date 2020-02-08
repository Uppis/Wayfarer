package com.vajasoft.wayfarer;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


public class MatchedFile {
    private final Path file;
    private final BasicFileAttributes attrs;
    private final List<MatchedLine> lines;

    public MatchedFile(Path file, BasicFileAttributes attrs) {
        this.file = file;
        this.attrs = attrs;
        this.lines = new ArrayList<>();
    }

    public final MatchedLine matchingLineFound(String line, int lineNbr) {
        MatchedLine matchedLine = new MatchedLine(line, lineNbr);
        lines.add(matchedLine);
        return matchedLine;
    }

    public Path getFile() {
        return file;
    }

    public BasicFileAttributes getAttrs() {
        return attrs;
    }

    public List<MatchedLine> getLines() {
        return lines;
    }

    public int getTotalNbrofHits() {
        int ret = 0;
        for (MatchedLine line : lines) {
            ret += line.getTotalNbrofHits();
        }
        return ret;
    }
}
