package cn.mdkml.filenametagtool.component;

import cn.mdkml.filenametagtool.util.FileUtil;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件名列渲染器：显示系统默认图标 + 文件名。
 * <p>
 * 使用 {@link FileSystemView} 获取文件/文件夹的系统图标，
 * 同一扩展名的图标会缓存以提高性能。
 * 文件名会过滤掉标签部分，只显示纯文件名。
 * 支持搜索关键词高亮显示。
 * </p>
 */
public class FileNameCellRenderer extends DefaultTableCellRenderer {

    private static final int ICON_SIZE = 16;
    private static final String DIR_CACHE_KEY = "__DIR__";

    private final Map<String, Icon> iconCache = new HashMap<>();
    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] keywords = new String[0];

    /**
     * 设置需要高亮的搜索关键词
     *
     * @param keywords 关键词数组
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords != null ? keywords : new String[0];
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (!(value instanceof FileTableModel.FileEntry entry)) {
            label.setText(value != null ? value.toString() : "");
            label.setIcon(null);
            return label;
        }

        // 设置图标
        Icon icon = getIcon(entry.file);
        label.setIcon(icon);

        // 设置文本（过滤标签，显示纯文件名，支持高亮）
        String cleanName = getCleanFileName(entry.file);
        label.setText(highlightText(cleanName));
        label.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));

        return label;
    }

    /**
     * 获取文件的系统图标（带缓存）
     *
     * @param file 文件或文件夹
     * @return 系统图标
     */
    private Icon getIcon(File file) {
        String cacheKey = file.isDirectory() ? DIR_CACHE_KEY : getExtensionKey(file);
        return iconCache.computeIfAbsent(cacheKey, k -> {
            Icon icon = fileSystemView.getSystemIcon(file);
            if (icon != null) {
                // 确保图标大小一致
                if (icon.getIconWidth() != ICON_SIZE || icon.getIconHeight() != ICON_SIZE) {
                    Image img = ((ImageIcon) icon).getImage().getScaledInstance(
                            ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                }
            }
            return icon;
        });
    }

    /**
     * 获取文件扩展名作为缓存键（小写）
     *
     * @param file 文件
     * @return 扩展名键，如 ".pdf"；无扩展名返回文件名本身
     */
    private String getExtensionKey(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            return name.substring(dot).toLowerCase();
        }
        return name;
    }

    /**
     * 获取过滤标签后的纯文件名
     *
     * @param file 文件
     * @return 纯文件名
     */
    private String getCleanFileName(File file) {
        String rawName = file.getName();
        int dot = rawName.lastIndexOf('.');
        String base = dot > 0 ? rawName.substring(0, dot) : rawName;
        String ext = dot > 0 ? rawName.substring(dot) : "";
        String cleanName = FileUtil.getAllTagsPattern().matcher(base).replaceAll("").trim();
        return cleanName.isEmpty() ? rawName : cleanName + ext;
    }

    /**
     * 对文本应用搜索关键词高亮
     *
     * @param text 原始文本
     * @return 带高亮的 HTML 文本，无关键词时返回原始文本
     */
    private String highlightText(String text) {
        if (text == null || keywords.length == 0) {
            return text;
        }

        String lowerText = text.toLowerCase();

        // 找出所有匹配区间
        List<int[]> highlights = new ArrayList<>();
        for (String kw : keywords) {
            String lowerKw = kw.toLowerCase();
            int from = 0;
            while ((from = lowerText.indexOf(lowerKw, from)) >= 0) {
                highlights.add(new int[]{from, from + kw.length()});
                from += kw.length();
            }
        }

        if (highlights.isEmpty()) {
            return text;
        }

        // 合并重叠区间
        highlights.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] interval : highlights) {
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < interval[0]) {
                merged.add(interval);
            } else {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], interval[1]);
            }
        }

        // 构建 HTML 高亮
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        StringBuilder html = new StringBuilder("<html><nobr>");
        int lastEnd = 0;
        for (int[] interval : merged) {
            int start = interval[0];
            int end = interval[1];
            if (start > lastEnd) {
                html.append(escaped, lastEnd, start);
            }
            html.append("<span style=\"background:#FFEB3B;color:#000;padding:1px 2px\">");
            html.append(escaped, start, end);
            html.append("</span>");
            lastEnd = end;
        }
        if (lastEnd < escaped.length()) {
            html.append(escaped, lastEnd, escaped.length());
        }
        html.append("</nobr></html>");

        return html.toString();
    }
}
