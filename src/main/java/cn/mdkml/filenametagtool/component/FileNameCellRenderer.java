package cn.mdkml.filenametagtool.component;

import cn.mdkml.filenametagtool.util.FileUtil;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件名列渲染器：显示系统默认图标 + 文件名。
 * <p>
 * 使用 {@link FileSystemView} 获取文件/文件夹的系统图标，
 * 同一扩展名的图标会缓存以提高性能。
 * 文件名会过滤掉标签部分，只显示纯文件名。
 * </p>
 */
public class FileNameCellRenderer extends DefaultTableCellRenderer {

    private static final int ICON_SIZE = 16;
    private static final String DIR_CACHE_KEY = "__DIR__";

    private final Map<String, Icon> iconCache = new HashMap<>();
    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

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

        // 设置文本（过滤标签，显示纯文件名）
        label.setText(getCleanFileName(entry.file));
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
}
