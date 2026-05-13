package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.Config;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 静态配置工具类，支持直接通过类名调用，无需实例化
 *
 * 使用方法：
 * 1. 初始化配置（默认路径）：ConfigUtil.init()
 * 2. 初始化配置（指定路径）：ConfigUtil.init("/path/to/config.conf")
 * 3. 重新加载配置：ConfigUtil.reload()
 * 4. 获取配置值：ConfigUtil.get("key", "default")
 * 5. 保存配置：ConfigUtil.saveFromConfig("comment")
 */
public final class ConfigUtil {

    private static Properties properties;
    private static Path configFilePath;

    /**
     * 私有构造函数，防止实例化
     * <p>
     * 本类为静态工具类，所有方法均为静态方法，无需也不应该创建实例
     */
    private ConfigUtil() {
    }

    /**
     * 初始化配置（默认路径）
     * <p>
     * 默认配置文件路径：用户主目录/.filenametagtool/filename-tagtool.conf
     */
    public static void init() {
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, Config.CONFIG_FILE_PATH);
        configFilePath = appDir.resolve(Config.CONFIG_FILE_NAME);

        properties = new Properties();

        reload();
    }

    /**
     * 初始化配置（指定路径）
     *
     * @param configPath 配置文件路径
     */
    public static void init(String configPath) {
        if (configPath == null || configPath.isEmpty()) {
            init();
            return;
        }

        configFilePath = Paths.get(configPath).toAbsolutePath();
        properties = new Properties();

        reload();
    }

    /**
     * 重新加载配置文件
     * <p>
     * 如果配置文件不存在，会创建目录结构
     * 如果配置文件存在且可读，会加载配置内容
     * 最后从Properties对象填充到Config静态字段
     */
    public static void reload() {
        Path file = configFilePath;
        if (!Files.exists(file)) {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                try {
                    Files.createDirectories(parent);
                } catch (IOException e) {
                    SwingUtil.showError("创建配置目录失败: " + e.getMessage());
                }
            }
        } else if (Files.isReadable(file)) {
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
                properties.load(isr);
            } catch (IOException e) {
                SwingUtil.showError("加载配置失败: " + e.getMessage());
            }
        } else {
            SwingUtil.showError("配置文件不可读: " + file.toAbsolutePath());
        }

        setConfig();
    }

    /**
     * 从Properties对象读取值填充到Config静态字段
     */
    public static void setConfig() {
        Config.windowX = getInt(Config.KEY_WINDOW_X, Config.windowX);
        Config.windowY = getInt(Config.KEY_WINDOW_Y, Config.windowY);
        Config.windowW = getInt(Config.KEY_WINDOW_W, Config.windowW);
        Config.windowH = getInt(Config.KEY_WINDOW_H, Config.windowH);
        Config.divider = getInt(Config.KEY_DIVIDER, Config.divider);
        Config.groupTagsWindowX = getInt(Config.KEY_GROUP_TAGS_WINDOW_X, Config.groupTagsWindowX);
        Config.groupTagsWindowY = getInt(Config.KEY_GROUP_TAGS_WINDOW_Y, Config.groupTagsWindowY);
        Config.groupTagsWindowWidth = getInt(Config.KEY_GROUP_TAGS_WINDOW_WIDTH, Config.groupTagsWindowWidth);
        Config.groupTagsWindowHeight = getInt(Config.KEY_GROUP_TAGS_WINDOW_HEIGHT, Config.groupTagsWindowHeight);
        Config.iconPath = properties.getProperty(Config.KEY_ICON_PATH, Config.iconPath);
        Config.tags = getList(Config.KEY_TAGS, Config.DELIMITER);
        Config.addTagHorizontalDivider = getInt(Config.KEY_ADD_TAG_HORIZONTAL_DIVIDER, Config.addTagHorizontalDivider);
        Config.addTagVerticalDivider = getInt(Config.KEY_ADD_TAG_VERTICAL_DIVIDER, Config.addTagVerticalDivider);
        Config.addTagSmartHistoryDivider = getInt(Config.KEY_ADD_TAG_SMART_HISTORY_DIVIDER, Config.addTagSmartHistoryDivider);
        Config.tagBracketStyle = properties.getProperty(Config.KEY_TAG_BRACKET_STYLE, Config.tagBracketStyle);
        Config.fileSortColumn = getInt(Config.KEY_FILE_SORT_COLUMN, Config.fileSortColumn);
        Config.fileSortAscending = getInt(Config.KEY_FILE_SORT_ASCENDING, Config.fileSortAscending ? 0 : 1) == 0;
        Config.fileColumnWidths = getIntArray(Config.KEY_FILE_COLUMN_WIDTHS, Config.fileColumnWidths);
        loadTagColors();
    }

    /**
     * 获取整型配置值
     *
     * @param key          配置键名
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取列表型配置值
     *
     * @param key       配置键名
     * @param delimiter 分隔符
     * @return 配置值列表
     */
    public static List<String> getList(String key, String delimiter) {
        String value = properties.getProperty(key);
        List<String> result = new ArrayList<>();
        if (value != null && !value.trim().isEmpty()) {
            for (String item : value.split(delimiter)) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    /**
     * 获取整型数组配置值
     *
     * @param key          配置键名
     * @param defaultValue 默认值
     * @return 配置值数组
     */
    public static int[] getIntArray(String key, int[] defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            String[] parts = value.split(",");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result.length > 0 ? result : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 设置字符串配置值
     *
     * @param key   配置键名
     * @param value 配置值
     */
    public static void set(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    /**
     * 设置整型配置值
     *
     * @param key   配置键名
     * @param value 配置值
     */
    public static void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    /**
     * 设置列表型配置值
     *
     * @param key       配置键名
     * @param values    配置值列表
     * @param delimiter 分隔符
     */
    public static void set(String key, List<String> values, String delimiter) {
        if (values == null || values.isEmpty()) {
            set(key, (String) null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(values.get(i));
        }
        set(key, sb.toString());
    }

    /**
     * 保存配置到文件
     */
    public static void save() {
        setProperties();
        writeProperties();
    }

    /**
     * 将Properties对象写入配置文件（带详细注释）
     */
    private static void writeProperties() {
        Path parent = configFilePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                SwingUtil.showError("创建配置目录失败: " + e.getMessage());
                return;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFilePath.toFile()), StandardCharsets.UTF_8))) {
            writer.write("# FileNameTagTool 配置文件");
            writer.newLine();
            writer.newLine();

            writePropertyLine(writer, "# 窗口位置和大小", Config.KEY_WINDOW_X, String.valueOf(Config.windowX));
            writePropertyLine(writer, "# 窗口位置和大小", Config.KEY_WINDOW_Y, String.valueOf(Config.windowY));
            writePropertyLine(writer, "# 窗口位置和大小", Config.KEY_WINDOW_W, String.valueOf(Config.windowW));
            writePropertyLine(writer, "# 窗口位置和大小", Config.KEY_WINDOW_H, String.valueOf(Config.windowH));
            writer.newLine();

            writePropertyLine(writer, "# 分割线位置", Config.KEY_DIVIDER, String.valueOf(Config.divider));
            writer.newLine();

            writePropertyLine(writer, "# 标签管理窗口位置和大小", Config.KEY_GROUP_TAGS_WINDOW_X, String.valueOf(Config.groupTagsWindowX));
            writePropertyLine(writer, "# 标签管理窗口位置和大小", Config.KEY_GROUP_TAGS_WINDOW_Y, String.valueOf(Config.groupTagsWindowY));
            writePropertyLine(writer, "# 标签管理窗口位置和大小", Config.KEY_GROUP_TAGS_WINDOW_WIDTH, String.valueOf(Config.groupTagsWindowWidth));
            writePropertyLine(writer, "# 标签管理窗口位置和大小", Config.KEY_GROUP_TAGS_WINDOW_HEIGHT, String.valueOf(Config.groupTagsWindowHeight));
            writer.newLine();

            writePropertyLine(writer, "# 图标文件目录路径", Config.KEY_ICON_PATH, Config.iconPath);
            writer.newLine();

            writePropertyLine(writer, "# 添加标签页-左右分隔线位置", Config.KEY_ADD_TAG_HORIZONTAL_DIVIDER, String.valueOf(Config.addTagHorizontalDivider));
            writePropertyLine(writer, "# 添加标签页-右侧上下分隔线位置", Config.KEY_ADD_TAG_VERTICAL_DIVIDER, String.valueOf(Config.addTagVerticalDivider));
            writePropertyLine(writer, "# 添加标签页-智能标签与历史标签分隔线位置", Config.KEY_ADD_TAG_SMART_HISTORY_DIVIDER, String.valueOf(Config.addTagSmartHistoryDivider));
            writer.newLine();

            writePropertyLine(writer, "# 标签包裹符号样式（fullwidth=全角【】 bracket=半角[]）", Config.KEY_TAG_BRACKET_STYLE, Config.tagBracketStyle);
            writer.newLine();

            writePropertyLine(writer, "# 文件列表排序列索引（0=名称, 1=修改日期, 2=类型, 3=大小）", Config.KEY_FILE_SORT_COLUMN, String.valueOf(Config.fileSortColumn));
            writePropertyLine(writer, "# 文件列表排序方向（0=升序, 1=降序）", Config.KEY_FILE_SORT_ASCENDING, Config.fileSortAscending ? "0" : "1");
            writePropertyLine(writer, "# 文件列表各列宽度（名称,标签,修改日期,类型,大小）", Config.KEY_FILE_COLUMN_WIDTHS, intArrayToString(Config.fileColumnWidths));
            writer.newLine();

            writePropertyLine(writer, "# 标签颜色配置", Config.KEY_TAG_COLORS, getTagColorsString());
            writer.newLine();

            writer.write("# 标签列表（多个标签用逗号分隔）");
            writer.newLine();
            writer.write(Config.KEY_TAGS + "=" + String.join(Config.DELIMITER, Config.tags));
            writer.newLine();
        } catch (IOException e) {
            SwingUtil.showError("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 写入单个属性行（带注释）
     */
    private static void writePropertyLine(BufferedWriter writer, String comment, String key, String value) throws IOException {
        writer.write(comment);
        writer.newLine();
        writer.write(key + "=" + value);
        writer.newLine();
    }

    /**
     * 将Config静态字段的值填充到Properties对象
     */
    private static void setProperties() {
        set(Config.KEY_WINDOW_X, Config.windowX);
        set(Config.KEY_WINDOW_Y, Config.windowY);
        set(Config.KEY_WINDOW_W, Config.windowW);
        set(Config.KEY_WINDOW_H, Config.windowH);
        set(Config.KEY_DIVIDER, Config.divider);
        set(Config.KEY_GROUP_TAGS_WINDOW_X, Config.groupTagsWindowX);
        set(Config.KEY_GROUP_TAGS_WINDOW_Y, Config.groupTagsWindowY);
        set(Config.KEY_GROUP_TAGS_WINDOW_WIDTH, Config.groupTagsWindowWidth);
        set(Config.KEY_GROUP_TAGS_WINDOW_HEIGHT, Config.groupTagsWindowHeight);
        set(Config.KEY_ICON_PATH, Config.iconPath);
        set(Config.KEY_TAGS, Config.tags, Config.DELIMITER);
        set(Config.KEY_ADD_TAG_HORIZONTAL_DIVIDER, Config.addTagHorizontalDivider);
        set(Config.KEY_ADD_TAG_VERTICAL_DIVIDER, Config.addTagVerticalDivider);
        set(Config.KEY_ADD_TAG_SMART_HISTORY_DIVIDER, Config.addTagSmartHistoryDivider);
        set(Config.KEY_TAG_BRACKET_STYLE, Config.tagBracketStyle);
        set(Config.KEY_FILE_SORT_COLUMN, Config.fileSortColumn);
        set(Config.KEY_FILE_SORT_ASCENDING, Config.fileSortAscending ? 0 : 1);
        set(Config.KEY_FILE_COLUMN_WIDTHS, intArrayToString(Config.fileColumnWidths));
        saveTagColors();
    }

    /**
     * 将整型数组转换为逗号分隔的字符串
     *
     * @param arr 整型数组
     * @return 逗号分隔的字符串
     */
    private static String intArrayToString(int[] arr) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * 从配置文件加载标签颜色
     */
    private static void loadTagColors() {
        Config.tagColors.clear();
        String value = properties.getProperty(Config.KEY_TAG_COLORS, "");
        if (value != null && !value.isEmpty()) {
            String[] pairs = value.split(",");
            for (String pair : pairs) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    try {
                        Config.tagColors.put(parts[0].trim(), Color.decode(parts[1].trim()));
                    } catch (NumberFormatException e) {
                        // 忽略无效的颜色值
                    }
                }
            }
        }
    }

    /**
     * 将标签颜色保存到Properties对象
     */
    private static void saveTagColors() {
        set(Config.KEY_TAG_COLORS, getTagColorsString());
    }

    /**
     * 获取标签颜色的字符串表示
     *
     * @return 标签颜色字符串
     */
    private static String getTagColorsString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Color> entry : Config.tagColors.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append(":").append(colorToHex(entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * 将颜色转换为十六进制字符串
     *
     * @param color 颜色对象
     * @return 十六进制颜色字符串（如 #FF0000）
     */
    private static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
