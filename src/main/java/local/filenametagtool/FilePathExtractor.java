//package local.filenametagtool;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.datatransfer.Clipboard;
//import java.awt.datatransfer.DataFlavor;
//import java.awt.datatransfer.Transferable;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.File;
//
//public class FilePathExtractor extends JFrame {
//    private JTextArea textArea;
//    private JButton getSelectedFilesButton;
//    private JLabel statusLabel;
//
//    public FilePathExtractor() {
//        // 设置窗口标题
//        setTitle("文件路径提取工具");
//
//        // 设置窗口基本属性
//        setSize(500, 400);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        // 设置窗口位置
//        setLocationRelativeTo(null);
//
//        // 创建主面板
//        JPanel mainPanel = new JPanel(new BorderLayout());
//
//        // 创建文本区域显示文件路径
//        textArea = new JTextArea();
//        textArea.setEditable(false);
//        JScrollPane scrollPane = new JScrollPane(textArea);
//        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//
//        // 创建按钮面板
//        JPanel buttonPanel = new JPanel(new FlowLayout());
//        getSelectedFilesButton = new JButton("获取选中的文件路径");
//        getSelectedFilesButton.addActionListener(new GetSelectedFilesAction());
//        buttonPanel.add(getSelectedFilesButton);
//
//        // 创建状态标签
//        statusLabel = new JLabel("点击按钮获取选中文件路径");
//        buttonPanel.add(statusLabel);
//
//        // 将组件添加到主面板
//        mainPanel.add(scrollPane, BorderLayout.CENTER);
//        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
//
//        // 添加到窗口
//        add(mainPanel);
//
//        // 设置窗口始终置顶
//        setAlwaysOnTop(true);
//
//        // 显示窗口
//        setVisible(true);
//    }
//
//    private class GetSelectedFilesAction extends Component implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            try {
//                // 获取系统剪贴板
//                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//
//                // 获取剪贴板内容
//                Transferable contents = clipboard.getContents(null);
//
//                if (contents != null && contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
//                    // 获取选中的文件列表
//                    java.util.List<File> fileList = (java.util.List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
//
//                    // 清空文本区域
//                    textArea.setText("");
//
//                    // 输出所有选中文件的路径
//                    StringBuilder sb = new StringBuilder();
//                    for (File file : fileList) {
//                        sb.append(file.getAbsolutePath()).append("\n");
//                    }
//
//                    textArea.setText(sb.toString());
//                    statusLabel.setText("成功获取 " + fileList.size() + " 个文件路径");
//                } else {
//                    // 如果剪贴板中没有文件信息，提示用户先选中文件
//                    JOptionPane.showMessageDialog(this,
//                            "请先在资源管理器中选中文件，然后按 Ctrl+C 复制",
//                            "提示",
//                            JOptionPane.INFORMATION_MESSAGE);
//                    statusLabel.setText("请先在资源管理器中选中文件并复制");
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                statusLabel.setText("获取文件路径失败");
//            }
//        }
//    }
//
//    public static void main(String[] args) {
//        // 设置系统的外观
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // 在事件调度线程中创建GUI
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                new FilePathExtractor();
//            }
//        });
//    }
//}