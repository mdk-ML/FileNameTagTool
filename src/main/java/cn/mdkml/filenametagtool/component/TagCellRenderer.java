package cn.mdkml.filenametagtool.component;

import cn.mdkml.filenametagtool.util.TagColorManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 标签列单元格渲染器
 * <p>
 * 将标签文本渲染为带背景色的小标签样式。
 * 使用 TagColorManager 获取标签的背景颜色。
 * 支持鼠标悬停时在标签右上角显示删除按钮。
 * </p>
 */
public class TagCellRenderer extends JPanel implements TableCellRenderer {

    /** 标签水平内边距 */
    public static final int TAG_PADDING_H = 8;
    /** 标签垂直内边距 */
    public static final int TAG_PADDING_V = 3;
    /** 标签间距 */
    public static final int TAG_GAP = 8;
    /** 标签圆角半径 */
    public static final int TAG_RADIUS = 10;
    /** 标签最大宽度比例（相对于单元格宽度） */
    private static final double MAX_WIDTH_RATIO = 0.95;
    /** 删除按钮大小 */
    private static final int DELETE_SIZE = 14;
    /** 删除按钮点击区域额外扩展的像素（让点击更容易命中） */
    private static final int DELETE_HIT_PADDING = 2;

    /** 当前单元格的标签列表 */
    private final List<String> tags = new ArrayList<>();
    /** 每个标签的绘制位置 */
    private final List<Rectangle> tagBounds = new ArrayList<>();
    /** 搜索标签回调函数 */
    private SearchTagCallback searchCallback;
    /** 删除标签回调函数 */
    private DeleteTagCallback deleteCallback;
    /** 当前正在绘制的行索引 */
    private int currentRow = -1;
    /** 鼠标悬停的行索引（-1 表示无悬停） */
    private int hoveredRow = -1;
    /** 鼠标悬停的标签索引（-1 表示无悬停） */
    private int hoveredTagIndex = -1;
    /** 绘制时使用的字体度量，供点击检测复用 */
    private FontMetrics paintedFontMetrics;

    /**
     * 搜索标签回调接口
     */
    @FunctionalInterface
    public interface SearchTagCallback {
        /**
         * 搜索标签时调用
         *
         * @param tagName 标签名
         */
        void onSearch(String tagName);
    }

    /**
     * 删除标签回调接口
     */
    @FunctionalInterface
    public interface DeleteTagCallback {
        /**
         * 删除标签时调用
         *
         * @param tagName 标签名
         */
        void onDelete(String tagName);
    }

    /**
     * 构造函数
     */
    public TagCellRenderer() {
        setOpaque(true);
    }

    /**
     * 设置搜索标签回调
     *
     * @param callback 回调函数
     */
    public void setSearchCallback(SearchTagCallback callback) {
        this.searchCallback = callback;
    }

    /**
     * 设置删除标签回调
     *
     * @param callback 回调函数
     */
    public void setDeleteCallback(DeleteTagCallback callback) {
        this.deleteCallback = callback;
    }

    /**
     * 设置鼠标悬停的标签位置
     *
     * @param row      行索引
     * @param tagIndex 标签索引（-1 表示无悬停）
     */
    public void setHoveredTag(int row, int tagIndex) {
        this.hoveredRow = row;
        this.hoveredTagIndex = tagIndex;
    }

    /**
     * 获取悬停行索引
     *
     * @return 行索引
     */
    public int getHoveredRow() {
        return hoveredRow;
    }

