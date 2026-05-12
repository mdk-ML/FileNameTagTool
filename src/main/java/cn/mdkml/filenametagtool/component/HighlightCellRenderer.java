package cn.mdkml.filenametagtool.component;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 支持搜索关键词高亮的表格单元格渲染器。
 * 匹配的关键词文本会被黄色背景高亮显示。
 */
public class HighlightCellRenderer extends DefaultTableCellRenderer {

    private String[] keywords = new String[0];

    /**
     * 设置需要高亮的搜索关键词
     *
     * @param keywords 关键词数组
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords != null ? keywords : new String[0];
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (value == null || keywords.length == 0) {
            label.setText(value != null ? value.toString() : "");
            return label;
        }

        String text = value.toString();
        String lowerText = text.toLowerCase();

        // 找出所有匹配区间
        List<int[]> highlights = new ArrayList<>();
        for (String kw : keywords) {
            String lowerKw = kw.toLowerCase();
            int from = 0;
            while ((from = lowerText.indexOf(lowerKw, from)) >= 0) {
                highlights.add(new int[]{from, from + kw.length()});
                from += kw.length();
            }
        }

        if (highlights.isEmpty()) {
            label.setText(text);
            return label;
        }

        // 合并重叠区间
        highlights.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] interval : highlights) {
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < interval[0]) {
                merged.add(interval);
            } else {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], interval[1]);
            }
        }

        // 构建 HTML 高亮
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        StringBuilder html = new StringBuilder("<html><nobr>");
        int lastEnd = 0;
        for (int[] interval : merged) {
            int start = interval[0];
            int end = interval[1];
            if (start > lastEnd) {
                html.append(escaped, lastEnd, start);
            }
            html.append("<span style=\"background:#FFEB3B;color:#000;padding:1px 2px\">");
            html.append(escaped, start, end);
            html.append("</span>");
            lastEnd = end;
        }
        if (lastEnd < escaped.length()) {
            html.append(escaped, lastEnd, escaped.length());
        }
        html.append("</nobr></html>");

        label.setText(html.toString());
        return label;
    }
}
