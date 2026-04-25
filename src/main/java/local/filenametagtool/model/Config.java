package local.filenametagtool.model;

import java.util.ArrayList;
import java.util.List;

public class Config {
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


    public static int windowX=0;
    public static int windowY=0;
    public static int windowW=0;
    public static int windowH=0;
    public static int divider=0;
    public static int groupTagsWindowX=0;
    public static int groupTagsWindowY=0;
    public static int groupTagsWindowWidth=0;
    public static int groupTagsWindowHeight=0;
    public static String everythingPath = "C:\\Program Files\\Everything\\Everything.exe";
    public static String iconPath = "C:\\Users\\MU\\Documents\\FileNameTagTool\\ico\\";
    public static List<String> tags = new ArrayList<>();

    public static void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag.trim());
        }
    }

    public static void clearTags() {
        tags.clear();
    }
}