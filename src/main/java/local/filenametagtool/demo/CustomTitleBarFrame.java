package local.filenametagtool.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CustomTitleBarFrame extends JFrame {
    // 窗口拖拽坐标
    private int mouseX, mouseY;

    public CustomTitleBarFrame() {
        // 1. 关键：隐藏系统原生标题栏
        setUndecorated(true);
        setSize(500, 300);
        setLocationRelativeTo(null); // 居中
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ===================== 自定义标题栏 =====================
        JPanel titleBar = new JPanel();
        titleBar.setBackground(new Color(30, 144, 255)); // 标题栏背景色
        titleBar.setLayout(new BorderLayout());
        titleBar.setPreferredSize(new Dimension(getWidth(), 40)); // 标题栏高度

        // 2. 核心：用 HTML 设置标题字体样式（加粗、字体、大小、颜色）
        JLabel titleLabel = new JLabel(
                "<html><b style='font-family:微软雅黑; font-size:16px; color:white;'>" +
                        "自定义标题栏（可加粗/改字体/改大小）</b></html>"
        );
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0)); // 左边距

        // 关闭按钮
        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(255, 60, 60));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> System.exit(0));

        // 组装标题栏
        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(closeBtn, BorderLayout.EAST);

        // ===================== 窗口拖拽功能 =====================
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
        titleBar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(getX() + e.getX() - mouseX, getY() + e.getY() - mouseY);
            }
        });

        // ===================== 主界面 =====================
        JPanel mainPanel = new JPanel();
        mainPanel.add(new JLabel("主内容区域"));

        // 总布局
        setLayout(new BorderLayout());
        add(titleBar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        // Swing 组件必须在事件 dispatch 线程运行
        SwingUtilities.invokeLater(() -> new CustomTitleBarFrame().setVisible(true));
    }
}