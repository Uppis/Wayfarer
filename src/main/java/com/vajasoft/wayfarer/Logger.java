package com.vajasoft.wayfarer;

import java.text.MessageFormat;
import java.util.function.Supplier;

/**
 *
 * @author Z705692
 */
public class Logger {
    public static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Logger.class.getPackage().getName());

    public static Supplier<String> supply(String msg, Object... params) {
        return () -> MessageFormat.format(msg, params);
    }
}
