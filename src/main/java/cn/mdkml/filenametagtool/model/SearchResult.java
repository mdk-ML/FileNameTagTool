package cn.mdkml.filenametagtool.model;

import java.util.Date;

// 搜索结果实体类
public class SearchResult {
    private String fullPath;
    private String fileName;
    private long size;
    private long lastModified;
    private boolean isDirectory;

    public SearchResult(String fullPath, String fileName, long size, long lastModified, boolean isDirectory) {
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.size = size;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
    }

    public SearchResult(String fullPath, String fileName, boolean isDirectory) {
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.isDirectory = isDirectory;
    }

    // Getter方法
    public String getFullPath() {
        return fullPath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public String toString() {
        return (isDirectory ? "[文件夹] " : "[文件] ") + fullPath + " (大小: " + formatSize(size) + ", 修改时间: " + new Date(lastModified) + ")";
    }

    // 格式化文件大小
    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
