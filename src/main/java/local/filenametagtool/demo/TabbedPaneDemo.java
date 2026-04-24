package local.filenametagtool.demo;

import javax.swing.*;
import java.awt.*;

public class TabbedPaneDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Swing 标签页演示");
            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            // ===================== 1. 创建标签页组件 =====================
            JTabbedPane tabbedPane = new JTabbedPane();

            // ===================== 2. 设置标签位置（可选） =====================
            // JTabbedPane.TOP(默认) / BOTTOM / LEFT / RIGHT
            tabbedPane.setTabPlacement(JTabbedPane.TOP);

            // ===================== 3. 添加第一个标签页（普通文本） =====================
            JPanel panel1 = new JPanel();
            panel1.add(new JLabel("这是【首页】标签页内容"));
            panel1.setBackground(Color.WHITE);
            // 参数：标签标题、图标、面板
            tabbedPane.addTab("首页", null, panel1);

            // ===================== 4. 添加第二个标签页（HTML标题，呼应你之前的需求） =====================
            JPanel panel2 = new JPanel();
            // 用HTML设置标签标题样式（加粗、颜色）
            String htmlTitle = "<html><b style='color:blue;'>消息中心</b></html>";
            panel2.add(new JLabel("这是【消息】标签页，带HTML样式标题"));
            tabbedPane.addTab(htmlTitle, null, panel2);

            // ===================== 5. 添加第三个标签页（带图标） =====================
            JPanel panel3 = new JPanel();
            // 使用系统图标（可替换为你的本地图片）
            Icon icon = UIManager.getIcon("OptionPane.informationIcon");
            panel3.add(new JLabel("这是【设置】标签页，带图标"));
            tabbedPane.addTab("设置", icon, panel3);

            // ===================== 6. 标签切换事件监听 =====================
            tabbedPane.addChangeListener(e -> {
                // 获取当前选中的标签索引
                int selectedIndex = tabbedPane.getSelectedIndex();
                System.out.println("切换到标签：" + selectedIndex);
            });

            // ===================== 7. 自定义标签页样式（可选） =====================
            tabbedPane.setBackground(Color.LIGHT_GRAY); // 未选中标签背景
            tabbedPane.setForeground(Color.BLACK);     // 文字颜色

            // 将标签页加入窗口
            frame.add(tabbedPane);
            frame.setVisible(true);
        });
    }
}