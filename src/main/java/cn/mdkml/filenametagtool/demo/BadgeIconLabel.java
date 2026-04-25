package cn.mdkml.filenametagtool.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 自带右上角徽标的图标标签（Swing 徽标组件）
 */
public class BadgeIconLabel extends JLabel {
    private int badgeNumber = 0; // 徽标数字
    private Color badgeBgColor = Color.RED; // 徽标背景色
    private Color badgeTextColor = Color.WHITE; // 徽标文字颜色

    public BadgeIconLabel() {
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    // 设置徽标数字（0=隐藏，>99=99+）
    public void setBadgeNumber(int number) {
        this.badgeNumber = Math.max(number, 0);
        repaint(); // 重绘徽标
    }

    // 自定义徽标颜色
    public void setBadgeColor(Color bgColor, Color textColor) {
        this.badgeBgColor = bgColor;
        this.badgeTextColor = textColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 无数字 / 数字为0，不绘制徽标
        if (badgeNumber <= 0) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. 计算徽标文字
        String text = badgeNumber > 99 ? "99+" : String.valueOf(badgeNumber);
        Font font = new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 10);

        g2d.setFont(font);

        // 2. 计算徽标尺寸（自适应文字宽度）
        FontMetrics metrics = g2d.getFontMetrics(font);
        int textWidth = metrics.stringWidth(text);
        int badgeWidth = Math.max(textWidth + 6, 16);
        int badgeHeight = 16;

        // 3. 定位：右上角
        int x = getWidth() - badgeWidth - 2;
        int y = 2;

        // 4. 绘制圆形/圆角矩形徽标背景
        g2d.setColor(badgeBgColor);
        g2d.fillRoundRect(x, y, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

        // 5. 绘制文字（居中）
        g2d.setColor(badgeTextColor);
        int textX = x + (badgeWidth - textWidth) / 2;
        int textY = y + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent();
        g2d.drawString(text, textX, textY);
    }

    // ===================== 测试主方法 =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Swing 徽标组件演示");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 50));

            // ========== 创建带徽标的图标 ==========
            BadgeIconLabel label1 = new BadgeIconLabel();
            // 设置图标（这里用一个占位图标，你可以替换成本地图片/Icon）
            label1.setIcon(createPlaceholderIcon(64, 64, new Color(100, 149, 237)));
            label1.setBadgeNumber(5); // 设置徽标数字

            BadgeIconLabel label2 = new BadgeIconLabel();
            label2.setIcon(createPlaceholderIcon(64, 64, new Color(255, 165, 0)));
            label2.setBadgeNumber(100); // 超过99自动显示99+

            BadgeIconLabel label3 = new BadgeIconLabel();
            label3.setIcon(createPlaceholderIcon(64, 64, new Color(128, 0, 128)));
            label3.setBadgeNumber(0); // 0=隐藏徽标
            // 自定义徽标颜色
            label3.setBadgeColor(Color.BLUE, Color.WHITE);

            // 添加到窗口
            frame.add(label1);
            frame.add(label2);
            frame.add(label3);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // 生成占位图标（测试用，正式使用替换为你的图片）
    private static Icon createPlaceholderIcon(int w, int h, Color color) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, w, h);
        g2d.dispose();
        return new ImageIcon(image);
    }
}