    /**
     * 获取悬停标签索引
     *
     * @return 标签索引
     */
    public int getHoveredTagIndex() {
        return hoveredTagIndex;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        this.currentRow = row;

        List<String> newTags = new ArrayList<>();
        if (value != null && !value.toString().isEmpty()) {
            String tagStr = value.toString();
            String[] tagArray = tagStr.split(",");
            for (String tag : tagArray) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    newTags.add(trimmed);
                }
            }
        }

        if (!tags.equals(newTags)) {
            tags.clear();
            tags.addAll(newTags);
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintedFontMetrics = g.getFontMetrics();
        List<Rectangle> newBounds = new ArrayList<>();
        paintTagsInternal(g, getWidth(), getHeight(), tags, null, newBounds);
        tagBounds.clear();
        tagBounds.addAll(newBounds);

        if (currentRow == hoveredRow && hoveredTagIndex >= 0 && hoveredTagIndex < tagBounds.size()) {
            drawDeleteButton(g, tagBounds.get(hoveredTagIndex));
        }
    }

    /**
     * 绘制删除按钮
     */
    private void drawDeleteButton(Graphics g, Rectangle tagRect) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int btnX = tagRect.x + tagRect.width - DELETE_SIZE / 2;
        int btnY = tagRect.y + (tagRect.height - DELETE_SIZE) / 2;
        int cx = btnX + DELETE_SIZE / 2;
        int cy = btnY + DELETE_SIZE / 2;

        g2d.setColor(new Color(200, 50, 50));
        g2d.fillOval(btnX, btnY, DELETE_SIZE, DELETE_SIZE);

        g2d.setColor(Color.WHITE);
        int pad = 3;
        g2d.drawLine(cx - pad, cy - pad, cx + pad, cy + pad);
        g2d.drawLine(cx + pad, cy - pad, cx - pad, cy + pad);

        g2d.dispose();
    }

    /**
     * 获取删除按钮的边界矩形（比视觉按钮略大，方便点击）
     */
    private static Rectangle getDeleteButtonRect(Rectangle tagRect) {
        int btnX = tagRect.x + tagRect.width - DELETE_SIZE / 2 - DELETE_HIT_PADDING;
        int btnY = tagRect.y + (tagRect.height - DELETE_SIZE) / 2 - DELETE_HIT_PADDING;
        int hitSize = DELETE_SIZE + DELETE_HIT_PADDING * 2;
        return new Rectangle(btnX, btnY, hitSize, hitSize);
    }

    /**
     * 计算标签的边界矩形列表
     *
     * @param tags       标签列表
     * @param fm         字体度量
     * @param cellWidth  单元格宽度
     * @param cellHeight 单元格高度
     * @return 标签边界矩形列表
     */
    public static List<Rectangle> computeTagBounds(List<String> tags, FontMetrics fm, int cellWidth, int cellHeight) {
        List<Rectangle> bounds = new ArrayList<>();
        if (tags == null || tags.isEmpty() || fm == null) {
            return bounds;
        }

        int x = TAG_GAP;
        int y = (cellHeight - fm.getHeight() - TAG_PADDING_V * 2) / 2;
        int maxWidth = (int) (cellWidth * MAX_WIDTH_RATIO);

        for (String tag : tags) {
            int textWidth = fm.stringWidth(tag);
            int tagWidth = textWidth + TAG_PADDING_H * 2;
            int tagHeight = fm.getHeight() + TAG_PADDING_V * 2;

            if (x + tagWidth > maxWidth) {
                break;
            }

            bounds.add(new Rectangle(x, y, tagWidth, tagHeight));
            x += tagWidth + TAG_GAP;
        }

        return bounds;
    }

    /**
     * 绘制标签并更新 bounds
     */
    private void paintTagsInternal(Graphics g, int width, int height, List<String> tags,
                                   Map<String, Integer> tagCounts, List<Rectangle> bounds) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (tags == null || tags.isEmpty()) {
            g2d.dispose();
            return;
        }

        FontMetrics fm = g2d.getFontMetrics();
        int x = TAG_GAP;
        int y = (height - fm.getHeight() - TAG_PADDING_V * 2) / 2;

        int maxWidth = (int) (width * MAX_WIDTH_RATIO);

        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            Color bgColor = TagColorManager.getTagColor(tag);
            Color fgColor = TagColorManager.getTagForegroundColor(bgColor);

            int textWidth = fm.stringWidth(tag);
            int tagWidth = textWidth + TAG_PADDING_H * 2;
            int tagHeight = fm.getHeight() + TAG_PADDING_V * 2;

            if (x + tagWidth > maxWidth) {
                g2d.setColor(new Color(150, 150, 150));
                g2d.drawString("...", x, y + TAG_PADDING_V + fm.getAscent());
                break;
            }

            g2d.setColor(bgColor);
            g2d.fillRoundRect(x, y, tagWidth, tagHeight, TAG_RADIUS, TAG_RADIUS);

            g2d.setColor(new Color(
                    Math.max(0, bgColor.getRed() - 20),
                    Math.max(0, bgColor.getGreen() - 20),
                    Math.max(0, bgColor.getBlue() - 20), 80));
            g2d.drawRoundRect(x, y, tagWidth - 1, tagHeight - 1, TAG_RADIUS, TAG_RADIUS);

            g2d.setColor(fgColor);
            g2d.drawString(tag, x + TAG_PADDING_H, y + TAG_PADDING_V + fm.getAscent());

            if (bounds != null) {
                bounds.add(new Rectangle(x, y, tagWidth, tagHeight));
            }

            x += tagWidth + TAG_GAP;
        }

        g2d.dispose();
    }

    /**
     * 绘制标签（静态方法，供其他组件复用）
     *
     * @param g           图形上下文
     * @param width       绘制区域宽度
     * @param height      绘制区域高度
     * @param tags        标签列表
     */
    public static void paintTags(Graphics g, int width, int height, List<String> tags) {
        paintTags(g, width, height, tags, null);
    }

    /**
     * 绘制标签（静态方法，供其他组件复用，支持显示数量）
     *
     * @param g           图形上下文
     * @param width       绘制区域宽度
     * @param height      绘制区域高度
     * @param tags        标签列表
     * @param tagCounts   标签数量映射（null 表示不显示数量）
     */
    public static void paintTags(Graphics g, int width, int height, List<String> tags, Map<String, Integer> tagCounts) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (tags == null || tags.isEmpty()) {
            g2d.dispose();
            return;
        }

        FontMetrics fm = g2d.getFontMetrics();
        int x = TAG_GAP;
        int y = (height - fm.getHeight() - TAG_PADDING_V * 2) / 2;

        int maxWidth = (int) (width * MAX_WIDTH_RATIO);

        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            Color bgColor = TagColorManager.getTagColor(tag);
            Color fgColor = TagColorManager.getTagForegroundColor(bgColor);

            int textWidth = fm.stringWidth(tag);
            int tagWidth = textWidth + TAG_PADDING_H * 2;
            int tagHeight = fm.getHeight() + TAG_PADDING_V * 2;

            if (x + tagWidth > maxWidth) {
                g2d.setColor(new Color(150, 150, 150));
                g2d.drawString("...", x, y + TAG_PADDING_V + fm.getAscent());
                break;
            }

            g2d.setColor(bgColor);
            g2d.fillRoundRect(x, y, tagWidth, tagHeight, TAG_RADIUS, TAG_RADIUS);

            g2d.setColor(new Color(
                    Math.max(0, bgColor.getRed() - 20),
                    Math.max(0, bgColor.getGreen() - 20),
                    Math.max(0, bgColor.getBlue() - 20), 80));
            g2d.drawRoundRect(x, y, tagWidth - 1, tagHeight - 1, TAG_RADIUS, TAG_RADIUS);

            g2d.setColor(fgColor);
            g2d.drawString(tag, x + TAG_PADDING_H, y + TAG_PADDING_V + fm.getAscent());

            x += tagWidth + TAG_GAP;
        }

        g2d.dispose();
    }

    /**
     * 绘制带数量的标签（静态方法，供历史标签使用）
     *
     * @param g           图形上下文
     * @param width       绘制区域宽度
     * @param height      绘制区域高度
     * @param tag         标签名
     * @param count       标签数量
     */
    public static void paintTagWithCount(Graphics g, int width, int height, String tag, int count) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics fm = g2d.getFontMetrics();
        int x = TAG_GAP;
        int y = (height - fm.getHeight() - TAG_PADDING_V * 2) / 2;

        Color bgColor = TagColorManager.getTagColor(tag);
        Color fgColor = TagColorManager.getTagForegroundColor(bgColor);

        int textWidth = fm.stringWidth(tag);
        int tagWidth = textWidth + TAG_PADDING_H * 2;
        int tagHeight = fm.getHeight() + TAG_PADDING_V * 2;

        g2d.setColor(bgColor);
        g2d.fillRoundRect(x, y, tagWidth, tagHeight, TAG_RADIUS, TAG_RADIUS);

        g2d.setColor(new Color(
                Math.max(0, bgColor.getRed() - 20),
                Math.max(0, bgColor.getGreen() - 20),
                Math.max(0, bgColor.getBlue() - 20), 80));
        g2d.drawRoundRect(x, y, tagWidth - 1, tagHeight - 1, TAG_RADIUS, TAG_RADIUS);

        g2d.setColor(fgColor);
        g2d.drawString(tag, x + TAG_PADDING_H, y + TAG_PADDING_V + fm.getAscent());

        if (count > 0) {
            String countText = count > 99 ? "99+" : String.valueOf(count);
            Font badgeFont = new Font("Microsoft YaHei UI", Font.PLAIN, 10);
            g2d.setFont(badgeFont);
            FontMetrics badgeFm = g2d.getFontMetrics();

            int badgeTextWidth = badgeFm.stringWidth(countText);
            int badgeWidth = Math.max(badgeTextWidth + 8, 20);
            int badgeHeight = 18;
            int badgeX = x + tagWidth + 4;
            int badgeY = y + (tagHeight - badgeHeight) / 2;

            g2d.setColor(new Color(150, 150, 150));
            g2d.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

            g2d.setColor(Color.WHITE);
            int textX = badgeX + (badgeWidth - badgeTextWidth) / 2;
            int textY = badgeY + (badgeHeight - badgeFm.getHeight()) / 2 + badgeFm.getAscent();
            g2d.drawString(countText, textX, textY);
        }

        g2d.dispose();
    }

    /**
     * 处理标签列点击事件。
     * 优先检测是否点击了删除按钮，再检测是否点击了标签（搜索）。
     *
     * @param row     行索引
     * @param point   点击位置（相对于单元格的坐标）
     * @param table   表格组件
     * @return 是否触发了操作
     */
    public boolean handleClick(int row, Point point, JTable table) {
        if (point == null || table == null) {
            return false;
        }

        String tagStr = table.getValueAt(row, 1).toString();
        if (tagStr == null || tagStr.toString().isEmpty()) {
            return false;
        }

        List<String> tagList = new ArrayList<>();
        for (String t : tagStr.toString().split(",")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                tagList.add(trimmed);
            }
        }
        if (tagList.isEmpty()) {
            return false;
        }

        Rectangle cellRect = table.getCellRect(row, 1, false);
        FontMetrics fm = paintedFontMetrics != null ? paintedFontMetrics : table.getFontMetrics(table.getFont());
        List<Rectangle> bounds = computeTagBounds(tagList, fm, cellRect.width, cellRect.height);

        for (int i = 0; i < bounds.size(); i++) {
            Rectangle deleteRect = getDeleteButtonRect(bounds.get(i));
            if (deleteRect.contains(point)) {
                // 仅在删除按钮可见时（鼠标悬停在该标签上）才执行删除
                if (hoveredRow == row && hoveredTagIndex == i && deleteCallback != null) {
                    deleteCallback.onDelete(tagList.get(i));
                }
                return true;
            }
        }

        for (int i = 0; i < bounds.size(); i++) {
            if (bounds.get(i).contains(point)) {
                if (searchCallback != null) {
                    searchCallback.onSearch(tagList.get(i));
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 获取指定位置的标签索引
     *
     * @param row     行索引
     * @param point   位置（相对于单元格的坐标）
     * @param table   表格组件
     * @return 标签索引，不在标签上则返回 -1
     */
    public int getTagIndexAtPoint(int row, Point point, JTable table) {
        if (point == null || table == null) {
            return -1;
        }

        String tagStr = table.getValueAt(row, 1).toString();
        if (tagStr == null || tagStr.toString().isEmpty()) {
            return -1;
        }

        List<String> tagList = new ArrayList<>();
        for (String t : tagStr.toString().split(",")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                tagList.add(trimmed);
            }
        }
        if (tagList.isEmpty()) {
            return -1;
        }

        Rectangle cellRect = table.getCellRect(row, 1, false);
        FontMetrics fm = paintedFontMetrics != null ? paintedFontMetrics : table.getFontMetrics(table.getFont());
        List<Rectangle> bounds = computeTagBounds(tagList, fm, cellRect.width, cellRect.height);

        for (int i = 0; i < bounds.size(); i++) {
            if (bounds.get(i).contains(point)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取指定位置的标签名
     *
     * @param point 位置
     * @return 标签名，如果位置不在标签上则返回null
     */
    public String getTagAtPoint(Point point) {
        if (point == null || tags.isEmpty()) {
            return null;
        }

        for (int i = 0; i < tagBounds.size(); i++) {
            if (tagBounds.get(i).contains(point)) {
                return tags.get(i);
            }
        }

        return null;
    }
}