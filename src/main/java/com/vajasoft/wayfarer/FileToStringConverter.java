package com.vajasoft.wayfarer;

import java.io.File;
import javafx.util.StringConverter;

/**
 *
 * @author z705692
 */
class FileToStringConverter extends StringConverter<File> {
    public FileToStringConverter() {
    }

    @Override
    public String toString(File file) {
        return file != null ? file.getAbsolutePath() : "";
    }

    @Override
    public File fromString(String path) {
        return path == null || path.isEmpty() ? null : new File(path);
    }
    
}
