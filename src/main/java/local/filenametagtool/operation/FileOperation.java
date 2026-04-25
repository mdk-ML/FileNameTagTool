package local.filenametagtool.operation;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class FileOperation {

    private static final Pattern LEADING_TAGS_PATTERN = Pattern.compile("^(?:【[^】]*】)+");

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
        List<String> existing = parseLeadingTags(base);
        String rest = removeLeadingTags(base);

        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        for (String t : addTags) {
            if (!containsIgnoreCase(merged, t)) merged.add(t);
        }
        for (String t : existing) {
            if (!containsIgnoreCase(merged, t)) merged.add(t);
        }

        String prefix = buildTagPrefix(new java.util.ArrayList<>(merged));
        return prefix + rest;
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
            String cleaned = removeLeadingTags(base);
            return cleaned + ext;
        }
        return removeLeadingTags(leaf);
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
            String cleaned = removeSpecificLeadingTags(base, tagsToRemove);
            return cleaned + ext;
        }
        return removeSpecificLeadingTags(leaf, tagsToRemove);
    }

    /**
     * 移除基础名中的指定标签
     *
     * @param name         基础名
     * @param tagsToRemove 要移除的标签集合
     * @return 新的基础名
     */
    private static String removeSpecificLeadingTags(String name, Set<String> tagsToRemove) {
        List<String> existingTags = parseLeadingTags(name);
        List<String> remainingTags = new java.util.ArrayList<>();
        for (String tag : existingTags) {
            if (!tagsToRemove.contains(tag)) {
                remainingTags.add(tag);
            }
        }
        if (remainingTags.isEmpty()) {
            return LEADING_TAGS_PATTERN.matcher(name).replaceFirst("");
        }
        String prefix = buildTagPrefix(remainingTags);
        String rest = LEADING_TAGS_PATTERN.matcher(name).replaceFirst("");
        return prefix + rest;
    }

    /**
     * 移除基础名中的所有标签
     *
     * @param name 基础名
     * @return 新的基础名
     */
    private static String removeLeadingTags(String name) {
        return LEADING_TAGS_PATTERN.matcher(name).replaceFirst("");
    }

    /**
     * 解析基础名开头的标签
     *
     * @param name 基础名
     * @return 标签列表
     */
    private static List<String> parseLeadingTags(String name) {
        List<String> out = new java.util.ArrayList<>();
        int i = 0;
        while (i < name.length() && name.charAt(i) == '【') {
            int end = name.indexOf('】', i + 1);
            if (end < 0) break;
            String inner = name.substring(i + 1, end).trim();
            if (!inner.isEmpty()) out.add(inner);
            i = end + 1;
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