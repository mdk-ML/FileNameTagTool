package local.filenametagtool.manager;

import local.filenametagtool.model.Config;
import local.filenametagtool.util.ConfigUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TagManager {

    /**
     * 记住标签到配置中
     *
     * @param tags      标签列表
     * @param smartTags 智能标签集合
     */
    public static void rememberTags(List<String> tags, Set<String> smartTags) {
        if (tags == null || tags.isEmpty()) return;

        List<String> incoming = normalizeTags(tags);
        if (incoming.isEmpty()) return;

        List<String> filteredIncoming = new ArrayList<>();
        for (String tag : incoming) {
            if (smartTags == null || !smartTags.contains(tag)) {
                filteredIncoming.add(tag);
            }
        }
        incoming = filteredIncoming;
        if (incoming.isEmpty()) return;

        Config cfg = ConfigUtil.reload();
        List<String> old = new ArrayList<>(cfg.getTags());
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String t : old) {
            if (t == null) continue;
            String tt = t.trim();
            if (tt.isEmpty()) continue;
            boolean isDup = false;
            for (String v : incoming) {
                if (tt.equalsIgnoreCase(v)) {
                    isDup = true;
                    break;
                }
            }
            if (!isDup) merged.add(tt);
        }
        merged.addAll(incoming);

        cfg.setTags(new ArrayList<>(merged));
        ConfigUtil.saveFromConfig(cfg, "FileNameTagTool config (auto-generated)");
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