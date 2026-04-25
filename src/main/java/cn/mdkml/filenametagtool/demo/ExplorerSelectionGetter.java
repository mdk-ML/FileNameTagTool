//package local.filenametagtool;
//
//import com.sun.jna.*;
//import com.sun.jna.platform.win32.*;
//import com.sun.jna.platform.win32.COM.IUnknown;
//import com.sun.jna.ptr.IntByReference;
//import com.sun.jna.ptr.PointerByReference;
//import com.sun.jna.win32.W32APIOptions;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//public class ExplorerSelectionGetter extends JFrame {
//
//    // 定义 COM 接口
//    public interface IShellWindows extends IUnknown {
//        IShellWindows INSTANCE = Native.load("shell32", IShellWindows.class, W32APIOptions.DEFAULT_OPTIONS);
//
//        int Count();
//        Pointer Item(int index);
//        void _NewEnum(PointerByReference ppunk);
//    }
//
//    public interface IWebBrowserApp extends IUnknown {
//        String get_LocationURL();
//        String get_LocationName();
//    }
//
//    public interface IShellFolderViewDual extends IUnknown {
//        void SelectedItems(PointerByReference ppid);
//    }
//
//    public interface FolderItems extends IUnknown {
//        int Count();
//        Pointer Item(int index);
//    }
//
//    public interface FolderItem extends IUnknown {
//        String get_Path();
//    }
//
//    private JButton getSelectedFilesBtn;
//    private JTextArea outputArea;
//
//    public ExplorerSelectionGetter() {
//        setTitle("Windows资源管理器选中文件获取器");
//        setAlwaysOnTop(true);
//        setSize(600, 400);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLayout(new BorderLayout());
//
//        // 顶部面板
//        JPanel topPanel = new JPanel(new FlowLayout());
//        getSelectedFilesBtn = new JButton("获取选中文件");
//        getSelectedFilesBtn.addActionListener(this::getSelectedFiles);
//        topPanel.add(getSelectedFilesBtn);
//
//        // 输出区域
//        outputArea = new JTextArea();
//        outputArea.setEditable(false);
//        JScrollPane scrollPane = new JScrollPane(outputArea);
//
//        add(topPanel, BorderLayout.NORTH);
//        add(scrollPane, BorderLayout.CENTER);
//
//        setVisible(true);
//    }
//
//    private void getSelectedFiles(ActionEvent e) {
//        outputArea.setText("正在获取选中文件...\n");
//
//        try {
//            List<String> selectedFiles = getSelectedFilesFromExplorer();
//
//            if (selectedFiles.isEmpty()) {
//                outputArea.append("未找到选中的文件。请确保资源管理器中有选中的文件。\n");
//            } else {
//                outputArea.append("找到 " + selectedFiles.size() + " 个选中的文件:\n");
//                for (String filePath : selectedFiles) {
//                    outputArea.append(filePath + "\n");
//                }
//            }
//        } catch (Exception ex) {
//            outputArea.append("获取文件时出错: " + ex.getMessage() + "\n");
//            ex.printStackTrace();
//        }
//    }
//
//    private List<String> getSelectedFilesFromExplorer() {
//        List<String> selectedFiles = new ArrayList<>();
//
//        // 初始化 COM
//        Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
//
//        try {
//            // 创建 ShellWindows 对象
//            IShellWindows shellWindows = IShellWindows.INSTANCE;
//
//            // 获取资源管理器窗口数量
//            int count = shellWindows.Count();
//
//            for (int i = 0; i < count; i++) {
//                Pointer pDisp = shellWindows.Item(i);
//                if (pDisp == null) continue;
//
//                // 尝试转换为 IWebBrowserApp
//                IWebBrowserApp webBrowser = IWebBrowserApp.INSTANCE;
//
//                // 获取位置 URL
//                String locationUrl = webBrowser.get_LocationURL();
//
//                // 检查是否是文件系统路径
//                if (locationUrl != null && locationUrl.startsWith("file:///")) {
//                    // 获取选中项
//                    selectedFiles.addAll(getSelectedItemsFromWindow(pDisp));
//                }
//            }
//        } finally {
//            // 清理 COM
//            Ole32.INSTANCE.CoUninitialize();
//        }
//
//        return selectedFiles;
//    }
//
//    private List<String> getSelectedItemsFromWindow(Pointer pDisp) {
//        List<String> files = new ArrayList<>();
//
//        try {
//            // 获取 IShellFolderViewDual 接口
//            PointerByReference ppv = new PointerByReference();
//            HRESULT hr = Ole32.INSTANCE.CoCreateInstance(
//                    new Guid.CLSID("{9BA05972-F6A8-11CF-A442-00A0C90A8F39}"), // CLSID_ShellWindows
//                    null,
//                    WTypes.CLSCTX_ALL,
//                    new Guid.IID("{EAB22AC3-30C1-11CF-A7EB-0000C05BAE0B}"), // IID_IShellFolderViewDual
//                    ppv
//            );
//
//            if (!hr.equals(WinError.S_OK)) {
//                return files;
//            }
//
//            IShellFolderViewDual folderView = IShellFolderViewDual.INSTANCE;
//
//            // 获取选中项
//            PointerByReference ppid = new PointerByReference();
//            folderView.SelectedItems(ppid);
//
//            if (ppid.getValue() != null) {
//                FolderItems items = FolderItems.INSTANCE;
//                int itemCount = items.Count();
//
//                for (int j = 0; j < itemCount; j++) {
//                    Pointer pItem = items.Item(j);
//                    if (pItem != null) {
//                        FolderItem folderItem = FolderItem.INSTANCE;
//                        String path = folderItem.get_Path();
//                        if (path != null) {
//                            files.add(path);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            // 忽略错误，继续处理其他窗口
//        }
//
//        return files;
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            new ExplorerSelectionGetter();
//        });
//    }
//}