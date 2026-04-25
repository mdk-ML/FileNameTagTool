package local.filenametagtool;

import local.filenametagtool.model.Action;
import local.filenametagtool.model.Parsed;
import local.filenametagtool.operation.FileOperation;
import local.filenametagtool.manager.TagManager;
import local.filenametagtool.ui.UITool;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 文件名标签工具主类
 * 用于在文件名开头添加、移除标签，以及管理文件版本
 */
public final class FileNameTagTool {

    /**
     * 主方法，程序入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 初始化配置
        local.filenametagtool.util.ConfigUtil.init();

        final Parsed parsed = parseArgs(args);
        if (parsed.action == null) {
            UITool.showMessage("缺少动作参数，请用：add | removeAll | newVersion | copyWithoutTags。", "提示");
            return;
        }

        final Action action = parsed.action;
        final List<Path> existing = parsed.paths.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FileNameTagTool::safeToPath)
                .filter(Objects::nonNull)
                .filter(p -> java.nio.file.Files.exists(p, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .distinct()
                .toList();

        if (existing.isEmpty()) {
            UITool.showMessage("没有获取到有效的文件/文件夹路径。请先在资源管理器中选中后再点击菜单。", "提示");
            return;
        }

        if (action == Action.SEARCH) {
            UITool.createTagManagerWindow(existing);
            return;
        }

        final List<String> addTags;
        final Set<String> removeTags;
        if (action == Action.ADD) {
            final Object[] result = UITool.askTagsWithHistory();
            if (result == null) return;

            final List<String> tags = (List<String>) result[0];
            final Set<String> smartTags = (Set<String>) result[1];

            final List<String> normalized = TagManager.normalizeTags(tags);
            if (normalized.isEmpty()) return;

            TagManager.rememberTags(normalized, smartTags);
            addTags = normalized;
            removeTags = null;
        } else if (action == Action.REMOVE) {
            removeTags = UITool.askTagsToRemove(existing);
            if (removeTags == null) return;
            addTags = null;
        } else {
            addTags = null;
            removeTags = null;
        }

        try {
            int renamed = 0;
            int skipped = 0;
            for (Path p : existing) {
                try {
                    final boolean ok;
                    if (action == Action.ADD) {
                        ok = FileOperation.addTagsToNamePrefix(p, addTags);
                    } else if (action == Action.REMOVE) {
                        ok = FileOperation.removeTags(p, removeTags);
                    } else if (action == Action.NEW_VERSION) {
                        ok = FileOperation.createNewVersion(p);
                    } else if (action == Action.COPY_WITHOUT_TAGS) {
                        ok = FileOperation.copyWithoutTags(p);
                    } else {
                        ok = FileOperation.removeAllTags(p);
                    }
                    if (ok) renamed++;
                    else skipped++;
                } catch (Exception e) {
                    skipped++;
                }
            }

            UITool.showMessage("选择项：" + existing.size() + "\n成功重命名：" + renamed + "\n跳过/失败：" + skipped, "完成");
        } catch (Exception e) {
            UITool.showMessage(String.valueOf(e), "错误");
        }
    }

    /**
     * 解析命令行参数
     *
     * @param args 命令行参数
     * @return 解析后的动作和路径
     */
    private static Parsed parseArgs(String[] args) {
        if (args == null || args.length == 0) return new Parsed(null, List.of());
        Action action = Action.fromArg(args[0]);
        List<String> paths = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a == null) continue;
            String v = a.trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            if (!v.isEmpty()) paths.add(v);
        }
        return new Parsed(action, paths);
    }

    /**
     * 安全地将字符串转换为Path对象
     *
     * @param s 字符串
     * @return Path对象，若转换失败则返回null
     */
    private static Path safeToPath(String s) {
        try {
            return Paths.get(s);
        } catch (Exception e) {
            return null;
        }
    }
}
