package local.filenametagtool.util;

import local.filenametagtool.model.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
 * 5. 保存配置：ConfigUtil.saveFromConfig(config, "comment")
 */
public final class ConfigUtil {

    private static final String DEFAULT_CONFIG_FILE_NAME = "filename-tagtool.conf";

    private static Properties properties;
    private static Path configFilePath;

    private ConfigUtil() {
    }

    /**
     * 重新加载配置文件
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

        loadFromProperties();
    }

    /**
     * 从Properties对象读取值填充到Config静态字段
     */
    public static void loadFromProperties() {
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

        String tagsStr = properties.getProperty(Config.KEY_TAGS, "");
        if (tagsStr != null && !tagsStr.trim().isEmpty()) {
            Config.tags = new ArrayList<>(Arrays.asList(tagsStr.split(",")));
        }
    }

    /**
     * 初始化配置（默认路径）
     */
    public static void init() {
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, ".filenametagtool");
        configFilePath = appDir.resolve(DEFAULT_CONFIG_FILE_NAME);

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

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    public static double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

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

    public static void set(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    public static void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    public static void set(String key, long value) {
        set(key, String.valueOf(value));
    }

    public static void set(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    public static void set(String key, double value) {
        set(key, String.valueOf(value));
    }

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

    public static void save(String comment) {
        Path parent = configFilePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                System.err.println("创建配置目录失败: " + e.getMessage());
                return;
            }
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(configFilePath.toFile()), StandardCharsets.UTF_8)) {
            properties.store(osw, comment);
        } catch (IOException e) {
            System.err.println("保存配置失败: " + e.getMessage());
        }
    }

    public static void save() {
        save(null);
    }

    public static void saveFromConfig(String comment) {
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

        if (Config.tags != null && !Config.tags.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Config.tags.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(Config.tags.get(i));
            }
            set(Config.KEY_TAGS, sb.toString());
        } else {
            set(Config.KEY_TAGS, (String) null);
        }

        save(comment);
    }

    public static boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    public static void remove(String key) {
        properties.remove(key);
    }

    public static void clear() {
        properties.clear();
    }

    public static List<String> getKeys() {
        List<String> keys = new ArrayList<>();
        for (Object key : properties.keySet()) {
            keys.add(key.toString());
        }
        return keys;
    }

    public static Path getConfigFilePath() {
        return configFilePath;
    }

    public static Properties getProperties() {
        return (Properties) properties.clone();
    }
}