package cn.mdkml.filenametagtool.model;

/**
 * 标签管理窗口的标签页索引常量
 * <p>
 * 使用静态常量方式，支持 switch-case 语句
 */
public final class TabIndex {
    
    private TabIndex() {
        // 私有构造函数，防止实例化
    }
    
    /** 统一标签管理页索引 */
    public static final int UNIFIED_TAG = 0;
    
    /** 设置标签页索引 */
    public static final int SETTINGS = 1;
}
