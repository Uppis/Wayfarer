package com.vajasoft.wayfarer;

import java.nio.file.Path;
import java.util.Objects;

/**
 *
 * @author Z705692
 */
public class SearchCriteria {
    private final Path rootFolder;
    private final String filemask;
    private final String txtToSearch;
    private final boolean searchTextCaseSensitive;
    private final boolean wholeWordSearch;
    private final boolean searchTextRegex;
    private final boolean fileMaskRegex;

    public SearchCriteria(Path rootFolder, String filemask, boolean fileMaskRegex, String txtToSearch, boolean searchTextCaseSensitive, boolean wholeWordSearch, boolean searchTextRegex) {
        this.rootFolder = rootFolder;
        this.filemask = filemask;
        this.txtToSearch = txtToSearch;
        this.searchTextCaseSensitive = searchTextCaseSensitive;
        this.wholeWordSearch = wholeWordSearch;
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

    public boolean isWholeWordSearch() {
        return wholeWordSearch;
    }

    public boolean isSearchTextRegex() {
        return searchTextRegex;
    }

    public boolean isFileMaskRegex() {
        return fileMaskRegex;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.rootFolder);
        hash = 29 * hash + Objects.hashCode(this.filemask);
        hash = 29 * hash + Objects.hashCode(this.txtToSearch);
        hash = 29 * hash + (this.searchTextCaseSensitive ? 1 : 0);
        hash = 29 * hash + (this.wholeWordSearch ? 1 : 0);
        hash = 29 * hash + (this.searchTextRegex ? 1 : 0);
        hash = 29 * hash + (this.fileMaskRegex ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SearchCriteria other = (SearchCriteria)obj;
        if (this.searchTextCaseSensitive != other.searchTextCaseSensitive) {
            return false;
        }
        if (this.searchTextRegex != other.searchTextRegex) {
            return false;
        }
        if (this.wholeWordSearch != other.wholeWordSearch) {
            return false;
        }
        if (this.fileMaskRegex != other.fileMaskRegex) {
            return false;
        }
        if (!Objects.equals(this.filemask, other.filemask)) {
            return false;
        }
        if (!Objects.equals(this.txtToSearch, other.txtToSearch)) {
            return false;
        }
        if (!Objects.equals(this.rootFolder, other.rootFolder)) {
            return false;
        }
        return true;
    }
}
