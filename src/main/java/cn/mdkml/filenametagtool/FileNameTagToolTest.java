package cn.mdkml.filenametagtool;

import cn.mdkml.filenametagtool.model.Action;
import cn.mdkml.filenametagtool.model.Parsed;
import cn.mdkml.filenametagtool.util.FileUtil;
import cn.mdkml.filenametagtool.util.SwingUtil;
import cn.mdkml.filenametagtool.util.ConfigUtil;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
            "C:\\Users\\MU\\Desktop\\PMP\\880视频课程（周子裕、李凤兰）"
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
                .collect(Collectors.toList());

        if (!existing.isEmpty() && parsed.action == Action.MANAGE) {
            SwingUtil.createTagManagerWindow(existing.get(0).toString());
        } else {
            System.out.println("没有获取到有效的文件/文件夹路径。");
        }
    }
    public static void testNewVersion() {
        SwingUtil.initLookAndFeel();
        ConfigUtil.init();

        String[] args = {
            "newVersion",
            "C:\\Users\\MU\\Desktop\\PMP\\880视频课程（周子裕、李凤兰）\\第14章 其他敏捷实践.pdf"
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
                .collect(Collectors.toList());

        if (!existing.isEmpty() && parsed.action == Action.NEW_VERSION) {
            try {
                int renamed = 0;
                int skipped = 0;
                for (Path path : existing) {
                    try {
                        if (FileUtil.createNewVersion(path)) {
                            renamed++;
                        } else {
                            skipped++;
                        }
                    } catch (Exception e) {
                        skipped++;
                    }
                }
                SwingUtil.showSuccess("选择项：" + existing.size() + "\n成功重命名：" + renamed + "\n跳过/失败：" + skipped);
            } catch (Exception e) {
                SwingUtil.showError(String.valueOf(e));
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
        testNewVersion();
    }
}
