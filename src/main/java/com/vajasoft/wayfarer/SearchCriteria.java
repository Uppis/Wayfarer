package com.vajasoft.wayfarer;

import java.nio.file.Path;

/**
 *
 * @author Z705692
 */
public class SearchCriteria {
    private final Path rootFolder;
    private final String filemask;
    private final String txtToSearch;
    private final boolean searchTextCaseSensitive;
    private final boolean searchTextRegex;
    private final boolean fileMaskRegex;

    public SearchCriteria(Path rootFolder, String filemask, boolean fileMaskRegex, String txtToSearch, boolean searchTextCaseSensitive, boolean searchTextRegex) {
        this.rootFolder = rootFolder;
        this.filemask = filemask;
        this.txtToSearch = txtToSearch;
        this.searchTextCaseSensitive = searchTextCaseSensitive;
        this.searchTextRegex = searchTextRegex;
        this.fileMaskRegex = fileMaskRegex;
    }

    public Path getRootFolder() {
        return rootFolder;
    }

    public String getFilemask() {
        return filemask;
    }

    public String getTxtToSearch() {
        return txtToSearch;
    }

    public boolean isSearchTextCaseSensitive() {
        return searchTextCaseSensitive;
    }

    public boolean isSearchTextRegex() {
        return searchTextRegex;
    }

    public boolean isFileMaskRegex() {
        return fileMaskRegex;
    }
}
