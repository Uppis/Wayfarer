package com.vajasoft.wayfarer;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


public class HitInfo {
    private final BasicFileAttributes attrs;
    private final String line;
    private final int lineNbr;
    private final List<Integer[]> hits;

    public HitInfo(BasicFileAttributes attrs, String line, int lineNbr) {
        this.attrs = attrs;
        this.line = line;
        this.lineNbr = lineNbr;
        this.hits = new ArrayList<>();
    }

}
