package cn.mdkml.filenametagtool.demo;

import javax.swing.*;

public class HtmlDialogDemo {
    public static void main(String[] args) {
        String htmlContent = "<html>"
                + "<body style='font-size:14px; font-family:微软雅黑;'>"
                + "<h2 style='color:#2E86AB;'>Swing HTML 支持示例</h2>"
                + "<p>这是一段 <b style='color:#A23B72;'>加粗文本</b>，"
                + "这是一段 <i style='color:#F18F01;'>斜体文本</i>。</p>"
                + "<p>支持换行显示，<br>也可以插入图片：</p>"
                + "<img src='https://www.example.com/icon.png' width='32' height='32'>"
                + "</body></html>";

        // 直接在 JOptionPane 中显示 HTML
        JOptionPane.showMessageDialog(
                null,
                htmlContent,
                "HTML 弹窗",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}