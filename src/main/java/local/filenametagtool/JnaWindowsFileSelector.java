//package local.filenametagtool;
//
//import com.sun.jna.platform.win32.COM.util.IDispatch;
//import com.sun.jna.platform.win32.COM.util.ObjectFactory;
//import com.sun.jna.platform.win32.Guid;
//import com.sun.jna.platform.win32.Ole32;
//
//import javax.swing.*;
//import java.awt.*;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Windows 资源管理器选中文件获取工具（JNA 纯 API 版）
// * 支持同时获取多个资源管理器窗口的选中文件/文件夹
// */
//public class JnaWindowsFileSelector extends JFrame {
//
//    public JnaWindowsFileSelector() {
//        // 窗口基础配置（置顶）
//        setTitle("JNA Windows 文件选择器");
//        setSize(500, 300);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setAlwaysOnTop(true); // 核心：窗口置顶
//        setLocationRelativeTo(null); // 屏幕居中
//
//        // UI 组件
//        JButton getFilesBtn = new JButton("获取当前选中的文件/文件夹");
//        JTextArea resultArea = new JTextArea();
//        resultArea.setEditable(false);
//        resultArea.setLineWrap(true);
//        resultArea.setWrapStyleWord(true);
//        JScrollPane scrollPane = new JScrollPane(resultArea);
//
//        // 按钮点击事件（使用 SwingWorker 避免 UI 卡顿）
//        getFilesBtn.addActionListener(e -> {
//            getFilesBtn.setEnabled(false);
//            resultArea.setText("正在获取资源管理器选中项...");
//
//            new SwingWorker<List<String>, Void>() {
//                @Override
//                protected List<String> doInBackground() {
//                    // 耗时操作放在后台线程
//                    return WindowsShellHelper.getSelectedFilesFromAllExplorers();
//                }
//
//                @Override
//                protected void done() {
//                    try {
//                        List<String> selectedFiles = get();
//                        if (selectedFiles.isEmpty()) {
//                            resultArea.setText("⚠️ 未在任何资源管理器窗口中选中文件/文件夹");
//                        } else {
//                            resultArea.setText("✅ 共选中 " + selectedFiles.size() + " 个项：\n\n"
//                                    + String.join("\n", selectedFiles));
//                        }
//                    } catch (Exception ex) {
//                        resultArea.setText("❌ 获取失败：" + ex.getMessage());
//                        ex.printStackTrace();
//                    } finally {
//                        getFilesBtn.setEnabled(true);
//                    }
//                }
//            }.execute();
//        });
//
//        // 布局
//        setLayout(new BorderLayout(10, 10));
//        add(getFilesBtn, BorderLayout.NORTH);
//        add(scrollPane, BorderLayout.CENTER);
//
//        // 窗口边距
//        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//    }
//
//    /**
//     * Windows Shell COM 接口封装（内部静态类，无需额外文件）
//     */
//    private static class WindowsShellHelper {
//        // 核心方法：获取所有打开的资源管理器窗口中的选中文件
//        public static List<String> getSelectedFilesFromAllExplorers() {
//            List<String> filePaths = new ArrayList<>();
//
//            try {
//                // 使用 PowerShell 命令获取选中的文件
//                // 这种方法更可靠，不需要复杂的 COM 接口调用
//                ProcessBuilder pb = new ProcessBuilder(
//                        "powershell.exe", "-Command", 
//                        "foreach ($window in (New-Object -ComObject Shell.Application).Windows) { if ($window.Name -eq '文件资源管理器') { foreach ($item in $window.Document.SelectedItems()) { $item.Path } } }"
//                );
//                pb.redirectErrorStream(true);
//                Process process = pb.start();
//                
//                try (java.io.BufferedReader reader = new java.io.BufferedReader(
//                        new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)))
//                {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        if (!line.trim().isEmpty()) {
//                            filePaths.add(line.trim());
//                        }
//                    }
//                }
//                
//                process.waitFor();
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//
//            return filePaths;
//        }
//    }
//
//    public static void main(String[] args) {
//        // 所有 Swing UI 操作必须在事件调度线程中执行
//        SwingUtilities.invokeLater(() -> {
//            new JnaWindowsFileSelector().setVisible(true);
//        });
//    }
//}