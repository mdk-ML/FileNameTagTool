package local.filenametagtool;

import local.filenametagtool.model.Action;
import local.filenametagtool.model.Parsed;
import local.filenametagtool.ui.UITool;
import local.filenametagtool.util.ConfigUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class FileNameTagToolTest {
    
    public static void testSearch() {
        // 设置系统的外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // 取消标签页的焦点指示器
            UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.setProperty("java.awt.headless", "false");
        
        // 初始化配置
        ConfigUtil.init();
        
        // 模拟命令行参数
        String[] args = {
            "search",
            "C:\\Users\\MU\\Desktop\\【测试】FileNameTagTool"
        };
        
        // 解析参数
        final Parsed parsed = FileNameTagTool.parseArgs(args);
        
        // 过滤有效路径
        final List<Path> existing = parsed.paths.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FileNameTagTool::safeToPath)
                .filter(java.util.Objects::nonNull)
                .filter(p -> java.nio.file.Files.exists(p, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .distinct()
                .toList();
        
        // 执行搜索操作
        if (!existing.isEmpty() && parsed.action == Action.SEARCH) {
            UITool.createTagManagerWindow(existing);
        } else {
            System.out.println("没有获取到有效的文件/文件夹路径。");
        }
    }
    
    public static void main(String[] args) {
        testSearch();
    }
}