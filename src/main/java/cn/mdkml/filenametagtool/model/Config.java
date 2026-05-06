package cn.mdkml.filenametagtool.model;

import java.util.ArrayList;
import java.util.List;

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
    public static final String KEY_EVERYTHING_PATH = "everythingPath";
    public static final String KEY_ICON_PATH = "iconPath";
    public static final String KEY_TAGS = "tag";
    /** 添加标签页-左右分隔线位置（历史标签 / 文件+自定义标签） */
    public static final String KEY_ADD_TAG_HORIZONTAL_DIVIDER = "addTagTab.horizontalDivider";
    /** 添加标签页-右侧上下分隔线位置（文件列表 / 自定义标签输入） */
    public static final String KEY_ADD_TAG_VERTICAL_DIVIDER = "addTagTab.verticalDivider";
    /** 添加标签页-智能标签与历史标签分隔线位置 */
    public static final String KEY_ADD_TAG_SMART_HISTORY_DIVIDER = "addTagTab.smartHistoryDivider";


    public static int windowX = 0;
    public static int windowY = 0;
    public static int windowW = 0;
    public static int windowH = 0;
    public static int divider = 0;
    public static int groupTagsWindowX = 0;
    public static int groupTagsWindowY = 0;
    public static int groupTagsWindowWidth = 0;
    public static int groupTagsWindowHeight = 0;
    public static String everythingPath = "C:/Program Files/Everything/Everything.exe";
    public static String iconPath = "C:/Users/MU/Documents/FileNameTagTool/ico/";
    public static List<String> tags = new ArrayList<>();
    /** 添加标签页-左右分隔线位置 */
    public static int addTagHorizontalDivider = 0;
    /** 添加标签页-右侧上下分隔线位置 */
    public static int addTagVerticalDivider = 0;
    /** 添加标签页-智能标签与历史标签分隔线位置 */
    public static int addTagSmartHistoryDivider = 0;

}