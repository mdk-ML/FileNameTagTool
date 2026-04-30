package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.Config;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class FileUtil {

    /** 版本号标签正则：V数字（不含括号的内部内容） */
    public static final Pattern VERSION_TAG_PATTERN = Pattern.compile("^V\\d+$");
    /** 匹配所有【...】标签（不限于前导位置） */
    private static final Pattern ALL_TAGS_PATTERN = Pattern.compile("【[^】]*】");
    /** 标签排序中的特殊占位：源文件名位置 */
    public static final String TAG_ORDER_FILENAME = "{文件名}";
    /** 标签排序中的特殊占位：版本号位置 */
    public static final String TAG_ORDER_VERSION = "{版本号}";

    /**
     * 向文件名前缀添加标签
     *
     * @param path    文件路径
     * @param addTags 要添加的标签列表
     * @return 是否成功添加标签
     * @throws IOException 如果文件操作失败
     */
    public static boolean addTagsToNamePrefix(Path path, List<String> addTags) throws IOException {
        if (addTags == null || addTags.isEmpty()) return false;

        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = addTagsToLeafPreserveExt(leaf, addTags);
        if (newLeaf.equals(leaf)) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.move(path, target);
        return true;
    }

    /**
     * 移除文件名中的所有标签
     *
     * @param path 文件路径
     * @return 是否成功移除标签
     * @throws IOException 如果文件操作失败
     */
    public static boolean removeAllTags(Path path) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = removeTagsFromLeafPreserveExt(leaf);
        if (newLeaf.equals(leaf)) return false;
        if (newLeaf.trim().isEmpty()) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.move(path, target);
        return true;
    }

    /**
     * 移除文件名中的指定标签
     *
     * @param path         文件路径
     * @param tagsToRemove 要移除的标签集合
     * @return 是否成功移除标签
     * @throws IOException 如果文件操作失败
     */
    public static boolean removeTags(Path path, Set<String> tagsToRemove) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = removeSpecificTagsFromLeafPreserveExt(leaf, tagsToRemove);
        if (newLeaf.equals(leaf)) return false;
        if (newLeaf.trim().isEmpty()) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.move(path, target);
        return true;
    }

    /**
     * 创建文件的新版本
     *
     * @param path 文件路径
     * @return 是否成功创建新版本
     * @throws IOException 如果文件操作失败
     */
    public static boolean createNewVersion(Path path) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = generateNewVersionName(leaf);
        if (newLeaf.equals(leaf)) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
        return true;
    }

    /**
     * 复制文件并移除标签
     *
     * @param path 文件路径
     * @return 是否成功复制
     * @throws IOException 如果文件操作失败
     */
    public static boolean copyWithoutTags(Path path) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = removeTagsFromLeafPreserveExt(leaf);
        if (newLeaf.equals(leaf)) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
        return true;
    }

    /**
     * 重排文件名中的标签顺序，使其按照 Config.tags 的全局顺序排列。
     *
     * @param path 文件路径
     * @return 是否成功重排
     * @throws IOException 如果文件操作失败
     */
    public static boolean reorderTags(Path path) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) {
            return false;
        }

        String leaf = fileName.toString();
        int dot = leaf.lastIndexOf('.');
        String base;
        String ext;
        if (dot > 0) {
            base = leaf.substring(0, dot);
            ext = leaf.substring(dot);
        } else {
            base = leaf;
            ext = "";
        }

        List<String> existing = parseAllTags(base);
        String rest = ALL_TAGS_PATTERN.matcher(base).replaceAll("");
        if (existing.isEmpty()) {
            return false;
        }

        String newLeaf = buildOrderedPrefix(rest, existing) + ext;
        if (newLeaf.equals(leaf)) {
            return false;
        }

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);
        Files.move(path, target);
        return true;
    }

    /**
     * 确保目标路径不存在，如果存在则添加序号
     *
     * @param target 目标路径
     * @return 确保不存在的路径
     */
    public static Path ensureNonExisting(Path target) {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return target;

        Path parent = target.getParent();
        String leaf = target.getFileName().toString();

        String base = leaf;
        String ext = "";
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            base = leaf.substring(0, dot);
            ext = leaf.substring(dot);
        }

        for (int i = 1; i < 10000; i++) {
            Path candidate = parent.resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) return candidate;
        }
        return target;
    }

    /**
     * 向文件名添加标签，保留扩展名
     *
     * @param leaf    文件名
     * @param addTags 要添加的标签列表
     * @return 新的文件名
     */
    private static String addTagsToLeafPreserveExt(String leaf, List<String> addTags) {
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            String base = leaf.substring(0, dot);
            String ext = leaf.substring(dot);
            return addTagsToBase(base, addTags) + ext;
        }
        return addTagsToBase(leaf, addTags);
    }

    /**
     * 向文件基础名添加标签
     *
     * @param base    文件基础名
     * @param addTags 要添加的标签列表
     * @return 新的文件基础名
     */
    private static String addTagsToBase(String base, List<String> addTags) {
        List<String> existing = parseAllTags(base);
        String rest = ALL_TAGS_PATTERN.matcher(base).replaceAll("");

        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        for (String tag : addTags) {
            if (!containsIgnoreCase(merged, tag)) {
                merged.add(tag);
            }
        }
        for (String tag : existing) {
            if (!containsIgnoreCase(merged, tag)) {
                merged.add(tag);
            }
        }

        return buildOrderedPrefix(rest, new ArrayList<>(merged));
    }

    /**
     * 按照 Config.tags 中的全局顺序对标签列表排序。
     * 不在 Config.tags 中的标签追加到末尾。
     *
     * @param tags 待排序的标签列表
     * @return 排序后的标签列表
     */
    private static List<String> sortByConfigOrder(List<String> tags) {
        List<String> configOrder = Config.tags;
        if (configOrder.isEmpty()) {
            return tags;
        }

        List<String> normalTags = new ArrayList<>();
        List<String> versionTags = new ArrayList<>();
        for (String tag : tags) {
            if (VERSION_TAG_PATTERN.matcher(tag).matches()) {
                versionTags.add(tag);
            } else {
                normalTags.add(tag);
            }
        }

        List<String> result = new ArrayList<>();
        for (String configTag : configOrder) {
            if (TAG_ORDER_FILENAME.equals(configTag)) {
                continue;
            }
            if (TAG_ORDER_VERSION.equals(configTag)) {
                result.addAll(versionTags);
                versionTags.clear();
                continue;
            }
            for (String tag : normalTags) {
                if (tag.equalsIgnoreCase(configTag) && !containsIgnoreCase(result, tag)) {
                    result.add(tag);
                    break;
                }
            }
        }
        // 追加剩余版本号标签
        result.addAll(versionTags);
        // 追加不在配置中的普通标签
        for (String tag : normalTags) {
            if (!containsIgnoreCase(result, tag)) {
                result.add(tag);
            }
        }
        return result;
    }

    /**
     * 按照 Config.tags 的全局顺序构建文件名前缀。
     * 支持"文件名"和"版本号"特殊占位：
     * - "文件名"决定源文件名（rest）的插入位置
     * - "版本号"决定版本号标签的插入位置
     *
     * @param baseName 源文件名（不含标签和扩展名）
     * @param tags     标签列表
     * @return 构建好的文件名前缀（含标签和源文件名）
     */
    private static String buildOrderedPrefix(String baseName, List<String> tags) {
        List<String> configOrder = Config.tags;
        List<String> orderedTags = sortByConfigOrder(tags);
        // 分离版本号标签和普通标签
        List<String> normalTags = new ArrayList<>();
        List<String> versionTags = new ArrayList<>();
        for (String tag : orderedTags) {
            if (VERSION_TAG_PATTERN.matcher(tag).matches()) {
                versionTags.add(tag);
            } else {
                normalTags.add(tag);
            }
        }

        StringBuilder prefix = new StringBuilder();
        int normalIndex = 0;
        int versionIndex = 0;
        boolean baseNameInserted = false;

        if (configOrder.isEmpty()) {
            // 无配置：所有标签 + 文件名
            for (String tag : normalTags) {
                prefix.append("【").append(tag).append("】");
            }
            for (String tag : versionTags) {
                prefix.append("【").append(tag).append("】");
            }
            prefix.append(baseName);
            return prefix.toString();
        }

        for (String orderItem : configOrder) {
            if (TAG_ORDER_FILENAME.equals(orderItem)) {
                prefix.append(baseName);
                baseNameInserted = true;
            } else if (TAG_ORDER_VERSION.equals(orderItem)) {
                while (versionIndex < versionTags.size()) {
                    prefix.append("【").append(versionTags.get(versionIndex)).append("】");
                    versionIndex++;
                }
            } else {
                // 普通标签位置
                if (normalIndex < normalTags.size()) {
                    prefix.append("【").append(normalTags.get(normalIndex)).append("】");
                    normalIndex++;
                }
            }
        }
        // 追加剩余标签
        while (normalIndex < normalTags.size()) {
            prefix.append("【").append(normalTags.get(normalIndex)).append("】");
            normalIndex++;
        }
        while (versionIndex < versionTags.size()) {
            prefix.append("【").append(versionTags.get(versionIndex)).append("】");
            versionIndex++;
        }
        // 如果文件名未在配置中指定位置，追加到末尾
        if (!baseNameInserted) {
            prefix.append(baseName);
        }
        return prefix.toString();
    }

    /**
     * 移除文件名中的所有标签，保留扩展名
     *
     * @param leaf 文件名
     * @return 新的文件名
     */
    private static String removeTagsFromLeafPreserveExt(String leaf) {
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            String base = leaf.substring(0, dot);
            String ext = leaf.substring(dot);
            return ALL_TAGS_PATTERN.matcher(base).replaceAll("") + ext;
        }
        return ALL_TAGS_PATTERN.matcher(leaf).replaceAll("");
    }

    /**
     * 移除文件名中的指定标签，保留扩展名
     *
     * @param leaf         文件名
     * @param tagsToRemove 要移除的标签集合
     * @return 新的文件名
     */
    private static String removeSpecificTagsFromLeafPreserveExt(String leaf, Set<String> tagsToRemove) {
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            String base = leaf.substring(0, dot);
            String ext = leaf.substring(dot);
            String cleaned = removeSpecificTags(base, tagsToRemove);
            return cleaned + ext;
        }
        return removeSpecificTags(leaf, tagsToRemove);
    }

    /**
     * 移除基础名中的指定标签
     *
     * @param name         基础名
     * @param tagsToRemove 要移除的标签集合
     * @return 新的基础名
     */
    private static String removeSpecificTags(String name, Set<String> tagsToRemove) {
        List<String> existingTags = parseAllTags(name);
        List<String> remainingTags = new java.util.ArrayList<>();
        for (String tag : existingTags) {
            if (!tagsToRemove.contains(tag)) {
                remainingTags.add(tag);
            }
        }
        String rest = ALL_TAGS_PATTERN.matcher(name).replaceAll("");
        if (remainingTags.isEmpty()) {
            return rest;
        }
        return buildTagPrefix(remainingTags) + rest;
    }

    /**
     * 移除基础名中的所有标签
     *
     * @param name 基础名
     * @return 新的基础名
     */
    private static String removeLeadingTags(String name) {
        return ALL_TAGS_PATTERN.matcher(name).replaceAll("");
    }

    /**
     * 解析文件名中所有位置的标签（不限于前导位置）。
     * 凡是被【】包裹的内容都视为标签。
     *
     * @param name 文件名
     * @return 标签列表
     */
    public static List<String> parseAllTags(String name) {
        List<String> out = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = ALL_TAGS_PATTERN.matcher(name);
        while (matcher.find()) {
            String tag = matcher.group();
            String inner = tag.substring(1, tag.length() - 1).trim();
            if (!inner.isEmpty()) {
                out.add(inner);
            }
        }
        return out;
    }

    /**
     * 构建标签前缀
     *
     * @param tags 标签列表
     * @return 标签前缀字符串
     */
    private static String buildTagPrefix(List<String> tags) {
        StringBuilder sb = new StringBuilder();
        for (String t : tags) {
            sb.append("【").append(t).append("】");
        }
        return sb.toString();
    }

    /**
     * 生成新版本的文件名
     *
     * @param leaf 文件名
     * @return 新版本的文件名
     */
    private static String generateNewVersionName(String leaf) {
        int dot = leaf.lastIndexOf('.');
        String base, ext;
        if (dot > 0) {
            base = leaf.substring(0, dot);
            ext = leaf.substring(dot);
        } else {
            base = leaf;
            ext = "";
        }

        java.util.regex.Pattern versionPattern = java.util.regex.Pattern.compile("^(?:【[^】]*】)*【V(\\d+)】");
        java.util.regex.Matcher matcher = versionPattern.matcher(base);

        if (matcher.find()) {
            try {
                String versionStr = matcher.group(1);
                int version = Integer.parseInt(versionStr);
                int newVersion = version + 1;
                String newBase = matcher.replaceFirst("【V" + newVersion + "】");
                return newBase + ext;
            } catch (NumberFormatException e) {
                String prefix = matcher.replaceFirst("");
                return prefix + "【V2】" + ext;
            }
        } else {
            return "【V2】" + base + ext;
        }
    }

    /**
     * 忽略大小写检查集合是否包含指定值
     *
     * @param list  集合
     * @param value 值
     * @return 是否包含
     */
    private static boolean containsIgnoreCase(Iterable<String> list, String value) {
        for (String s : list) {
            if (s != null && value != null && s.equalsIgnoreCase(value)) return true;
        }
        return false;
    }
}