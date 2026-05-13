package cn.mdkml.filenametagtool.component;

import cn.mdkml.filenametagtool.model.Config;
import cn.mdkml.filenametagtool.util.FileUtil;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件列表表格模型，支持多关键词搜索过滤和按列排序。
 *
 * 排序逻辑：
 * - 搜索时保持按表头列排序
 * - 标签列排序：按标签分数总和排序（配置文件最后的标签10分，倒数第二20分，以此类推）
 */
public class FileTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"名称", "标签", "修改日期", "类型", "大小"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private final List<FileEntry> allEntries = new ArrayList<>();
    private List<FileEntry> displayedEntries = new ArrayList<>();
    private String[] searchKeywords = new String[0];
    private int sortColumn = 0;
    private boolean sortAscending = true;

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
        applyFilterAndSort();
    }

    /**
     * 设置排序列和方向（用户点击表头时调用）
     */
    public void setSort(int column, boolean ascending) {
        this.sortColumn = column;
        this.sortAscending = ascending;
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

        // 始终按列排序
        filtered.sort((a, b) -> compareEntries(a, b, sortColumn, sortAscending));

        this.displayedEntries = filtered;
        fireTableDataChanged();
    }

    /**
     * 检查文件条目是否匹配所有搜索关键词
     *
     * @param entry 文件条目
     * @return 是否匹配所有关键词
     */
    private boolean matchesAllKeywords(FileEntry entry) {
        String lowerName = entry.file.getName().toLowerCase();
        for (String kw : searchKeywords) {
            if (!lowerName.contains(kw.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较两个文件条目，支持按指定列和排序方向进行比较
     *
     * @param a         第一个文件条目
     * @param b         第二个文件条目
     * @param column    排序列索引
     * @param ascending 是否升序
     * @return 比较结果
     */
    private int compareEntries(FileEntry a, FileEntry b, int column, boolean ascending) {
        int cmp = 0;
        switch (column) {
            case 0: // 名称
                cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                break;
            case 1: // 标签 - 按标签分数排序
                cmp = compareTagsByScore(a.file.getName(), b.file.getName());
                if (cmp == 0) {
                    cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                }
                break;
            case 2: // 修改日期
                cmp = Long.compare(a.file.lastModified(), b.file.lastModified());
                break;
            case 3: // 类型
                cmp = a.type.compareToIgnoreCase(b.type);
                if (cmp == 0) {
                    cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                }
                break;
            case 4: // 大小
                cmp = Long.compare(a.file.length(), b.file.length());
                break;
            default:
                cmp = 0;
        }
        return ascending ? cmp : -cmp;
    }

    /**
     * 按标签分数比较两个文件
     * 配置文件最后的标签是10分，倒数第二是20分，以此增加
     * 没有标签的是0分
     *
     * @param fileNameA 文件名A
     * @param fileNameB 文件名B
     * @return 比较结果
     */
    private int compareTagsByScore(String fileNameA, String fileNameB) {
        int scoreA = calculateTagScore(fileNameA);
        int scoreB = calculateTagScore(fileNameB);
        return Integer.compare(scoreA, scoreB);
    }

    /**
     * 计算文件的标签分数总和
     * 配置文件最后的标签是10分，倒数第二是20分，依次增加
     * 没有标签或标签不在配置中的是0分
     * 特殊标签处理：
     * - {文件名} 不计算分数
     * - {当前日期} 匹配文件中的日期标签（如 20260513）
     * - {版本号} 匹配文件中的版本号标签（如 V1、V2）
     *
     * @param fileName 文件名
     * @return 标签分数总和
     */
    private int calculateTagScore(String fileName) {
        List<String> tags = FileUtil.parseAllTags(fileName);
        if (tags.isEmpty()) {
            return 0;
        }

        int totalScore = 0;
        int configSize = Config.tags.size();

        for (String tag : tags) {
            // 跳过 {文件名} 特殊标签
            if (FileUtil.TAG_ORDER_FILENAME.equals(tag)) {
                continue;
            }

            // 检查是否是日期标签（如 20260513）
            if (FileUtil.DATE_TAG_PATTERN.matcher(tag).matches()) {
                int dateIndex = Config.tags.indexOf(FileUtil.TAG_ORDER_DATE);
                if (dateIndex >= 0) {
                    int score = (configSize - dateIndex) * 10;
                    totalScore += score;
                }
                continue;
            }

            // 检查是否是版本号标签（如 V1、V2）
            if (FileUtil.VERSION_TAG_PATTERN.matcher(tag).matches()) {
                int versionIndex = Config.tags.indexOf(FileUtil.TAG_ORDER_VERSION);
                if (versionIndex >= 0) {
                    int score = (configSize - versionIndex) * 10;
                    totalScore += score;
                }
                continue;
            }

            // 普通标签：在配置中查找
            int index = Config.tags.indexOf(tag);
            if (index >= 0) {
                int score = (configSize - index) * 10;
                totalScore += score;
            }
        }

        return totalScore;
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
            case 0: return String.class;  // 名称
            case 1: return String.class;  // 标签
            case 2: return String.class;  // 修改日期
            case 3: return String.class;  // 类型
            case 4: return String.class;  // 大小
            default: return Object.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= displayedEntries.size()) {
            return "";
        }
        FileEntry entry = displayedEntries.get(rowIndex);
        switch (columnIndex) {
            case 0: { // 名称（过滤标签，显示纯文件名）
                String rawName = entry.file.getName();
                int dot = rawName.lastIndexOf('.');
                String base = dot > 0 ? rawName.substring(0, dot) : rawName;
                String ext = dot > 0 ? rawName.substring(dot) : "";
                String cleanName = FileUtil.getAllTagsPattern().matcher(base).replaceAll("").trim();
                return cleanName.isEmpty() ? rawName : cleanName + ext;
            }
            case 1: { // 标签
                List<String> tags = FileUtil.parseAllTags(entry.file.getName());
                return String.join(",", tags);
            }
            case 2: // 修改日期
                return DATE_FORMAT.format(new Date(entry.file.lastModified()));
            case 3: // 类型
                return entry.type;
            case 4: // 大小
                return formatFileSize(entry.file.length());
            default:
                return "";
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
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
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
