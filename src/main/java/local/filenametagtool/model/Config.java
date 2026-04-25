package local.filenametagtool.model;

import java.util.ArrayList;
import java.util.List;

public class Config {
    // 配置项名称常量
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

    private int windowX;
    private int windowY;
    private int windowW;
    private int windowH;
    private int divider;
    private int groupTagsWindowX;
    private int groupTagsWindowY;
    private int groupTagsWindowWidth;
    private int groupTagsWindowHeight;
    private String everythingPath = "C:\\Program Files\\Everything\\Everything.exe";
    private String iconPath = "C:\\Users\\MU\\Documents\\FileNameTagTool\\ico\\";
    private List<String> tags = new ArrayList<>();

    // Getters and Setters
    public int getWindowX() {
        return windowX;
    }

    public void setWindowX(int windowX) {
        this.windowX = windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public void setWindowY(int windowY) {
        this.windowY = windowY;
    }

    public int getWindowW() {
        return windowW;
    }

    public void setWindowW(int windowW) {
        this.windowW = windowW;
    }

    public int getWindowH() {
        return windowH;
    }

    public void setWindowH(int windowH) {
        this.windowH = windowH;
    }

    public int getDivider() {
        return divider;
    }

    public void setDivider(int divider) {
        this.divider = divider;
    }

    public int getGroupTagsWindowX() {
        return groupTagsWindowX;
    }

    public void setGroupTagsWindowX(int groupTagsWindowX) {
        this.groupTagsWindowX = groupTagsWindowX;
    }

    public int getGroupTagsWindowY() {
        return groupTagsWindowY;
    }

    public void setGroupTagsWindowY(int groupTagsWindowY) {
        this.groupTagsWindowY = groupTagsWindowY;
    }

    public int getGroupTagsWindowWidth() {
        return groupTagsWindowWidth;
    }

    public void setGroupTagsWindowWidth(int groupTagsWindowWidth) {
        this.groupTagsWindowWidth = groupTagsWindowWidth;
    }

    public int getGroupTagsWindowHeight() {
        return groupTagsWindowHeight;
    }

    public void setGroupTagsWindowHeight(int groupTagsWindowHeight) {
        this.groupTagsWindowHeight = groupTagsWindowHeight;
    }

    public String getEverythingPath() {
        return everythingPath;
    }

    public void setEverythingPath(String everythingPath) {
        this.everythingPath = everythingPath;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag.trim());
        }
    }

    public void clearTags() {
        tags.clear();
    }
}