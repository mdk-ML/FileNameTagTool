package cn.mdkml.filenametagtool.component;

import cn.mdkml.filenametagtool.model.Config;
import cn.mdkml.filenametagtool.util.FileUtil;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 文件列表表格模型，支持多关键词搜索过滤和按列排序。
 *
 * 排序逻辑：
 * - 搜索时保持按表头列排序
 * - 名称列排序：Win11 风格（逐字符比较，特殊字符 < 数字 < 字母 < 汉字）
 * - 标签列排序：按标签分数总和排序（配置文件最后的标签10分，倒数第二20分，以此类推）
 */
public class FileTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"名称", "标签", "路径", "修改日期", "类型", "大小"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private final List<FileEntry> allEntries = new ArrayList<>();
    private List<FileEntry> displayedEntries = new ArrayList<>();
    private String[] searchKeywords = new String[0];
    private int sortColumn = 0;
    private boolean sortAscending = true;
    private String rootPath = "";  // 根目录路径，用于计算相对路径

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
     * 递归扫描目录，加载所有文件和子文件夹
     *
     * @param rootPath 根目录路径
     */
    public void setFilesRecursive(String rootPath) {
        allEntries.clear();
        this.rootPath = rootPath;
        try {
            Files.walkFileTree(Path.of(rootPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // 跳过根目录自身（只添加其子项）
                    if (!dir.toString().equals(rootPath)) {
                        allEntries.add(new FileEntry(dir.toFile()));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    allEntries.add(new FileEntry(file.toFile()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // 跳过无法访问的文件
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // 降级为非递归加载
            File rootDir = new File(rootPath);
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    allEntries.add(new FileEntry(f));
                }
            }
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
            total += entry.size;
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
        // 文件始终排在文件夹前面（不受排序方向影响）
        if (a.isDirectory != b.isDirectory) {
            return a.isDirectory ? 1 : -1;
        }

        int cmp = 0;
        switch (column) {
            case 0: // 名称（Win11 风格：逐字符比较，特殊字符 < 数字 < 字母 < 汉字）
                cmp = compareNamesWin11Style(a.file.getName(), b.file.getName());
                break;
            case 1: // 标签 - 按标签分数排序
                cmp = compareTagsByScore(a.file.getName(), b.file.getName());
                if (cmp == 0) {
                    cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                }
                break;
            case 2: // 路径 - 按相对路径排序
                cmp = getRelativePath(a).compareToIgnoreCase(getRelativePath(b));
                if (cmp == 0) {
                    cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                }
                break;
            case 3: // 修改日期
                cmp = Long.compare(a.file.lastModified(), b.file.lastModified());
                break;
            case 4: // 类型
                cmp = a.type.compareToIgnoreCase(b.type);
                if (cmp == 0) {
                    cmp = a.file.getName().compareToIgnoreCase(b.file.getName());
                }
                break;
            case 5: // 大小（文件夹为递归累加大小）
                cmp = Long.compare(a.size, b.size);
                break;
            default:
                cmp = 0;
        }
        return ascending ? cmp : -cmp;
    }

    /**
     * Win11 风格文件名比较
     * <p>
     * 排序规则：
     * 1. 逐字符比较，从第一个字符开始
     * 2. 字符类型优先级：特殊字符(0) < 数字(1) < 字母(2) < 汉字(3)
     * 3. 数字按数值比较（自然排序），字母按字母顺序，汉字按拼音比较
     * 4. 长度不同时，较短的排在前面
     * </p>
     *
     * @param nameA 文件名A
     * @param nameB 文件名B
     * @return 比较结果
     */
    private int compareNamesWin11Style(String nameA, String nameB) {
        // 使用中文拼音排序器
        Collator pinyinCollator = Collator.getInstance(Locale.CHINA);
        pinyinCollator.setStrength(Collator.PRIMARY); // 忽略大小写

        int i = 0;
        int j = 0;
        while (i < nameA.length() && j < nameB.length()) {
            char c1 = nameA.charAt(i);
            char c2 = nameB.charAt(j);
            int type1 = getCharType(c1);
            int type2 = getCharType(c2);

            // 不同类型：按优先级比较（特殊字符 < 数字 < 字母 < 汉字）
            if (type1 != type2) {
                return Integer.compare(type1, type2);
            }

            // 同类型比较
            switch (type1) {
                case 1: // 数字 vs 数字：按数值比较
                    String num1 = extractNumber(nameA, i);
                    String num2 = extractNumber(nameB, j);
                    int cmpNum = Long.compare(Long.parseLong(num1), Long.parseLong(num2));
                    if (cmpNum != 0) {
                        return cmpNum;
                    }
                    i += num1.length();
                    j += num2.length();
                    break;
                case 2: // 字母 vs 字母：按字母顺序比较
                    int cmpLetter = Character.toLowerCase(c1) - Character.toLowerCase(c2);
                    if (cmpLetter != 0) {
                        return cmpLetter;
                    }
                    i++;
                    j++;
                    break;
                case 3: // 汉字 vs 汉字：按拼音比较
                    int cmpPinyin = pinyinCollator.compare(String.valueOf(c1), String.valueOf(c2));
                    if (cmpPinyin != 0) {
                        return cmpPinyin;
                    }
                    i++;
                    j++;
                    break;
                default: // 特殊字符 vs 特殊字符：按字符值比较
                    int cmpSpecial = c1 - c2;
                    if (cmpSpecial != 0) {
                        return cmpSpecial;
                    }
                    i++;
                    j++;
                    break;
            }
        }

        // 长度不同：较短的排在前面
        return Integer.compare(nameA.length(), nameB.length());
    }

    /**
     * 判断字符类型
     *
     * @param c 字符
     * @return 0=特殊字符, 1=数字, 2=字母, 3=汉字
     */
    private int getCharType(char c) {
        if (Character.isDigit(c)) {
            return 1;
        }
        if (Character.isLetter(c)) {
            // 判断是否为汉字（CJK统一汉字）
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return 3;
            }
            return 2;
        }
        return 0;
    }

    /**
     * 从指定位置开始提取连续数字
     *
     * @param str   字符串
     * @param start 起始位置
     * @return 数字字符串
     */
    private String extractNumber(String str, int start) {
        StringBuilder sb = new StringBuilder();
        while (start < str.length() && Character.isDigit(str.charAt(start))) {
            sb.append(str.charAt(start));
            start++;
        }
        return sb.toString();
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
            case 0: return FileEntry.class;  // 名称（含图标）
            case 1: return String.class;  // 标签
            case 2: return String.class;  // 路径
            case 3: return String.class;  // 修改日期
            case 4: return String.class;  // 类型
            case 5: return String.class;  // 大小
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
            case 0: // 名称（返回 FileEntry，由 FileNameCellRenderer 渲染图标+文件名）
                return entry;
            case 1: { // 标签
                List<String> tags = FileUtil.parseAllTags(entry.file.getName());
                return String.join(",", tags);
            }
            case 2: // 路径（相对路径）
                return getRelativePath(entry);
            case 3: // 修改日期
                return DATE_FORMAT.format(new Date(entry.file.lastModified()));
            case 4: // 类型
                return entry.type;
            case 5: // 大小（文件夹为递归累加大小）
                return FileUtil.formatFileSize(entry.size);
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
        if (file.isDirectory()) {
            return "文件夹";
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            String ext = name.substring(dot + 1).toUpperCase();
            return ext + " 文件";
        }
        return "文件";
    }

    /**
     * 获取文件相对于根目录的路径
     * <p>
     * 文件夹返回自身相对路径，文件返回父目录相对路径。
     * 如果无法计算相对路径，则返回空字符串。
     * </p>
     *
     * @param entry 文件条目
     * @return 相对路径字符串
     */
    private String getRelativePath(FileEntry entry) {
        if (rootPath.isEmpty()) {
            return "";
        }
        try {
            Path filePath = entry.file.toPath().toAbsolutePath().normalize();
            Path root = Path.of(rootPath).toAbsolutePath().normalize();
            Path relative = root.relativize(filePath);
            // 文件：取父目录的相对路径
            if (entry.file.isFile()) {
                Path parent = relative.getParent();
                return parent != null ? parent.toString() : "";
            }
            // 文件夹：返回自身相对路径
            return relative.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 文件条目，缓存文件类型等元数据
     */
    static class FileEntry {
        final File file;
        final String type;
        final boolean isDirectory;
        final long size;

        FileEntry(File file) {
            this.file = file;
            this.isDirectory = file.isDirectory();
            this.type = getFileType(file);
            this.size = calculateSize(file);
        }
    }

    /**
     * 计算文件或文件夹的大小（字节）
     * <p>
     * 文件直接返回 {@link File#length()}。
     * 文件夹递归累加所有子文件的大小。
     * </p>
     *
     * @param file 文件或文件夹
     * @return 大小（字节）
     */
    private static long calculateSize(File file) {
        if (file.isFile()) {
            return file.length();
        }
        long total = 0;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                total += calculateSize(child);
            }
        }
        return total;
    }
}
