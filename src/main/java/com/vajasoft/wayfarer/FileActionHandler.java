package com.vajasoft.wayfarer;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Z705692
 */
@FunctionalInterface
public interface FileActionHandler {
    void handle(File f) throws IOException;
}
