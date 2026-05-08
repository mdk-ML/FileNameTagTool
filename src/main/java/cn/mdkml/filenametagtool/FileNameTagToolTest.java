package cn.mdkml.filenametagtool;

import cn.mdkml.filenametagtool.model.Action;
import cn.mdkml.filenametagtool.model.Parsed;
import cn.mdkml.filenametagtool.util.SwingUtil;
import cn.mdkml.filenametagtool.util.ConfigUtil;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * FileNameTagTool 的本地测试类。
 * <p>
 * 用于在开发阶段快速验证标签管理窗口等功能，不作为正式测试用例。
 * </p>
 */
public class FileNameTagToolTest {

    /**
     * 模拟 manage 命令，打开标签管理窗口。
     */
    public static void testSearch() {
        SwingUtil.initLookAndFeel();
        ConfigUtil.init();

        String[] args = {
            "manage",
            "C:\\Users\\MU\\Desktop\\【测试】FileNameTagTool"
        };

        final Parsed parsed = Parsed.parseArgs(args);

        // 过滤出实际存在的文件路径
        final List<Path> existing = parsed.paths.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FileNameTagTool::safeToPath)
                .filter(Objects::nonNull)
                .filter(p -> Files.exists(p, LinkOption.NOFOLLOW_LINKS))
                .distinct()
                .toList();

        if (!existing.isEmpty() && parsed.action == Action.MANAGE) {
            SwingUtil.createTagManagerWindow(existing.get(0).toString());
        } else {
            System.out.println("没有获取到有效的文件/文件夹路径。");
        }
    }
    /**
     * 模拟 add 命令，打开标签管理窗口并自动切换到添加标签页、选中指定文件。
     */
    public static void testAdd() {
        SwingUtil.initLookAndFeel();
        ConfigUtil.init();

        String[] args = {
            "add",
            "C:\\Users\\MU\\Desktop\\PMP\\预测部分思维导图汇总.pdf"
        };

        final Parsed parsed = Parsed.parseArgs(args);

        final List<Path> existing = parsed.paths.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FileNameTagTool::safeToPath)
                .filter(Objects::nonNull)
                .filter(p -> Files.exists(p, LinkOption.NOFOLLOW_LINKS))
                .distinct()
                .toList();

        if (!existing.isEmpty() && parsed.action == Action.ADD) {
            Path firstFile = existing.get(0);
            Path dir = firstFile.getParent();
            if (dir != null) {
                // initialTab = 1 对应添加标签页，filesToSelect 自动选中目标文件
                SwingUtil.createTagManagerWindow(dir.toString(), 1, existing);
            }
        } else {
            System.out.println("没有获取到有效的文件/文件夹路径。");
        }
    }

    /**
     * 测试入口。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // testSearch();
        testAdd();
    }
}
