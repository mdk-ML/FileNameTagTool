package cn.mdkml.filenametagtool.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public static final String CONFIG_FILE_PATH = ".filenametagtool";
    public static final String CONFIG_FILE_NAME = "filename-tagtool.conf";
    public static final String DELIMITER = ",";
    public static final String KEY_WINDOW_X = "window.x";
    public static final String KEY_WINDOW_Y = "window.y";
    public static final String KEY_WINDOW_W = "window.w";
    public static final String KEY_WINDOW_H = "window.h";
    public static final String KEY_DIVIDER = "ui.divider";
    public static final String KEY_GROUP_TAGS_WINDOW_X = "groupTagsWindowX";
    public static final String KEY_GROUP_TAGS_WINDOW_Y = "groupTagsWindowY";
    public static final String KEY_GROUP_TAGS_WINDOW_WIDTH = "groupTagsWindowWidth";
    public static final String KEY_GROUP_TAGS_WINDOW_HEIGHT = "groupTagsWindowHeight";
    public static final String KEY_ICON_PATH = "iconPath";
    public static final String KEY_TAGS = "tag";
    /** 添加标签页-左右分隔线位置（历史标签 / 文件+自定义标签） */
    public static final String KEY_ADD_TAG_HORIZONTAL_DIVIDER = "addTagTab.horizontalDivider";
    /** 添加标签页-右侧上下分隔线位置（文件列表 / 自定义标签输入） */
    public static final String KEY_ADD_TAG_VERTICAL_DIVIDER = "addTagTab.verticalDivider";
    /** 添加标签页-智能标签与历史标签分隔线位置 */
    public static final String KEY_ADD_TAG_SMART_HISTORY_DIVIDER = "addTagTab.smartHistoryDivider";
    /** 标签包裹符号样式 */
    public static final String KEY_TAG_BRACKET_STYLE = "tag.bracket.style";
    /** 全角方括号样式 【】 */
    public static final String STYLE_FULLWIDTH = "fullwidth";
    /** 半角方括号样式 [] */
    public static final String STYLE_BRACKET = "bracket";
    /** 文件列表排序列索引（0=名称, 1=修改日期, 2=类型, 3=大小） */
    public static final String KEY_FILE_SORT_COLUMN = "file.sort.column";
    /** 文件列表排序方向（0=升序, 1=降序） */
    public static final String KEY_FILE_SORT_ASCENDING = "file.sort.ascending";
    /** 文件列表各列宽度（逗号分隔：名称,标签,修改日期,类型,大小） */
    public static final String KEY_FILE_COLUMN_WIDTHS = "file.column.widths";
    /** 标签颜色配置（格式：标签名1:#FF0000,标签名2:#00FF00） */
    public static final String KEY_TAG_COLORS = "tag.colors";

    public static int windowX = 0;
    public static int windowY = 0;
    public static int windowW = 0;
    public static int windowH = 0;
    public static int divider = 0;
    public static int groupTagsWindowX = 0;
    public static int groupTagsWindowY = 0;
    public static int groupTagsWindowWidth = 0;
    public static int groupTagsWindowHeight = 0;
    public static String iconPath = "C:/Users/MU/Documents/FileNameTagTool/ico/";
    public static List<String> tags = new ArrayList<>();
    /** 添加标签页-左右分隔线位置 */
    public static int addTagHorizontalDivider = 0;
    /** 添加标签页-右侧上下分隔线位置 */
    public static int addTagVerticalDivider = 0;
    /** 添加标签页-智能标签与历史标签分隔线位置 */
    public static int addTagSmartHistoryDivider = 0;
    /** 标签包裹符号样式 */
    public static String tagBracketStyle = STYLE_FULLWIDTH;
    /** 文件列表排序列索引（0=名称, 1=修改日期, 2=类型, 3=大小） */
    public static int fileSortColumn = 0;
    /** 文件列表排序方向（true=升序, false=降序） */
    public static boolean fileSortAscending = true;
    /** 文件列表各列宽度（名称,标签,修改日期,类型,大小） */
    public static int[] fileColumnWidths = {200, 150, 140, 80, 80};
    /** 标签颜色映射表 */
    public static Map<String, Color> tagColors = new HashMap<>();

    /**
     * 获取当前标签左包裹符号
     *
     * @return 左包裹符号
     */
    public static String getTagWrapLeft() {
        return STYLE_BRACKET.equals(tagBracketStyle) ? "[" : "【";
    }

    /**
     * 获取当前标签右包裹符号
     *
     * @return 右包裹符号
     */
    public static String getTagWrapRight() {
        return STYLE_BRACKET.equals(tagBracketStyle) ? "]" : "】";
    }

}