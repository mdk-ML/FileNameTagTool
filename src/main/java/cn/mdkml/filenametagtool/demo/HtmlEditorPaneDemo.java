package cn.mdkml.filenametagtool.demo;

import javax.swing.*;
import java.awt.*;

public class HtmlEditorPaneDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("HTML 浏览器");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");

        // 加载本地 HTML 或远程 URL
        try {
            editorPane.setPage("https://www.example.com");
        } catch (Exception e) {
            editorPane.setText("<html><body>无法加载页面</body></html>");
        }

        frame.add(new JScrollPane(editorPane), BorderLayout.CENTER);
        frame.setVisible(true);
    }
}