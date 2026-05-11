package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class FileUtil {

    /** 版本号标签正则：V数字（不含括号的内部内容） */
    public static final Pattern VERSION_TAG_PATTERN = Pattern.compile("^V\\d+$");
    /** 日期标签正则：8位数字（如 20260506） */
    public static final Pattern DATE_TAG_PATTERN = Pattern.compile("^\\d{8}$");
    /** 匹配所有标签（不限于前导位置），根据当前配置动态生成 */
    private static final Supplier<Pattern> ALL_TAGS_PATTERN_SUPPLIER = () -> {
        String left = Pattern.quote(Config.getTagWrapLeft());
        String right = Pattern.quote(Config.getTagWrapRight());
        return Pattern.compile(left + "[^" + right + "]*" + right);
    };

    /**
     * 获取当前配置下的标签匹配正则表达式
     *
     * @return 标签匹配 Pattern
     */
    public static Pattern getAllTagsPattern() {
        return ALL_TAGS_PATTERN_SUPPLIER.get();
    }

    /**
     * 生成指定包裹符号的标签匹配正则表达式
     *
     * @param wrapL 左包裹符号
     * @param wrapR 右包裹符号
     * @return 标签匹配 Pattern
     */
    private static Pattern getTagPattern(String wrapL, String wrapR) {
        return Pattern.compile(Pattern.quote(wrapL) + "[^" + Pattern.quote(wrapR) + "]*" + Pattern.quote(wrapR));
    }
    /** 标签排序中的特殊占位：源文件名位置 */
    public static final String TAG_ORDER_FILENAME = "{文件名}";
    /** 标签排序中的特殊占位：版本号位置 */
    public static final String TAG_ORDER_VERSION = "{版本号}";
    /** 标签排序中的特殊占位：当前日期标签位置 */
    public static final String TAG_ORDER_DATE = "{当前日期}";

    /**
     * 按照 Config.tags 的顺序重构文件名，将新标签合并到已有标签中。
     * <p>
     * 处理流程：
     * 1. 解析文件名中已有的所有标签
     * 2. 合并已有标签和新标签（去重）
     * 3. 按照 Config.tags 的全局顺序重新构建文件名
     * </p>
     *
     * @param path    文件路径
     * @param addTags 要添加的标签列表
     * @return 是否成功重命名
     * @throws IOException 如果文件操作失败
     */
    public static boolean addTags(Path path, List<String> addTags) throws IOException {
        if (addTags == null || addTags.isEmpty()) {
            return false;
        }

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

        // 解析已有标签，提取纯文件名
        List<String> existing = parseAllTags(base);
        String rest = getAllTagsPattern().matcher(base).replaceAll("");

        // 合并已有标签和新标签
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

        // 按 Config.tags 顺序重构文件名
        String newLeaf = buildOrderedName(rest, new ArrayList<>(merged)) + ext;
        if (newLeaf.equals(leaf)) {
            return false;
        }

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);
        Files.move(path, target);
        return true;
    }

    /**
     * 将文件名中的旧标签替换为新标签。
     * 如果文件名中不包含旧标签，则不进行任何操作。
     *
     * @param path   文件路径
     * @param oldTag 旧标签名
     * @param newTag 新标签名
     * @return 是否成功替换标签
     * @throws IOException 如果文件操作失败
     */
    public static boolean replaceTag(Path path, String oldTag, String newTag) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) {
            return false;
        }

        String leaf = fileName.toString();
        int dot = leaf.lastIndexOf('.');
        String base = dot > 0 ? leaf.substring(0, dot) : leaf;
        String ext = dot > 0 ? leaf.substring(dot) : "";

        List<String> tags = parseAllTags(base);
        if (!tags.contains(oldTag)) {
            return false;
        }

        List<String> newTags = new ArrayList<>();
        for (String tag : tags) {
            if (tag.equals(oldTag)) {
                newTags.add(newTag);
            } else {
                newTags.add(tag);
            }
        }

        String rest = getAllTagsPattern().matcher(base).replaceAll("");
        String newLeaf = buildOrderedName(rest, newTags) + ext;
        if (newLeaf.equals(leaf)) {
            return false;
        }

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
        int dot = leaf.lastIndexOf('.');
        String base = dot > 0 ? leaf.substring(0, dot) : leaf;
        String ext = dot > 0 ? leaf.substring(dot) : "";

        List<String> existing = parseAllTags(base);
        List<String> remaining = new ArrayList<>();
        for (String tag : existing) {
            if (!tagsToRemove.contains(tag)) {
                remaining.add(tag);
            }
        }
        String rest = getAllTagsPattern().matcher(base).replaceAll("");
        String newLeaf = buildOrderedName(rest, remaining) + ext;
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
        int dot = leaf.lastIndexOf('.');
        String base = dot > 0 ? leaf.substring(0, dot) : leaf;
        String ext = dot > 0 ? leaf.substring(dot) : "";

        List<String> tags = parseAllTags(base);
        String rest = getAllTagsPattern().matcher(base).replaceAll("");

        // 查找并递增版本号
        boolean found = false;
        for (int i = 0; i < tags.size(); i++) {
            if (VERSION_TAG_PATTERN.matcher(tags.get(i)).matches()) {
                try {
                    int ver = Integer.parseInt(tags.get(i).substring(1)) + 1;
                    tags.set(i, "V" + ver);
                } catch (NumberFormatException e) {
                    tags.set(i, "V2");
                }
                found = true;
                break;
            }
        }
        if (!found) {
            tags.add(0, "V2");
        }

        String newLeaf = buildOrderedName(rest, tags) + ext;
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
     * 将当前标签设置（顺序和包裹符号）应用到文件。
     * 重排标签顺序并统一包裹符号为配置的样式。
     *
     * @param path 文件路径
     * @return 是否成功修改
     * @throws IOException 如果文件操作失败
     */
    public static boolean applyTagSettings(Path path) throws IOException {
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

        // 当前配置的包裹符号
        String curL = Config.getTagWrapLeft();
        String curR = Config.getTagWrapRight();
        // 备用包裹符号（另一种样式）
        String altL = Config.STYLE_BRACKET.equals(Config.tagBracketStyle) ? "【" : "[";
        String altR = Config.STYLE_BRACKET.equals(Config.tagBracketStyle) ? "】" : "]";

        // 先用当前样式解析，若无结果则用备用样式解析
        List<String> existing = parseAllTags(base, curL, curR);
        boolean styleChanged = false;
        if (existing.isEmpty() && !altL.equals(curL)) {
            existing = parseAllTags(base, altL, altR);
            styleChanged = !existing.isEmpty();
        }
        if (existing.isEmpty()) {
            return false;
        }

        // 移除所有旧标签（两种样式均移除），保留纯文件名
        String rest = getTagPattern(curL, curR).matcher(base).replaceAll("");
        if (!altL.equals(curL)) {
            rest = getTagPattern(altL, altR).matcher(rest).replaceAll("");
        }

        String newLeaf = buildOrderedName(rest, existing) + ext;
        if (newLeaf.equals(leaf) && !styleChanged) {
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
     * 按照 Config.tags 的全局顺序重构完整文件名。
     * 单次遍历 configOrder，依次放置普通标签、源文件名、版本号标签，
     * 最后追加不在配置中的标签。
     *
     * @param baseName 源文件名（不含标签和扩展名）
     * @param tags     所有标签列表
     * @return 重构后的完整文件名（不含扩展名）
     */
    private static String buildOrderedName(String baseName, List<String> tags) {
        List<String> configOrder = Config.tags;
        String wrapL = Config.getTagWrapLeft();
        String wrapR = Config.getTagWrapRight();
        if (configOrder.isEmpty()) {
            StringBuilder fallback = new StringBuilder();
            for (String tag : tags) {
                fallback.append(wrapL).append(tag).append(wrapR);
            }
            fallback.append(baseName);
            return fallback.toString();
        }

        // 分类标签
        List<String> normalTags = new ArrayList<>();
        List<String> versionTags = new ArrayList<>();
        List<String> dateTags = new ArrayList<>();
        for (String tag : tags) {
            if (VERSION_TAG_PATTERN.matcher(tag).matches()) {
                versionTags.add(tag);
            } else if (DATE_TAG_PATTERN.matcher(tag).matches()) {
                dateTags.add(tag);
            } else {
                normalTags.add(tag);
            }
        }

        // 标记已使用的标签
        boolean[] normalUsed = new boolean[normalTags.size()];
        boolean baseNamePlaced = false;

        StringBuilder result = new StringBuilder();

        // 按 configOrder 顺序依次放置
        for (String orderItem : configOrder) {
            if (TAG_ORDER_FILENAME.equals(orderItem)) {
                result.append(baseName);
                baseNamePlaced = true;
            } else if (TAG_ORDER_VERSION.equals(orderItem)) {
                for (String tag : versionTags) {
                    result.append(wrapL).append(tag).append(wrapR);
                }
                versionTags.clear();
            } else if (TAG_ORDER_DATE.equals(orderItem)) {
                for (String tag : dateTags) {
                    result.append(wrapL).append(tag).append(wrapR);
                }
                dateTags.clear();
            } else {
                // 普通标签：查找与 configOrder 匹配的标签
                for (int i = 0; i < normalTags.size(); i++) {
                    if (!normalUsed[i] && normalTags.get(i).equalsIgnoreCase(orderItem)) {
                        result.append(wrapL).append(normalTags.get(i)).append(wrapR);
                        normalUsed[i] = true;
                        break;
                    }
                }
            }
        }

        // 追加不在 configOrder 中的剩余标签
        for (int i = 0; i < normalTags.size(); i++) {
            if (!normalUsed[i]) {
                result.append(wrapL).append(normalTags.get(i)).append(wrapR);
            }
        }
        for (String tag : versionTags) {
            result.append(wrapL).append(tag).append(wrapR);
        }
        for (String tag : dateTags) {
            result.append(wrapL).append(tag).append(wrapR);
        }

        // 文件名未在配置中指定位置，追加到末尾
        if (!baseNamePlaced) {
            result.append(baseName);
        }

        return result.toString();
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
            return getAllTagsPattern().matcher(base).replaceAll("") + ext;
        }
        return getAllTagsPattern().matcher(leaf).replaceAll("");
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
        java.util.regex.Matcher matcher = getAllTagsPattern().matcher(name);
        while (matcher.find()) {
            String tag = matcher.group();
            String left = Config.getTagWrapLeft();
            String right = Config.getTagWrapRight();
            String inner = tag.substring(left.length(), tag.length() - right.length()).trim();
            if (!inner.isEmpty()) {
                out.add(inner);
            }
        }
        return out;
    }

    /**
     * 使用指定包裹符号解析文件名中的所有标签
     *
     * @param name   文件名
     * @param wrapL  左包裹符号
     * @param wrapR  右包裹符号
     * @return 标签列表
     */
    private static List<String> parseAllTags(String name, String wrapL, String wrapR) {
        List<String> out = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = getTagPattern(wrapL, wrapR).matcher(name);
        while (matcher.find()) {
            String tag = matcher.group();
            String inner = tag.substring(wrapL.length(), tag.length() - wrapR.length()).trim();
            if (!inner.isEmpty()) {
                out.add(inner);
            }
        }
        return out;
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