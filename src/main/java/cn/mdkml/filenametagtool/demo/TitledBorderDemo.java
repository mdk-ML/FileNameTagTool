package cn.mdkml.filenametagtool.demo;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class TitledBorderDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Swing 带标题边框");
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            // 1. 创建一个面板
            JPanel panel = new JPanel();
            panel.add(new JLabel("我是带标题边框的容器"));

            // 🔥 核心：给面板添加 带标题的边框
            TitledBorder border = BorderFactory.createTitledBorder("用户信息区域");
            panel.setBorder(border);

            frame.add(panel);
            frame.setVisible(true);
        });
    }
}