package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
                    System.err.println("创建配置目录失败: " + e.getMessage());
                }
            }
        } else if (Files.isReadable(file)) {
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
                properties.load(isr);
            } catch (IOException e) {
                System.err.println("加载配置失败: " + e.getMessage());
            }
        } else {
            System.err.println("配置文件不可读: " + file.toAbsolutePath());
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
        Config.everythingPath = properties.getProperty(Config.KEY_EVERYTHING_PATH, Config.everythingPath);
        Config.iconPath = properties.getProperty(Config.KEY_ICON_PATH, Config.iconPath);
        Config.tags = getList(Config.KEY_TAGS, Config.DELIMITER);
        Config.addTagHorizontalDivider = getInt(Config.KEY_ADD_TAG_HORIZONTAL_DIVIDER, Config.addTagHorizontalDivider);
        Config.addTagVerticalDivider = getInt(Config.KEY_ADD_TAG_VERTICAL_DIVIDER, Config.addTagVerticalDivider);
    }

    /**
     * 获取整型配置值
     * @param key 配置键名
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * 获取列表型配置值
     * @param key 配置键名
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
     * 设置字符串配置值
     * @param key 配置键名
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
     * @param key 配置键名
     * @param value 配置值
     */
    public static void set(String key, int value) {
        set(key, String.valueOf(value));
    }


    /**
     * 设置列表型配置值
     * @param key 配置键名
     * @param values 配置值列表
     * @param delimiter 分隔符
     */
    public static void set(String key, List<String> values, String delimiter) {
        if (values == null || values.isEmpty()) {
            set(key, (String) null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(delimiter);
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
                System.err.println("创建配置目录失败: " + e.getMessage());
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

            writePropertyLine(writer, "# Everything工具路径", Config.KEY_EVERYTHING_PATH, Config.everythingPath);
            writer.newLine();

            writePropertyLine(writer, "# 图标文件目录路径", Config.KEY_ICON_PATH, Config.iconPath);
            writer.newLine();

            writePropertyLine(writer, "# 添加标签页-左右分隔线位置", Config.KEY_ADD_TAG_HORIZONTAL_DIVIDER, String.valueOf(Config.addTagHorizontalDivider));
            writePropertyLine(writer, "# 添加标签页-右侧上下分隔线位置", Config.KEY_ADD_TAG_VERTICAL_DIVIDER, String.valueOf(Config.addTagVerticalDivider));
            writer.newLine();

            writer.write("# 标签列表（多个标签用逗号分隔）");
            writer.newLine();
            writer.write(Config.KEY_TAGS + "=" + String.join(Config.DELIMITER, Config.tags));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("保存配置失败: " + e.getMessage());
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
        set(Config.KEY_EVERYTHING_PATH, Config.everythingPath);
        set(Config.KEY_ICON_PATH, Config.iconPath);
        set(Config.KEY_TAGS, Config.tags, Config.DELIMITER);
        set(Config.KEY_ADD_TAG_HORIZONTAL_DIVIDER, Config.addTagHorizontalDivider);
        set(Config.KEY_ADD_TAG_VERTICAL_DIVIDER, Config.addTagVerticalDivider);
    }


}