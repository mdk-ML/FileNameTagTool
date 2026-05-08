package cn.mdkml.filenametagtool.component;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件列表表格模型，支持多关键词搜索过滤、相关度排序和按列排序。
 * 
 * 排序逻辑：
 * - 搜索时默认按相关度排序
 * - 用户点击表头后，切换为按列排序（搜索过滤仍保留）
 * - 清除搜索后，按列排序
 */
public class FileTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"名称", "修改日期", "类型", "大小"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private final List<FileEntry> allEntries = new ArrayList<>();
    private List<FileEntry> displayedEntries = new ArrayList<>();
    private String[] searchKeywords = new String[0];
    private int sortColumn = 0;
    private boolean sortAscending = true;
    /** 是否使用搜索相关度排序（搜索时默认true，用户点击表头后变为false） */
    private boolean useRelevanceSort = false;

    public FileTableModel() {
    }

    /**
     * 设置全部文件数据
     */
    public void setFiles(List<File> files) {
        allEntries.clear();
        for (File f : files) {
            allEntries.add(new FileEntry(f));
        }
        applyFilterAndSort();
    }

    /**
     * 设置搜索关键词（空格分隔）
     */
    public void setSearchKeywords(String[] keywords) {
        this.searchKeywords = keywords != null ? keywords : new String[0];
        // 搜索时自动启用相关度排序
        this.useRelevanceSort = this.searchKeywords.length > 0;
        applyFilterAndSort();
    }

    /**
     * 设置排序列和方向（用户点击表头时调用）
     */
    public void setSort(int column, boolean ascending) {
        this.sortColumn = column;
        this.sortAscending = ascending;
        // 用户主动排序时，关闭相关度排序
        this.useRelevanceSort = false;
        applyFilterAndSort();
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    /**
     * 获取指定行的 File 对象
     */
    public File getFileAt(int row) {
        if (row >= 0 && row < displayedEntries.size()) {
            return displayedEntries.get(row).file;
        }
        return null;
    }

    /**
     * 根据 File 对象查找行索引
     */
    public int findRowByFile(File file) {
        for (int i = 0; i < displayedEntries.size(); i++) {
            if (displayedEntries.get(i).file.equals(file)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取匹配的文件数量
     */
    public int getMatchCount() {
        return displayedEntries.size();
    }

    /**
     * 获取显示文件的总大小（字节）
     */
    public long getTotalSize() {
        long total = 0;
        for (FileEntry entry : displayedEntries) {
            total += entry.file.length();
        }
        return total;
    }

    /**
     * 应用过滤和排序
     */
    private void applyFilterAndSort() {
        List<FileEntry> filtered;

        if (searchKeywords.length == 0) {
            filtered = new ArrayList<>(allEntries);
        } else {
            filtered = allEntries.stream()
                    .filter(entry -> matchesAllKeywords(entry))
                    .collect(Collectors.toList());
        }

        // 排序逻辑：搜索时默认按相关度，用户点击表头后按列排序
        boolean isSearching = searchKeywords.length > 0;
        if (isSearching && useRelevanceSort) {
            // 搜索相关度排序：先按相关度降序，再按列排序作为次要排序
            filtered.sort((a, b) -> {
                int scoreA = calculateRelevance(a);
                int scoreB = calculateRelevance(b);
                int cmp = Integer.compare(scoreB, scoreA);
                if (cmp != 0) return cmp;
                return compareEntries(a, b, sortColumn, sortAscending);
            });
        } else {
            // 按列排序
            filtered.sort((a, b) -> compareEntries(a, b, sortColumn, sortAscending));
        }

        this.displayedEntries = filtered;
        fireTableDataChanged();
    }

    private boolean matchesAllKeywords(FileEntry entry) {
        String lowerName = entry.file.getName().toLowerCase();
        for (String kw : searchKeywords) {
            if (!lowerName.contains(kw.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private int calculateRelevance(FileEntry entry) {
        String lowerName = entry.file.getName().toLowerCase();
        int score = 0;
        int matchedCount = 0;
        for (String keyword : searchKeywords) {
            String lowerKeyword = keyword.toLowerCase();
            int idx = lowerName.indexOf(lowerKeyword);
            if (idx >= 0) {
                matchedCount++;
                score += 100;
                if (idx == 0) score += 50;
                int count = 0;
                int from = 0;
                while ((from = lowerName.indexOf(lowerKeyword, from)) >= 0) {
                    count++;
                    from += lowerKeyword.length();
                }
                score += (count - 1) * 10;
            }
        }
        score += matchedCount * 200;
        score += Math.max(0, 500 - entry.file.getName().length());
        return score;
    }

    private int compareEntries(FileEntry a, FileEntry b, int column, boolean ascending) {
        int cmp = 0;
        switch (column) {
            case 0: // 名称
                cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                break;
            case 1: // 修改日期
                cmp = Long.compare(a.file.lastModified(), b.file.lastModified());
                break;
            case 2: // 类型
                cmp = a.type.compareToIgnoreCase(b.type);
                if (cmp == 0) cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                break;
            case 3: // 大小
                cmp = Long.compare(a.file.length(), b.file.length());
                break;
            default:
                cmp = 0;
        }
        return ascending ? cmp : -cmp;
    }

    // ========== AbstractTableModel ==========

    @Override
    public int getRowCount() {
        return displayedEntries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return String.class;
            case 1: return String.class;
            case 2: return String.class;
            case 3: return String.class;
            default: return Object.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= displayedEntries.size()) return "";
        FileEntry entry = displayedEntries.get(rowIndex);
        switch (columnIndex) {
            case 0: return entry.file.getName();
            case 1: return DATE_FORMAT.format(new Date(entry.file.lastModified()));
            case 2: return entry.type;
            case 3: return formatFileSize(entry.file.length());
            default: return "";
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    // ========== 内部工具 ==========

    private static String getFileType(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            String ext = name.substring(dot + 1).toUpperCase();
            return ext + " 文件";
        }
        return "文件";
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 文件条目，缓存文件类型等元数据
     */
    private static class FileEntry {
        final File file;
        final String type;

        FileEntry(File file) {
            this.file = file;
            this.type = getFileType(file);
        }
    }
}
