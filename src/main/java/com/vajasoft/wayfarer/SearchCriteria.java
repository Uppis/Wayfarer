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
    private final boolean isSearchTextCaseSensitive;

    public SearchCriteria(Path rootFolder, String filemask, String txtToSearch, boolean isSearchTextCaseSensitive) {
        this.rootFolder = rootFolder;
        this.filemask = filemask;
        this.txtToSearch = txtToSearch;
        this.isSearchTextCaseSensitive = isSearchTextCaseSensitive;
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

    public boolean isIsSearchTextCaseSensitive() {
        return isSearchTextCaseSensitive;
    }

}
