package cn.mdkml.filenametagtool;

import cn.mdkml.filenametagtool.model.Action;
import cn.mdkml.filenametagtool.model.Parsed;
import cn.mdkml.filenametagtool.util.SwingUtil;
import cn.mdkml.filenametagtool.util.ConfigUtil;

import java.nio.file.Path;
import java.util.List;

public class FileNameTagToolTest {
    
    public static void testSearch() {
      // 初始化系统外观
        SwingUtil.initLookAndFeel();
        
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
            SwingUtil.createTagManagerWindow(existing);
        } else {
            System.out.println("没有获取到有效的文件/文件夹路径。");
        }
    }
    
    public static void main(String[] args) {
        testSearch();
    }
}