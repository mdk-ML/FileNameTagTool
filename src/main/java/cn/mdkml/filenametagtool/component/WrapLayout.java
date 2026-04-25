package cn.mdkml.filenametagtool.component;

import javax.swing.*;
import java.awt.*;

/**
 * 自动换行布局管理器
 * <p>
 * 扩展自 FlowLayout，增加了以下功能：
 * 1. 智能适配 JScrollPane 视口宽度
 * 2. 更准确的尺寸计算
 * 3. 组件自动换行排列
 * <p>
 * 适用于需要动态排列多个组件且希望自动换行的场景，如标签云、按钮组等
 */
public class WrapLayout extends FlowLayout {

    /**
     * 默认构造方法
     * <p>
     * 默认左对齐，水平和垂直间距均为5像素
     */
    public WrapLayout() {
        super(LEFT, 5, 5);
    }

    /**
     * 构造方法
     * <p>
     * @param align 对齐方式（FlowLayout.LEFT, FlowLayout.CENTER, FlowLayout.RIGHT）
     */
    public WrapLayout(int align) {
        super(align, 5, 5);
    }

    /**
     * 构造方法
     * <p>
     * @param align 对齐方式
     * @param hgap 水平间距
     * @param vgap 垂直间距
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * 计算首选布局尺寸
     * <p>
     * @param target 目标容器
     * @return 首选尺寸
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return computeSize(target, true);
    }

    /**
     * 计算最小布局尺寸
     * <p>
     * @param target 目标容器
     * @return 最小尺寸
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        return computeSize(target, false);
    }

    /**
     * 核心方法：计算布局尺寸
     * <p>
     * @param target 目标容器
     * @param isPreferred 是否计算首选尺寸
     * @return 计算后的尺寸
     */
    private Dimension computeSize(Container target, boolean isPreferred) {
        synchronized (target.getTreeLock()) {
            int hgap = getHgap();
            int vgap = getVgap();
            int width = target.getWidth();

            // 核心：获取JScrollPane视口宽度，强制面板适配窗口
            Container parent = target.getParent();
            if (parent != null && parent.getParent() instanceof JScrollPane scroll) {
                width = scroll.getViewport().getWidth();
            }

            Insets insets = target.getInsets();
            int availableWidth = width - insets.left - insets.right - hgap;
            availableWidth = Math.max(availableWidth, 0);

            int totalHeight = insets.top + vgap;
            int currentRowWidth = 0;
            int currentRowHeight = 0;
            int maxCompWidth = 0;

            // 遍历所有组件，计算布局尺寸
            for (Component comp : target.getComponents()) {
                if (!comp.isVisible()) continue; // 跳过不可见组件
                Dimension dim = isPreferred ? comp.getPreferredSize() : comp.getMinimumSize();
                maxCompWidth = Math.max(maxCompWidth, dim.width); // 记录最大组件宽度

                // 检查是否需要换行
                if (currentRowWidth + dim.width > availableWidth && currentRowWidth > 0) {
                    totalHeight += currentRowHeight + vgap; // 增加行高
                    currentRowWidth = 0; // 重置行宽
                    currentRowHeight = 0; // 重置行高
                }

                currentRowWidth += dim.width + hgap; // 累加行宽
                currentRowHeight = Math.max(currentRowHeight, dim.height); // 更新行高
            }

            totalHeight += currentRowHeight + insets.bottom + vgap;
            // 最小宽度 = 最大组件宽度（只有小于这个值才触发X滚动）
            int finalWidth = Math.max(width, maxCompWidth);
            return new Dimension(finalWidth, totalHeight);
        }
    }
}