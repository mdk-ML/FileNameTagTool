package cn.mdkml.filenametagtool;

import cn.mdkml.filenametagtool.model.Action;
import cn.mdkml.filenametagtool.model.Parsed;
import cn.mdkml.filenametagtool.util.ConfigUtil;
import cn.mdkml.filenametagtool.util.FileUtil;
import cn.mdkml.filenametagtool.util.SwingUtil;
import cn.mdkml.filenametagtool.util.TagUtil;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

        final List<String> addTags;
        final Set<String> removeTags;
        if (action == Action.ADD) {
            // 弹窗让用户输入要添加的标签
            final Object[] result = SwingUtil.askTagsWithHistory();
            if (result == null) {
                return;
            }
            final List<String> tags = (List<String>) result[0];
            final Set<String> smartTags = (Set<String>) result[1];
            final List<String> normalized = TagUtil.normalizeTags(tags);
            if (normalized.isEmpty()) {
                return;
            }
            TagUtil.rememberTags(normalized, smartTags);
            addTags = normalized;
            removeTags = null;
        } else if (action == Action.REMOVE) {
            // 弹窗让用户选择要移除的标签
            removeTags = SwingUtil.askTagsToRemove(existing);
            if (removeTags == null) {
                return;
            }
            addTags = null;
        } else {
            addTags = null;
            removeTags = null;
        }

        try {
            int renamed = 0;
            int skipped = 0;
            for (Path path : existing) {
                try {
                    boolean success;
                    if (action == Action.ADD) {
                        success = FileUtil.addTags(path, addTags);
                    } else if (action == Action.REMOVE) {
                        success = FileUtil.removeTags(path, removeTags);
                    } else if (action == Action.NEW_VERSION) {
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
