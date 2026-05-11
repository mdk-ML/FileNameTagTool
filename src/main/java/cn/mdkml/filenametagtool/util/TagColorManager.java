package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.Config;

import java.awt.Color;

/**
 * 标签颜色管理器
 * <p>
 * 提供标签颜色的自动分配算法，根据标签名哈希值生成柔和的背景色。
 * 支持用户自定义颜色，优先使用自定义颜色，否则自动分配。
 * </p>
 */
public final class TagColorManager {

    /**
     * 私有构造函数，防止实例化
     */
    private TagColorManager() {
    }

    /**
     * 预设的柔和色调池（HSB格式）
     * 饱和度和亮度固定，色相变化
     */
    private static final float[][] COLOR_PALETTE = {
            {0.0f, 0.45f, 0.92f},   // 红色系
            {0.08f, 0.45f, 0.92f},  // 橙色系
            {0.15f, 0.45f, 0.92f},  // 黄橙色系
            {0.25f, 0.45f, 0.92f},  // 黄绿色系
            {0.35f, 0.45f, 0.92f},  // 绿色系
            {0.45f, 0.45f, 0.92f},  // 青绿色系
            {0.55f, 0.45f, 0.92f},  // 青色系
            {0.60f, 0.45f, 0.92f},  // 蓝色系
            {0.70f, 0.45f, 0.92f},  // 蓝紫色系
            {0.80f, 0.45f, 0.92f},  // 紫色系
            {0.90f, 0.45f, 0.92f},  // 紫红色系
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

        // 优先从配置中获取自定义颜色
        Color customColor = Config.tagColors.get(tagName);
        if (customColor != null) {
            return customColor;
        }

        // 自动分配颜色
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

        // 选择色调
        int hueIndex = hash % COLOR_PALETTE.length;
        float[] hsb = COLOR_PALETTE[hueIndex];

        // 微调饱和度和亮度，增加变化
        float saturation = hsb[1] + ((hash % 20) - 10) / 100f;
        float brightness = hsb[2] + ((hash % 15) - 7) / 100f;

        // 限制在合理范围内
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

        // 计算亮度（使用标准公式）
        double luminance = (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue()) / 255;

        // 亮背景用深色文字，暗背景用浅色文字
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
