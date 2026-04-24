package local.filenametagtool.component;

import javax.swing.*;
import java.awt.*;

public class WrapLayout extends FlowLayout {

    public WrapLayout() {
        super(LEFT, 5, 5);
    }

    public WrapLayout(int align) {
        super(align, 5, 5);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return computeSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return computeSize(target, false);
    }

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

            for (Component comp : target.getComponents()) {
                if (!comp.isVisible()) continue;
                Dimension dim = isPreferred ? comp.getPreferredSize() : comp.getMinimumSize();
                maxCompWidth = Math.max(maxCompWidth, dim.width);

                if (currentRowWidth + dim.width > availableWidth && currentRowWidth > 0) {
                    totalHeight += currentRowHeight + vgap;
                    currentRowWidth = 0;
                    currentRowHeight = 0;
                }

                currentRowWidth += dim.width + hgap;
                currentRowHeight = Math.max(currentRowHeight, dim.height);
            }

            totalHeight += currentRowHeight + insets.bottom + vgap;
            // 最小宽度 = 最大组件宽度（只有小于这个值才触发X滚动）
            int finalWidth = Math.max(width, maxCompWidth);
            return new Dimension(finalWidth, totalHeight);
        }
    }
}