package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.Config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TagUtil {

    /**
     * 记住新标签到配置中。
     * 已存在的标签保持原位不变，新标签插入到 {文件名} 占位符前面。
     *
     * @param tags      标签列表
     * @param smartTags 智能标签集合
     */
    public static void rememberTags(List<String> tags, Set<String> smartTags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        List<String> incoming = normalizeTags(tags);
        if (incoming.isEmpty()) {
            return;
        }

        // 过滤掉智能标签
        List<String> filteredIncoming = new ArrayList<>();
        for (String tag : incoming) {
            if (smartTags == null || !smartTags.contains(tag)) {
                filteredIncoming.add(tag);
            }
        }
        incoming = filteredIncoming;
        if (incoming.isEmpty()) {
            return;
        }

        ConfigUtil.reload();
        List<String> old = new ArrayList<>(Config.tags);

        // 筛选出尚未记录的新标签，排除版本号标签
        List<String> newTags = new ArrayList<>();
        for (String tag : incoming) {
            if (FileUtil.VERSION_TAG_PATTERN.matcher(tag).matches()
                    || FileUtil.DATE_TAG_PATTERN.matcher(tag).matches()) {
                continue;
            }
            boolean exists = false;
            for (String existing : old) {
                if (tag.equalsIgnoreCase(existing)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                newTags.add(tag);
            }
        }

        if (newTags.isEmpty()) {
            return;
        }

        // 找到 {文件名} 的位置，将新标签插入到它后面
        int insertIndex = old.indexOf(FileUtil.TAG_ORDER_FILENAME);
        if (insertIndex < 0) {
            // 没有 {文件名} 占位符，追加到末尾
            insertIndex = old.size();
        } else {
            insertIndex += 1;
        }
        old.addAll(insertIndex, newTags);

        Config.tags = old;
        ConfigUtil.save();
    }

    /**
     * 标准化标签列表
     *
     * @param tags 标签列表
     * @return 标准化后的标签列表
     */
    public static List<String> normalizeTags(List<String> tags) {
        if (tags == null) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String t : tags) {
            if (t == null) continue;
            String v = t.trim();
            if (v.isEmpty()) continue;
            v = v.replace("【", "").replace("】", "").trim();
            if (v.isEmpty()) continue;
            out.add(v);
        }
        return new ArrayList<>(out);
    }
}