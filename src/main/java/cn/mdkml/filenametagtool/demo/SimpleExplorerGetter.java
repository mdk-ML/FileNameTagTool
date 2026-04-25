//package local.filenametagtool;
//
//import com.sun.jna.*;
//import com.sun.jna.platform.win32.*;
//import com.sun.jna.ptr.PointerByReference;
//import com.sun.jna.win32.W32APIOptions;
//
//import javax.swing.*;
//import java.awt.*;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//public class SimpleExplorerGetter extends JFrame {
//
//    public interface Shell32 extends Library {
//        Shell32 INSTANCE = Native.load("shell32", Shell32.class, W32APIOptions.DEFAULT_OPTIONS);
//
//        // 获取 Shell 对象
//        Pointer SHGetIDListFromObject(Pointer pUnk, PointerByReference ppidl);
//    }
//
//    public interface Ole32 extends Library {
//        Ole32 INSTANCE = Native.load("ole32", Ole32.class, W32APIOptions.DEFAULT_OPTIONS);
//
//        int CoInitializeEx(Pointer pvReserved, int dwCoInit);
//        void CoUninitialize();
//        int CoCreateInstance(Guid.GUID rclsid, Pointer pUnkOuter, int dwClsContext,
//                             Guid.GUID riid, PointerByReference ppv);
//    }
//
//    public SimpleExplorerGetter() {
//        setTitle("资源管理器选中文件获取器");
//        setAlwaysOnTop(true);
//        setSize(500, 300);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLayout(new FlowLayout());
//
//        JButton btn = new JButton("获取选中文件");
//        btn.addActionListener(e -> getSelectedFiles());
//        add(btn);
//
//        JTextArea textArea = new JTextArea(10, 40);
//        textArea.setEditable(false);
//        add(new JScrollPane(textArea));
//
//        setVisible(true);
//    }
//
//    private void getSelectedFiles() {
//        List<String> files = getSelectedFilesFromExplorer();
//
//        JTextArea textArea = (JTextArea) ((JScrollPane) getContentPane().getComponent(1)).getViewport().getView();
//        textArea.setText("");
//
//        if (files.isEmpty()) {
//            textArea.append("未找到选中的文件\n");
//        } else {
//            textArea.append("找到 " + files.size() + " 个文件:\n");
//            for (String file : files) {
//                textArea.append(file + "\n");
//            }
//        }
//    }
//
//    private List<String> getSelectedFilesFromExplorer() {
//        List<String> result = new ArrayList<>();
//
//        // 使用 VBScript 方式获取（更简单）
//        try {
//            // 创建 Shell.Application 对象
//            String script = "Set shell = CreateObject(\"Shell.Application\")\n" +
//                    "Set windows = shell.Windows\n" +
//                    "For Each window In windows\n" +
//                    "    If InStr(window.FullName, \"explorer.exe\") > 0 Then\n" +
//                    "        Set items = window.Document.SelectedItems\n" +
//                    "        For Each item In items\n" +
//                    "            WScript.Echo item.Path\n" +
//                    "        Next\n" +
//                    "    End If\n" +
//                    "Next";
//
//            // 执行脚本并获取输出
//            Process process = Runtime.getRuntime().exec("cscript //nologo /e:vbscript stdin");
//            java.io.PrintWriter writer = new java.io.PrintWriter(process.getOutputStream());
//            writer.println(script);
//            writer.close();
//
//            java.io.BufferedReader reader = new java.io.BufferedReader(
//                    new java.io.InputStreamReader(process.getInputStream()));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                File file = new File(line.trim());
//                if (file.exists()) {
//                    result.add(file.getAbsolutePath());
//                }
//            }
//
//            reader.close();
//            process.waitFor();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> new SimpleExplorerGetter());
//    }
//}