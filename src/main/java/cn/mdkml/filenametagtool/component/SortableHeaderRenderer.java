package cn.mdkml.filenametagtool.component;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * 可排序表格列头渲染器，支持悬停高亮和排序箭头指示。
 */
public class SortableHeaderRenderer extends DefaultTableCellRenderer {

    private static final Color HOVER_BG = new Color(230, 230, 230);
    private static final Color NORMAL_BG = new Color(245, 245, 245);
    private static final Color SORT_INDICATOR_COLOR = new Color(100, 100, 100);
    private static final Color BORDER_COLOR = new Color(220, 220, 220);

    private int hoverColumn = -1;
    private int sortColumn = -1;
    private boolean sortAscending = true;

    public SortableHeaderRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
    }

    public void setHoverColumn(int column) {
        this.hoverColumn = column;
    }

    public void setSortState(int column, boolean ascending) {
        this.sortColumn = column;
        this.sortAscending = ascending;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        label.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        if (column == hoverColumn) {
            label.setBackground(HOVER_BG);
        } else {
            label.setBackground(NORMAL_BG);
        }
        label.setOpaque(true);

        // 排序指示器
        if (column == sortColumn && column >= 0) {
            String arrow = sortAscending ? " \u25B2" : " \u25BC";
            label.setText(value.toString() + arrow);
            label.setForeground(SORT_INDICATOR_COLOR);
        } else {
            label.setText(value.toString());
            label.setForeground(Color.BLACK);
        }

        return label;
    }
}
