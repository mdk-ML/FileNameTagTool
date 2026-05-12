package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.Config;

import java.awt.Color;
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
        if (tags == null) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String t : tags) {
            if (t == null) {
                continue;
            }
            String v = t.trim();
            if (v.isEmpty()) {
                continue;
            }
            v = v.replace("【", "").replace("】", "").replace("[", "").replace("]", "").trim();
            if (v.isEmpty()) {
                continue;
            }
            out.add(v);
        }
        return new ArrayList<>(out);
    }

    // ==================== 标签颜色管理 ====================

    /**
     * 预设的柔和色调池（HSB格式）
     * 饱和度和亮度固定，色相变化
     */
    private static final float[][] COLOR_PALETTE = {
            {0.0f, 0.45f, 0.92f},
            {0.08f, 0.45f, 0.92f},
            {0.15f, 0.45f, 0.92f},
            {0.25f, 0.45f, 0.92f},
            {0.35f, 0.45f, 0.92f},
            {0.45f, 0.45f, 0.92f},
            {0.55f, 0.45f, 0.92f},
            {0.60f, 0.45f, 0.92f},
            {0.70f, 0.45f, 0.92f},
            {0.80f, 0.45f, 0.92f},
            {0.90f, 0.45f, 0.92f},
    };

    /**
     * 获取标签的背景颜色
     * <p>
     * 优先使用用户自定义颜色，否则根据标签名自动分配
     * </p>
     *
     * @param tagName 标签名
     * @return 标签背景颜色
     */
    public static Color getTagColor(String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            return new Color(220, 220, 220);
        }

        Color customColor = Config.tagColors.get(tagName);
        if (customColor != null) {
            return customColor;
        }

        return autoAssignColor(tagName);
    }

    /**
     * 根据标签名自动分配颜色
     * <p>
     * 使用标签名的哈希值选择色调，微调饱和度和亮度以增加变化
     * </p>
     *
     * @param tagName 标签名
     * @return 自动分配的颜色
     */
    private static Color autoAssignColor(String tagName) {
        int hash = Math.abs(tagName.hashCode());

        int hueIndex = hash % COLOR_PALETTE.length;
        float[] hsb = COLOR_PALETTE[hueIndex];

        float saturation = hsb[1] + ((hash % 20) - 10) / 100f;
        float brightness = hsb[2] + ((hash % 15) - 7) / 100f;

        saturation = Math.max(0.3f, Math.min(0.6f, saturation));
        brightness = Math.max(0.85f, Math.min(0.95f, brightness));

        return Color.getHSBColor(hsb[0], saturation, brightness);
    }

    /**
     * 获取标签的前景文字颜色（确保可读性）
     * <p>
     * 根据背景颜色的亮度计算合适的前景色：
     * - 亮背景使用深色文字
     * - 暗背景使用浅色文字
     * </p>
     *
     * @param bgColor 背景颜色
     * @return 前景文字颜色
     */
    public static Color getTagForegroundColor(Color bgColor) {
        if (bgColor == null) {
            return new Color(51, 51, 51);
        }

        double luminance = (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue()) / 255;

        return luminance > 0.6 ? new Color(51, 51, 51) : Color.WHITE;
    }

    /**
     * 设置标签颜色
     *
     * @param tagName 标签名
     * @param color   颜色值
     */
    public static void setTagColor(String tagName, Color color) {
        if (tagName == null || tagName.isEmpty()) {
            return;
        }

        if (color != null) {
            Config.tagColors.put(tagName, color);
        } else {
            Config.tagColors.remove(tagName);
        }
    }

    /**
     * 清除标签颜色（恢复自动分配）
     *
     * @param tagName 标签名
     */
    public static void clearColor(String tagName) {
        if (tagName != null) {
            Config.tagColors.remove(tagName);
        }
    }

    /**
     * 检查标签是否有自定义颜色
     *
     * @param tagName 标签名
     * @return 是否有自定义颜色
     */
    public static boolean hasCustomColor(String tagName) {
        return tagName != null && Config.tagColors.containsKey(tagName);
    }
}
