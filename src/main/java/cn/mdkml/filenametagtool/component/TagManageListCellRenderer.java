package cn.mdkml.filenametagtool.component;

import cn.mdkml.filenametagtool.util.FileUtil;
import cn.mdkml.filenametagtool.util.SwingUtil;
import cn.mdkml.filenametagtool.util.TagUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 标签管理列表单元格渲染器，集成排序和颜色设置功能。
 * <p>
 * 普通标签：显示颜色预览块 + 标签名 + 颜色来源
 * 特殊标签：显示说明文字
 * </p>
 */
public class TagManageListCellRenderer extends DefaultListCellRenderer {
    private static final Color SPECIAL_BG = new Color(255, 248, 225);
    private static final Color SPECIAL_FG = new Color(180, 130, 0);

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        String text = value != null ? value.toString() : "";
        boolean isSpecial = FileUtil.TAG_ORDER_FILENAME.equals(text)
                || FileUtil.TAG_ORDER_VERSION.equals(text)
                || FileUtil.TAG_ORDER_DATE.equals(text);

        if (isSpecial) {
            // 特殊标签使用简单样式
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, SwingUtil.BORDER_GRAY),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));

            if (isSelected) {
                // 选中状态保持系统默认
            } else {
                label.setBackground(SPECIAL_BG);
                label.setForeground(SPECIAL_FG);
            }

            if (FileUtil.TAG_ORDER_FILENAME.equals(text)) {
                label.setText("  {文件名}  —  源文件名在标签序列中的位置");
            } else if (FileUtil.TAG_ORDER_VERSION.equals(text)) {
                label.setText("  {版本号}  —  版本号（如 V1、V2）在标签序列中的位置");
            } else if (FileUtil.TAG_ORDER_DATE.equals(text)) {
                label.setText("  {当前日期}  —  日期标签（如 20260506）在标签序列中的位置");
            }
            return label;
        }

        // 普通标签使用带颜色预览的样式
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, SwingUtil.BORDER_GRAY),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        Color bgColor = TagUtil.getTagColor(text);

        // 颜色预览块
        JPanel colorPreview = new JPanel();
        colorPreview.setBackground(bgColor);
        colorPreview.setPreferredSize(new Dimension(24, 24));
        colorPreview.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // 标签名
        JLabel tagLabel = new JLabel(text);
        tagLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));

        // 颜色来源提示
        boolean isCustom = TagUtil.hasCustomColor(text);
        JLabel sourceLabel = new JLabel(isCustom ? "自定义" : "自动");
        sourceLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        sourceLabel.setForeground(Color.GRAY);

        panel.add(colorPreview, BorderLayout.WEST);
        panel.add(tagLabel, BorderLayout.CENTER);
        panel.add(sourceLabel, BorderLayout.EAST);

        if (isSelected) {
            panel.setBackground(list.getSelectionBackground());
        } else {
            panel.setBackground(SwingUtil.BG_WHITE);
        }

        return panel;
    }
}
