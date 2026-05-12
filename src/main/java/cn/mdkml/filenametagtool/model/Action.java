package cn.mdkml.filenametagtool.model;

/**
 * 文件操作动作枚举
 * <p>
 * 定义了文件名标签工具支持的所有操作类型，每个枚举值对应一个命令行参数名称
 */
public enum Action {
    /** 移除文件名中的所有标签 */
    REMOVE_ALL("removeAll"),
    /** 创建文件的新版本 */
    NEW_VERSION("newVersion"),
    /** 复制文件并去除标签 */
    COPY_WITHOUT_TAGS("copyWithoutTags"),
    /** 搜索包含指定标签的文件 */
    MANAGE("manage");

    /** 命令行参数名称 */
    public final String arg;

    Action(String arg) {
        this.arg = arg;
    }

    /**
     * 根据命令行参数名称查找对应的动作枚举值
     *
     * @param s 命令行参数字符串
     * @return 对应的动作枚举值，未匹配则返回null
     */
    public static Action fromArg(String s) {
        if (s == null) {
            return null;
        }
        for (Action a : values()) {
            if (a.arg.equalsIgnoreCase(s.trim())) {
                return a;
            }
        }
        return null;
    }
}
