package cn.mdkml.filenametagtool.component;

import javax.swing.*;
import java.awt.*;

/**
 * 带右上角徽标的 JToggleButton（开关按钮）
 */
public class BadgeToggleButton extends JToggleButton {
    private int badgeNumber = 0;
    private Color badgeBgColor = Color.RED;
    private Color badgeTextColor = Color.WHITE;

    public BadgeToggleButton() {
        super();
    }

    public BadgeToggleButton(String text) {
        super(text);
    }

    public BadgeToggleButton(Icon icon) {
        super(icon);
    }

    /**
     * 设置徽标数字（0=隐藏，>99=99+）
     *
     * @param number 徽标数字
     */
    public void setBadgeNumber(int number) {
        this.badgeNumber = Math.max(number, 0);
        if (this.badgeNumber > 0) {
            this.setText(getText() + " ");
        }
        repaint();
    }

    /**
     * 自定义徽标颜色
     *
     * @param bgColor    背景色
     * @param textColor  文字颜色
     */
    public void setBadgeColor(Color bgColor, Color textColor) {
        this.badgeBgColor = bgColor;
        this.badgeTextColor = textColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (badgeNumber <= 0) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String text = badgeNumber > 99 ? "99+" : String.valueOf(badgeNumber);
        Font font = new Font("Microsoft YaHei UI", Font.PLAIN, 10);
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics(font);

        int textWidth = metrics.stringWidth(text);
        int badgeWidth = Math.max(textWidth + 6, 16);
        int badgeHeight = 16;

        int x = getWidth() - badgeWidth - 2;
        int y = 2;

        g2d.setColor(badgeBgColor);
        g2d.fillRoundRect(x, y, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

        g2d.setColor(badgeTextColor);
        int textX = x + (badgeWidth - textWidth) / 2;
        int textY = y + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent();
        g2d.drawString(text, textX, textY);
    }
}
