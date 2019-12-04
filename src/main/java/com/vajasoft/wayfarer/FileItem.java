package com.vajasoft.wayfarer;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileItem {
    private String path;
    private String name;
    private boolean isDir;
    private long size;

    public FileItem(Path file) {
        try {
            path = file.normalize().toString();
            Path filename = file.getFileName();
            if (filename != null) {
                name = filename.toString();
            }
            isDir = Files.isDirectory(file);
            if (!isDir) {
                size = Files.size(file);
            }
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public String getPathName() {
        return path;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDir;
    }

    public long getSize() {
        return size;
    }

    void increaseSize(long by) {
        size += by;
    }
}
