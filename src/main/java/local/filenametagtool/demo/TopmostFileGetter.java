package local.filenametagtool.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TopmostFileGetter {

    public static void main(String[] args) {
        // 使用 SwingUtilities 确保线程安全
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        // 1. 创建置顶窗口
        JFrame frame = new JFrame("置顶文件获取器");
        frame.setAlwaysOnTop(true); // 关键代码：设置窗口永久置顶
        frame.setSize(300, 150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // 2. 创建核心按钮
        JButton getFileButton = new JButton("获取选中文件路径");
        
        // 3. 给按钮添加点击事件
        getFileButton.addActionListener(e -> fetchFilesFromClipboard());

        // 4. 添加组件并显示窗口
        frame.add(new JLabel("请先在资源管理器中选中文件并按 Ctrl+C"));
        frame.add(getFileButton);
        frame.setVisible(true);
    }

    /**
     * 从系统剪切板中获取文件路径并打印
     */
    private static void fetchFilesFromClipboard() {
        // 获取系统剪切板
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 获取剪切板中的内容
        Transferable contents = clipboard.getContents(null);

        if (contents != null) {
            // 检查剪切板中是否包含文件列表数据
            if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                try {
                    // 将数据转换为文件列表
                    @SuppressWarnings("unchecked")
                    List<File> fileList = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
                    
                    // 清空控制台并输出结果
                    System.out.println("成功获取到 " + fileList.size() + " 个文件:");
                    
                    // 遍历并输出每个文件的绝对路径
                    for (File file : fileList) {
                        System.out.println(file.getAbsolutePath());
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    JOptionPane.showMessageDialog(null, "读取剪切板数据失败!", "错误", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(null, "剪切板中不包含文件!\n请先在资源管理器中选中文件并按 Ctrl+C", "提示", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "剪切板为空!", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }
}