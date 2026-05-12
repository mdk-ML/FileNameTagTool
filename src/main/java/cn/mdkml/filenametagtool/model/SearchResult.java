package cn.mdkml.filenametagtool.model;

import java.util.Date;

/**
 * 搜索结果实体类
 */
public class SearchResult {
    private String fullPath;
    private String fileName;
    private long size;
    private long lastModified;
    private boolean isDirectory;

    /**
     * 构造完整的搜索结果
     *
     * @param fullPath     文件完整路径
     * @param fileName     文件名
     * @param size         文件大小（字节）
     * @param lastModified 最后修改时间戳
     * @param isDirectory  是否为目录
     */
    public SearchResult(String fullPath, String fileName, long size, long lastModified, boolean isDirectory) {
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.size = size;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
    }

    /**
     * 构造简化的搜索结果（不含大小和修改时间）
     *
     * @param fullPath    文件完整路径
     * @param fileName    文件名
     * @param isDirectory 是否为目录
     */
    public SearchResult(String fullPath, String fileName, boolean isDirectory) {
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.isDirectory = isDirectory;
    }

    /**
     * 获取文件完整路径
     *
     * @return 完整路径
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * 获取文件名
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取文件大小（字节）
     *
     * @return 文件大小
     */
    public long getSize() {
        return size;
    }

    /**
     * 获取最后修改时间戳
     *
     * @return 修改时间戳
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * 判断是否为目录
     *
     * @return 是否为目录
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public String toString() {
        return (isDirectory ? "[文件夹] " : "[文件] ") + fullPath + " (大小: " + formatSize(size) + ", 修改时间: " + new Date(lastModified) + ")";
    }

    /**
     * 格式化文件大小
     *
     * @param size 字节数
     * @return 格式化后的大小字符串
     */
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
