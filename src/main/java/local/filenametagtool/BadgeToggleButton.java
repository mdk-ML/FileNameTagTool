package local.filenametagtool;

import javax.swing.*;
import java.awt.*;

/**
 * 带右上角徽标的 JToggleButton（开关按钮）
 */
public class BadgeToggleButton extends JToggleButton {
    private int badgeNumber = 0;     // 徽标数字
    private Color badgeBgColor = Color.RED;  // 徽标背景色
    private Color badgeTextColor = Color.WHITE; // 徽标文字颜色

    // 构造方法（支持文字/图标）
    public BadgeToggleButton() {
        super();
    }

    public BadgeToggleButton(String text) {
        super(text);
    }

    public BadgeToggleButton(Icon icon) {
        super(icon);
    }

    // 设置徽标数字（0=隐藏，>99=99+）
    public void setBadgeNumber(int number) {
        this.badgeNumber = Math.max(number, 0);
        if (this.badgeNumber > 0) {
            //避免徽标叠加在文字内容上
            this.setText(getText() + " ");
        }
        repaint(); // 重绘
    }

    // 自定义徽标颜色
    public void setBadgeColor(Color bgColor, Color textColor) {
        this.badgeBgColor = bgColor;
        this.badgeTextColor = textColor;
        repaint();
    }

    // 核心：重写绘制，在按钮右上角画徽标
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 先绘制按钮本身
        if (badgeNumber <= 0) return; // 数字0不显示徽标

        Graphics2D g2d = (Graphics2D) g;
        // 开启抗锯齿（让徽标更圆润）
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. 徽标文字处理
        String text = badgeNumber > 99 ? "99+" : String.valueOf(badgeNumber);
        Font font = new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 10);
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics(font);

        // 2. 徽标尺寸
        int textWidth = metrics.stringWidth(text);
        int badgeWidth = Math.max(textWidth + 6, 16);
        int badgeHeight = 16;

        // 3. 定位：按钮右上角
        int x = getWidth() - badgeWidth - 2;
        int y = 2;

        // 4. 绘制圆形徽标背景
        g2d.setColor(badgeBgColor);
        g2d.fillRoundRect(x, y, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

        // 5. 绘制居中文字
        g2d.setColor(badgeTextColor);
        int textX = x + (badgeWidth - textWidth) / 2;
        int textY = y + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent();
        g2d.drawString(text, textX, textY);
    }

    // ===================== 测试演示 =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("JToggleButton 徽标演示");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(450, 200);
            frame.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 50));

            // 1. 普通数字徽标
            BadgeToggleButton btn1 = new BadgeToggleButton("消息");
            btn1.setBadgeNumber(5);

            // 2. 99+ 徽标
            BadgeToggleButton btn2 = new BadgeToggleButton("通知");
            btn2.setBadgeNumber(120);

            // 3. 隐藏徽标(数字0) + 自定义颜色
            BadgeToggleButton btn3 = new BadgeToggleButton("设置");
            btn3.setBadgeNumber(0);
            btn3.setBadgeColor(Color.BLUE, Color.WHITE);

            // 4. 图标+徽标（适配图标按钮）
            Icon icon = UIManager.getIcon("OptionPane.informationIcon");
            BadgeToggleButton btn4 = new BadgeToggleButton(icon);
            btn4.setBadgeNumber(9);

            // 添加到窗口
            frame.add(btn1);
            frame.add(btn2);
            frame.add(btn3);
            frame.add(btn4);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}