package cn.mdkml.filenametagtool;

import cn.mdkml.filenametagtool.model.Action;
import cn.mdkml.filenametagtool.model.Parsed;
import cn.mdkml.filenametagtool.util.ConfigUtil;
import cn.mdkml.filenametagtool.util.FileUtil;
import cn.mdkml.filenametagtool.util.SwingUtil;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * 文件名标签工具主类。
 * <p>
 * 提供文件名标签的添加、移除、版本管理等功能。
 * 作为右键菜单的入口，接收命令行参数并分发到对应的文件操作。
 * </p>
 */
public final class FileNameTagTool {

    /**
     * 程序入口，解析命令行参数并执行对应的文件标签操作。
     *
     * @param args 命令行参数，第一个为动作类型，后续为文件路径
     */
    public static void main(String[] args) {
        SwingUtil.initLookAndFeel();
        ConfigUtil.init();

        final Parsed parsed = Parsed.parseArgs(args);
        if (parsed.action == null) {
            SwingUtil.showMessage("缺少动作参数，请用：add | removeAll | newVersion | copyWithoutTags。");
            return;
        }

        final Action action = parsed.action;
        // 过滤出实际存在的文件路径，去重
        final List<Path> existing = parsed.paths.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FileNameTagTool::safeToPath)
                .filter(Objects::nonNull)
                .filter(p -> Files.exists(p, LinkOption.NOFOLLOW_LINKS))
                .distinct()
                .toList();

        if (existing.isEmpty()) {
            SwingUtil.showMessage("没有获取到有效的文件/文件夹路径。");
            return;
        }

        // 标签管理模式：打开标签管理窗口
        if (action == Action.MANAGE) {
            SwingUtil.createTagManagerWindow(existing.get(0).toString());
            return;
        }

        // 添加/移除标签模式：打开标签管理窗口，自动切换到对应标签页并选中文件
        if (action == Action.ADD || action == Action.REMOVE) {
            Path firstFile = existing.get(0);
            Path dir = firstFile.getParent();
            if (dir == null) {
                SwingUtil.showError("无法获取文件所在目录");
                return;
            }
            int tab = action == Action.ADD ? 1 : 2; // 1=添加标签, 2=移除标签
            SwingUtil.createTagManagerWindow(dir.toString(), tab, existing);
            return;
        }

        // NEW_VERSION / COPY_WITHOUT_TAGS / REMOVE_ALL
        try {
            int renamed = 0;
            int skipped = 0;
            for (Path path : existing) {
                try {
                    boolean success;
                    if (action == Action.NEW_VERSION) {
                        success = FileUtil.createNewVersion(path);
                    } else if (action == Action.COPY_WITHOUT_TAGS) {
                        success = FileUtil.copyWithoutTags(path);
                    } else {
                        success = FileUtil.removeAllTags(path);
                    }
                    if (success) {
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
    }

    /**
     * 安全地将字符串转换为 {@link Path} 对象，转换失败时返回 {@code null}。
     *
     * @param value 待转换的路径字符串
     * @return 对应的 Path 对象，或 {@code null}
     */
    static Path safeToPath(String value) {
        try {
            return Paths.get(value);
        } catch (Exception e) {
            return null;
        }
    }
}
