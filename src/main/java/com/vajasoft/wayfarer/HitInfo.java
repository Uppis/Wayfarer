package com.vajasoft.wayfarer;

import java.util.ArrayList;
import java.util.List;


public class HitInfo {
    private final String line;
    private final int lineNbr;
    private final List<Integer[]> hits;

    public HitInfo(String line, int lineNbr) {
        this.line = line;
        this.lineNbr = lineNbr;
        this.hits = new ArrayList<>();
    }

}
