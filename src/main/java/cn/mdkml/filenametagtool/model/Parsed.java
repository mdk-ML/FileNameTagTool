package cn.mdkml.filenametagtool.model;

import java.util.ArrayList;
import java.util.List;

public class Parsed {
    public final Action action;
    public final List<String> paths;

    public Parsed(Action action, List<String> paths) {
        this.action = action;
        this.paths = paths;
    }

    /**
     * 解析命令行参数
     *
     * @param args 命令行参数
     * @return 解析后的动作和路径
     */
    public static Parsed parseArgs(String[] args) {
        if (args == null || args.length == 0) {
            return new Parsed(null, List.of());
        }
        Action action = Action.fromArg(args[0]);
        List<String> paths = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a == null) {
                continue;
            }
            String v = a.trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            if (!v.isEmpty()) {
                paths.add(v);
            }
        }
        return new Parsed(action, paths);
    }
}
