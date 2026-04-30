package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.component.BadgeToggleButton;
import cn.mdkml.filenametagtool.component.WrapLayout;
import cn.mdkml.filenametagtool.model.Config;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public final class SwingUtil {

    private static final Color GRADIENT_START = new Color(74, 144, 226);
    private static final Color BG_LIGHT = new Color(248, 249, 250);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BG_CONTENT = new Color(249, 249, 249);
    private static final Color BG_MAIN = new Color(240, 240, 240);
    private static final Color BORDER_GRAY = new Color(220, 220, 220);
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color TEXT_LIGHT = Color.WHITE;

    /**
     * 初始化系统外观设置
     */
    public static void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));
            UIManager.put("SplitPaneDivider.border", BorderFactory.createLineBorder(BG_CONTENT));
            UIManager.put("SplitPane.background", BG_CONTENT);       // 分隔线背景
        } catch (Exception e) {
            showError("初始化系统外观失败：" + e.getMessage());
        }
        System.setProperty("java.awt.headless", "false");
    }

    /**
     * 显示提示弹窗（2 秒后自动关闭）。
     *
     * @param msg 消息内容
     */
    public static void showMessage(String msg) {
        showNotification(msg, "提示", MessageType.INFO);
    }

    /**
     * 显示成功弹窗（2 秒后自动关闭）。
     *
     * @param msg 消息内容
     */
    public static void showSuccess(String msg) {
        showNotification(msg, "成功", MessageType.SUCCESS);
    }

    /**
     * 显示错误弹窗（需用户手动关闭）。
     *
     * @param msg 错误内容
     */
    public static void showError(String msg) {
        showNotification(msg, "错误", MessageType.ERROR);
    }

    /** 弹窗类型枚举 */
    private enum MessageType { INFO, SUCCESS, ERROR }

    /**
     * 通知弹窗内部实现。
     *
     * @param msg 消息内容
     * @param title 标题
     * @param type 弹窗类型
     */
    private static void showNotification(String msg, String title, MessageType type) {
        boolean isError = type == MessageType.ERROR;

        JDialog dialog = new JDialog((Frame) null, title, false);
        dialog.setAlwaysOnTop(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        // 标题颜色：错误红色，成功绿色，提示蓝色
        Color titleColor;
        String iconText;
        Color iconColor;
        if (type == MessageType.ERROR) {
            titleColor = new Color(211, 47, 47);
            iconText = "✕";
            iconColor = new Color(211, 47, 47);
        } else if (type == MessageType.SUCCESS) {
            titleColor = new Color(76, 175, 80);
            iconText = "✓";
            iconColor = new Color(76, 175, 80);
        } else {
            titleColor = GRADIENT_START;
            iconText = "ℹ";
            iconColor = GRADIENT_START;
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        titleLabel.setForeground(titleColor);

        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        iconLabel.setForeground(iconColor);

        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setBackground(BG_WHITE);
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel msgLabel = new JLabel("<html><body style='width:280px;'>" + msg.replace("\n", "<br>") + "</body></html>");
        msgLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        msgLabel.setForeground(TEXT_DARK);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(Box.createVerticalStrut(12), BorderLayout.WEST);
        mainPanel.add(msgLabel, BorderLayout.CENTER);

        // 错误弹窗添加确定按钮，需手动关闭
        if (isError) {
            JButton okButton = new JButton("确定");
            okButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            okButton.setPreferredSize(new Dimension(80, 32));
            okButton.setFocusPainted(false);
            okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            okButton.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(BG_WHITE);
            buttonPanel.add(okButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        }

        dialog.getContentPane().add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        Dimension size = dialog.getSize();
        int maxWidth = 400;
        int maxHeight = 250;
        if (size.width > maxWidth || size.height > maxHeight) {
            dialog.setSize(Math.min(size.width, maxWidth), Math.min(size.height, maxHeight));
        }

        dialog.setVisible(true);

        // 非错误弹窗 2 秒后自动关闭
        if (!isError) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                SwingUtilities.invokeLater(dialog::dispose);
            }).start();
        }
    }

    /**
     * 询问用户要添加的标签，支持历史标签和智能标签
     *
     * @return 包含标签列表和智能标签集合的数组
     */
    public static Object[] askTagsWithHistory() {
        ConfigUtil.reload();
        List<String> history = new ArrayList<>();
        for (String tag : Config.tags) {
            if (!FileUtil.TAG_ORDER_FILENAME.equals(tag) && !FileUtil.TAG_ORDER_VERSION.equals(tag)) {
                history.add(tag);
            }
        }

        final JTextArea input = new JTextArea();
        input.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        input.setPreferredSize(new Dimension(400, 120));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setText("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈");
        input.setForeground(new Color(160, 160, 160));
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (input.getText().equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈")) {
                    input.setText("");
                    input.setForeground(TEXT_DARK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (input.getText().isEmpty()) {
                    input.setText("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈");
                    input.setForeground(new Color(160, 160, 160));
                }
            }
        });

        JPanel tagsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        List<JToggleButton> toggles = new ArrayList<>();
        for (String tag : history) {
            final String t = tag;
            JToggleButton b = new JToggleButton(tag);
            b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            b.setForeground(TEXT_DARK);
            b.setBackground(BG_WHITE);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setPreferredSize(new Dimension(80, 36));
            b.setHorizontalAlignment(SwingConstants.CENTER);

            b.addItemListener(e -> {
                if (b.isSelected()) {
                    b.setForeground(TEXT_LIGHT);
                    b.setBackground(GRADIENT_START);
                    b.setBorderPainted(false);
                } else {
                    b.setForeground(TEXT_DARK);
                    b.setBackground(BG_WHITE);
                    b.setBorderPainted(true);
                    b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
                }
                if (b.isSelected()) {
                    String currentText = input.getText();
                    if (currentText.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈") || currentText.trim()
                                                                                                                                  .isEmpty()) {
                        input.setText(t);
                        input.setForeground(TEXT_DARK);
                    } else {
                        String lastChar = currentText.substring(currentText.length() - 1);
                        if (lastChar.equals(" ") || lastChar.equals("\n")) {
                            input.setText(currentText + t);
                        } else {
                            input.setText(currentText + " " + t);
                        }
                    }
                } else {
                    String currentText = input.getText();
                    String updatedText = currentText.replaceAll("(,\s*)?" + Pattern.quote(t), "");
                    input.setText(updatedText);
                }
                input.requestFocusInWindow();
            });
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        for (JToggleButton tb : toggles) {
                            tb.setForeground(TEXT_DARK);
                            tb.setBackground(BG_WHITE);
                            tb.setBorderPainted(true);
                            tb.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
                            tb.setSelected(false);
                        }
                        b.setForeground(TEXT_LIGHT);
                        b.setBackground(GRADIENT_START);
                        b.setBorderPainted(false);
                        b.setSelected(true);
                        input.setText(t);
                        input.setForeground(TEXT_DARK);
                        input.putClientProperty("instantOk", Boolean.TRUE);
                        Window w = SwingUtilities.getWindowAncestor(b);
                        if (w != null) w.dispose();
                    }
                }
            });
            toggles.add(b);
            tagsPanel.add(b);
        }

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_WHITE);

        final Set<String> smartTags = new HashSet<>();

        JPanel smartTagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        smartTagsPanel.setBackground(BG_WHITE);

        // 当前日期标签
        JPanel dateTagPanel = createSmartTagPanel("📅", "当前日期", java.time.LocalDate.now()
                                                                                      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")), () -> {
            String dateTag = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            smartTags.add(dateTag);
            return dateTag;
        });
        smartTagsPanel.add(dateTagPanel);

        // 最近使用标签
        JPanel recentTagPanel = createSmartTagPanel("🔄", "最近使用", "工作, 重要", () -> {
            smartTags.add("工作");
            smartTags.add("重要");
            return "工作 重要";
        });
        smartTagsPanel.add(recentTagPanel);

        // 热门标签
        JPanel hotTagPanel = createSmartTagPanel("🔥", "热门标签", "项目A 会议", () -> {
            smartTags.add("项目A");
            smartTags.add("会议");
            return "项目A 会议";
        });
        smartTagsPanel.add(hotTagPanel);

        // 推荐标签
        JPanel recommendTagPanel = createSmartTagPanel("💡", "推荐标签", "文档 计划", () -> {
            smartTags.add("文档");
            smartTags.add("计划");
            return "文档 计划";
        });
        smartTagsPanel.add(recommendTagPanel);

        JPanel inputLabelPanel = new JPanel();
        inputLabelPanel.setLayout(new BoxLayout(inputLabelPanel, BoxLayout.Y_AXIS));
        inputLabelPanel.setBackground(BG_WHITE);
        JLabel inputLabel = new JLabel("自定义标签内容");
        inputLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        inputLabel.setForeground(TEXT_DARK);
        inputLabelPanel.add(inputLabel);
        inputLabelPanel.add(Box.createVerticalStrut(4));
        JLabel inputHint = new JLabel("请输入或要增加的标签内容（支持多个标签，用空格分隔）：");
        inputHint.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        inputHint.setForeground(new Color(120, 120, 120));
        inputLabelPanel.add(inputHint);
        inputLabelPanel.add(Box.createVerticalStrut(8));
        inputLabelPanel.add(input);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(BG_WHITE);
        bottom.add(Box.createVerticalStrut(20));
        bottom.add(smartTagsPanel);
        bottom.add(Box.createVerticalStrut(20));
        bottom.add(inputLabelPanel);
        bottom.add(Box.createVerticalStrut(20));

        JButton ok = new JButton("确定");
        JButton cancel = new JButton("取消");
        stylePrimaryButton(ok);
        styleSecondaryButton(cancel);

        final JDialog dialog = new JDialog((Frame) null, "", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(BG_WHITE);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBackground(BG_WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        headerPanel.setPreferredSize(new Dimension(600, 60));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setBackground(BG_WHITE);
        JLabel iconLabel = new JLabel("💎");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        JLabel titleLabel = new JLabel("增加标签");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_DARK);
        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        JPanel closePanel = new JPanel();
        closePanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closePanel.setBackground(BG_WHITE);
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
        closeButton.setForeground(TEXT_DARK);
        closeButton.setBackground(BG_WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(true);
        closeButton.setBorderPainted(false);
        closeButton.setPreferredSize(new Dimension(30, 30));
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            input.setText("");
            input.putClientProperty("cancelled", Boolean.TRUE);
            dialog.dispose();
        });
        closePanel.add(closeButton);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(closePanel, BorderLayout.EAST);

        ok.addActionListener(e -> dialog.dispose());
        cancel.addActionListener(e -> {
            input.setText("");
            input.putClientProperty("cancelled", Boolean.TRUE);
            dialog.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttons.setBackground(BG_WHITE);
        buttons.add(cancel);
        buttons.add(ok);

        bottom.add(buttons);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tagsScroll, bottom);
        split.setResizeWeight(0.5);
        split.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        split.setBackground(BG_WHITE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBackground(BG_WHITE);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(split, BorderLayout.CENTER);

        dialog.getContentPane().add(contentPanel);

        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(800, 600));

        if (Config.windowW > 0 && Config.windowH > 0) {
            dialog.setBounds(Config.windowX, Config.windowY, Config.windowW, Config.windowH);
        } else {
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setSize(new Dimension(840, 680));
        }
        if (Config.divider > 0) split.setDividerLocation(Config.divider);
        else split.setDividerLocation(0.5);

        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            input.selectAll();
        });

        Config.windowX = dialog.getX();
        Config.windowY = dialog.getY();
        Config.windowW = dialog.getWidth();
        Config.windowH = dialog.getHeight();
        Config.divider = split.getDividerLocation();
        ConfigUtil.save();

        Object cancelled = input.getClientProperty("cancelled");
        if (Boolean.TRUE.equals(cancelled)) return null;

        String typed = input.getText();
        if (typed.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈")) {
            typed = "";
        }
        List<String> tags = splitTags(typed);
        return new Object[]{tags, smartTags};
    }

    /**
     * 询问用户要移除的标签
     *
     * @param files 文件路径列表
     * @return 要移除的标签集合
     */
    public static Set<String> askTagsToRemove(List<java.nio.file.Path> files) {
        Set<String> existingFileTags = new LinkedHashSet<>();
        for (java.nio.file.Path file : files) {
            java.nio.file.Path fileName = file.getFileName();
            if (fileName != null) {
                String name = fileName.toString();
                List<String> tags = FileUtil.parseAllTags(name);
                existingFileTags.addAll(tags);
            }
        }

        if (existingFileTags.isEmpty()) {
            showMessage("所选文件没有标签可移除。");
            return null;
        }

        final Set<String> tagsToRemove = new HashSet<>();
        final Color REMOVE_RED = new Color(244, 67, 54);

        JPanel tagsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
        List<JToggleButton> toggles = new ArrayList<>();
        for (String tag : existingFileTags) {
            final String t = tag;
            JToggleButton b = new JToggleButton(tag);
            b.setBackground(BG_WHITE);
            b.setForeground(TEXT_DARK);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
            b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));

            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                        if (tagsToRemove.contains(t)) {
                            tagsToRemove.remove(t);
                            b.setBackground(BG_WHITE);
                            b.setForeground(TEXT_DARK);
                            b.setBorderPainted(true);
                            b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
                        } else {
                            tagsToRemove.add(t);
                            b.setBackground(REMOVE_RED);
                            b.setForeground(Color.WHITE);
                            b.setBorderPainted(false);
                        }
                    } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        tagsToRemove.clear();
                        tagsToRemove.add(t);
                        Window w = SwingUtilities.getWindowAncestor(b);
                        if (w != null) w.dispose();
                    }
                }
            });
            toggles.add(b);
            tagsPanel.add(b);
        }

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0), "文件标签（单击标记待移除，双击直接移除）", SwingConstants.LEFT, SwingConstants.TOP, new Font("Microsoft YaHei UI", Font.PLAIN, 13), TEXT_DARK));
        tagsScroll.setBackground(BG_LIGHT);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(BG_WHITE);
        bottom.add(Box.createVerticalStrut(16));

        JButton remove = new JButton("移除标签");
        JButton cancel = new JButton("取消");
        stylePrimaryButton(remove);
        styleSecondaryButton(cancel);

        final JDialog dialog = new JDialog((Frame) null, "移除标签", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(BG_WHITE);

        remove.addActionListener(e -> dialog.dispose());
        cancel.addActionListener(e -> {
            tagsToRemove.clear();
            dialog.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        buttons.setBackground(BG_WHITE);
        buttons.add(remove);
        buttons.add(cancel);

        bottom.add(buttons);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tagsScroll, bottom);
        split.setResizeWeight(0.80);
        split.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        split.setBackground(BG_LIGHT);
        dialog.getContentPane().add(split);

        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(580, 420));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setSize(new Dimension(620, 480));

        dialog.setVisible(true);

        return tagsToRemove.isEmpty() ? null : tagsToRemove;
    }

    /**
     * 创建标签管理窗口。
     *
     * @param path 目录路径
     */
    public static void createTagManagerWindow(String path) {
        ConfigUtil.reload();

        // 1. 空值检查
        if (path == null) {
            showError("路径参数不能为空");
            return;
        }

        // 2. 空字符串检查
        path = path.trim();
        if (path.isEmpty()) {
            showError("路径字符串不能为空");
            return;
        }

        Path dirPath = Paths.get(path);

        // 3. 路径存在性检查
        if (!Files.exists(dirPath)) {
            showError("路径不存在: " + path);
            return;
        }

        // 4. 目录类型检查（严格验证：必须是目录）
        if (!Files.isDirectory(dirPath)) {
            showError("参数类型不匹配：" + path + "\n请提供有效的目录路径，而非文件路径");
            return;
        }

        // 5. 目录可读性检查
        if (!Files.isReadable(dirPath)) {
            showError("无法读取目录：" + path + "\n请检查目录权限");
            return;
        }

        // 6. 空目录检查
        try {
            if (Files.list(dirPath).filter(Files::isRegularFile).count() == 0) {
                showMessage("目录为空：" + path);
                return;
            }
        } catch (IOException e) {
            showError("无法访问目录: " + path + "\n错误: " + e.getMessage());
            return;
        }

        JFrame frame = new JFrame("文件标签管理 " + path);
        List<Image> icons = new ArrayList<>();
        try {
            String iconBasePath = Config.iconPath;
            icons.add(new ImageIcon(iconBasePath + "tags-16.png").getImage());
            icons.add(new ImageIcon(iconBasePath + "tags-32.png").getImage());
            icons.add(new ImageIcon(iconBasePath + "tags-48.png").getImage());
            icons.add(new ImageIcon(iconBasePath + "tags-64.png").getImage());
            frame.setIconImages(icons);
        } catch (Exception e) {
            showError("加载图标失败：" + e.getMessage());
        }
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(true);

        if (Config.groupTagsWindowWidth > 0 && Config.groupTagsWindowHeight > 0) {
            frame.setSize(Config.groupTagsWindowWidth, Config.groupTagsWindowHeight);
        } else {
            frame.setSize(800, 600);
        }

        if (Config.groupTagsWindowX > 0 && Config.groupTagsWindowY > 0) {
            frame.setLocation(Config.groupTagsWindowX, Config.groupTagsWindowY);
        } else {
            frame.setLocationRelativeTo(null);
        }

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        final String finalPath = path;
        final JTabbedPane finalTabbedPane = tabbedPane;
        final Runnable[] refreshSearch = new Runnable[1];
        final Runnable[] refreshAddTag = new Runnable[1];
        final Runnable[] refreshRemoveTag = new Runnable[1];
        final Runnable[] refreshSettings = new Runnable[1];
        refreshSearch[0] = () -> finalTabbedPane.setComponentAt(0, createSearchTab(finalPath, refreshSearch[0]));
        refreshAddTag[0] = () -> finalTabbedPane.setComponentAt(1, createAddTagTab(finalPath, refreshAddTag[0]));
        refreshRemoveTag[0] = () -> finalTabbedPane.setComponentAt(2, createRemoveTagTab(finalPath, refreshRemoveTag[0]));
        refreshSettings[0] = () -> finalTabbedPane.setComponentAt(3, createSettingsTab(finalPath, refreshSettings[0]));

        tabbedPane.addTab("搜索", null, createSearchTab(path, refreshSearch[0]));
        tabbedPane.addTab("添加标签", null, createAddTagTab(path, refreshAddTag[0]));
        tabbedPane.addTab("移除标签", null, createRemoveTagTab(path, refreshRemoveTag[0]));
        tabbedPane.addTab("设置", null, createSettingsTab(path, refreshSettings[0]));

        // 切换标签时刷新对应面板内容
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = finalTabbedPane.getSelectedIndex();
            switch (selectedIndex) {
                case 0 -> refreshSearch[0].run();
                case 1 -> refreshAddTag[0].run();
                case 2 -> refreshRemoveTag[0].run();
                case 3 -> refreshSettings[0].run();
            }
        });

        mainContainer.add(tabbedPane, BorderLayout.CENTER);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveWindowPosition(frame);
            }
        });

        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                saveWindowPosition(frame);
            }

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                saveWindowPosition(frame);
            }
        });

        frame.setContentPane(mainContainer);
        frame.setVisible(true);
    }

    /**
     * 创建搜索标签面板。
     *
     * @param path         目录路径
     * @param refreshAction 刷新回调
     * @return 搜索面板
     */
    private static JPanel createSearchTab(String path, Runnable refreshAction) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(BG_CONTENT);

        EverythingUtil searcher = EverythingUtil.getInstance();
        if (!searcher.isEverythingRunning()) {
            JLabel errorLabel = new JLabel("错误：Everything 客户端未运行，请先启动 Everything");
            errorLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
            errorLabel.setForeground(Color.RED);
            panel.add(errorLabel, BorderLayout.CENTER);
            return panel;
        }

        List<EverythingUtil.SearchResult> results = searcher.search("【 】", path);

        java.util.Map<String, Integer> tagCount = new java.util.LinkedHashMap<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("【([^】]+)】");
        for (EverythingUtil.SearchResult result : results) {
            String fileName = result.getFileName();
            java.util.regex.Matcher matcher = pattern.matcher(fileName);
            while (matcher.find()) {
                String tag = matcher.group(1);
                tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
            }
        }

        ConfigUtil.reload();

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new WrapLayout());
        contentPanel.setBackground(BG_CONTENT);

        List<BadgeToggleButton> toggleButtons = new ArrayList<>();

        if (tagCount.isEmpty()) {
            JLabel emptyLabel = new JLabel("未找到任何标签");
            emptyLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(102, 102, 102));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(emptyLabel);
        } else {
            List<java.util.Map.Entry<String, Integer>> sortedTags = new ArrayList<>(tagCount.entrySet());
            sortedTags.sort(java.util.Map.Entry.comparingByValue());

            for (java.util.Map.Entry<String, Integer> entry : sortedTags) {
                String tag = entry.getKey();
                int count = entry.getValue();

                BadgeToggleButton toggleButton = new BadgeToggleButton(tag);
                toggleButton.setBadgeNumber(count);
                toggleButton.setBadgeColor(BG_MAIN, TEXT_DARK);
                toggleButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
                toggleButton.setFocusPainted(false);
                toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                toggleButtons.add(toggleButton);

                toggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            for (JToggleButton btn : toggleButtons) {
                                if (btn != toggleButton && btn.isSelected()) {
                                    btn.setSelected(false);
                                }
                            }
                            String searchQuery = path + " 【" + tag + "】";
                            EverythingUtil.launchEverythingUI(searchQuery, Config.everythingPath);
                        }
                    }
                });

                contentPanel.add(toggleButton);
            }
        }

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(BG_CONTENT);
        TitledBorder border = BorderFactory.createTitledBorder("本页标签统计");
        scrollPane.setBorder(border);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton searchButton = new JButton("搜索选中的标签");
        searchButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        searchButton.setPreferredSize(new Dimension(searchButton.getPreferredSize().width + 20, 36));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> {
            List<String> selectedTags = new ArrayList<>();
            for (BadgeToggleButton toggleButton : toggleButtons) {
                if (toggleButton.isSelected()) {
                    String tagText = toggleButton.getText().trim(); // 去除空格
                    selectedTags.add(tagText);
                }
            }
            if (!selectedTags.isEmpty()) {
                StringBuilder queryBuilder = new StringBuilder(path);
                for (String tag : selectedTags) {
                    queryBuilder.append(" 【").append(tag).append("】");
                }
                EverythingUtil.launchEverythingUI(queryBuilder.toString(), Config.everythingPath);
            }
        });

        JButton refreshButton = new JButton("刷新");
        refreshButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        refreshButton.setPreferredSize(new Dimension(refreshButton.getPreferredSize().width + 20, 36));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshAction.run());

        buttonPanel.add(searchButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建添加标签面板。
     * <p>
     * 布局采用嵌套 JSplitPane 实现三个可拖拽区域：
     * 左侧为历史标签列表，右侧上部为当前目录文件列表，右侧下部为自定义标签输入框。
     * 支持双击历史标签直接为选中文件添加标签，也支持多选历史标签后点击按钮批量添加。
     * </p>
     *
     * @param path         目录路径
     * @param refreshAction 刷新回调
     * @return 添加标签面板
     */
    private static JPanel createAddTagTab(String path, Runnable refreshAction) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(BG_CONTENT);

        // 被选中的文件列表，由文件按钮的选中状态同步维护
        final List<File> selectedFiles = new ArrayList<>();
        // 所有历史标签按钮，用于双击和多选操作
        final List<JToggleButton> historyTagButtons = new ArrayList<>();

        // ==================== 左侧：历史标签模块 ====================
        ConfigUtil.reload();
        List<String> history = new ArrayList<>();
        for (String tag : Config.tags) {
            if (!FileUtil.TAG_ORDER_FILENAME.equals(tag) && !FileUtil.TAG_ORDER_VERSION.equals(tag)) {
                history.add(tag);
            }
        }
        JPanel tagsPanel = new JPanel();
        tagsPanel.setLayout(new BoxLayout(tagsPanel, BoxLayout.Y_AXIS));
        tagsPanel.setBackground(BG_CONTENT);
        for (String tag : history) {
            JToggleButton tagButton = new JToggleButton(tag);
            tagButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            tagButton.setFocusPainted(false);
            tagButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            tagButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            tagButton.setHorizontalAlignment(SwingConstants.CENTER);
            tagButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, tagButton.getPreferredSize().height));

            // 双击历史标签：立即将该标签添加到所有已选中的文件
            tagButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                        if (selectedFiles.isEmpty()) {
                            showMessage("请先在右侧选择要添加标签的文件");
                            return;
                        }
                        applyTagToSelectedFiles(path, selectedFiles, List.of(tag), refreshAction);
                    }
                }
            });

            historyTagButtons.add(tagButton);
            tagsPanel.add(tagButton);
            tagsPanel.add(Box.createVerticalStrut(5));
        }

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_CONTENT);
        tagsScroll.setBorder(BorderFactory.createTitledBorder("历史标签"));

        // ==================== 右侧上部：当前目录文件模块 ====================
        File currentDir = new File(path);
        File[] files = currentDir.listFiles();

        JPanel filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        filesPanel.setBackground(BG_CONTENT);

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    final File currentFile = file;
                    // 使用 HTML 实现文字左对齐
                    JToggleButton fileButton = new JToggleButton("<html><div style='text-align:left;width:100%;'>" + file.getName() + "</div></html>");
                    fileButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
                    fileButton.setFocusPainted(false);
                    fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    fileButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                    fileButton.setHorizontalAlignment(SwingConstants.LEFT);
                    fileButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, fileButton.getPreferredSize().height));

                    fileButton.addItemListener(e -> {
                        if (fileButton.isSelected()) {
                            selectedFiles.add(currentFile);
                        } else {
                            selectedFiles.remove(currentFile);
                        }
                    });

                    filesPanel.add(fileButton);
                    filesPanel.add(Box.createVerticalStrut(5));
                }
            }
        }

        JScrollPane filesScroll = new JScrollPane(filesPanel);
        filesScroll.setBackground(BG_CONTENT);
        filesScroll.setBorder(BorderFactory.createTitledBorder("当前目录文件"));

        // ==================== 右侧下部：自定义标签输入模块 ====================
        String placeholderText = "输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈";
        JTextArea input = new JTextArea();
        input.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setText(placeholderText);
        input.setForeground(new Color(160, 160, 160));
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent event) {
                if (input.getText().equals(placeholderText)) {
                    input.setText("");
                    input.setForeground(TEXT_DARK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                if (input.getText().isEmpty()) {
                    input.setText(placeholderText);
                    input.setForeground(new Color(160, 160, 160));
                }
            }
        });

        // 自定义标签输入区域用 TitledBorder 包裹，与历史标签和文件列表保持一致
        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setBackground(BG_CONTENT);
        inputScroll.setBorder(BorderFactory.createTitledBorder("自定义标签"));
        inputScroll.setPreferredSize(new Dimension(400, 120));

        // ==================== 嵌套 JSplitPane 实现三区域可拖拽 ====================
        // 右侧上下分隔：文件列表 / 自定义标签输入
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filesScroll, inputScroll);
        rightSplit.setResizeWeight(0.7);
        rightSplit.setBackground(BG_CONTENT);
        rightSplit.setBorder(null);

        // 左右分隔：历史标签 / (文件列表 + 自定义标签)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tagsScroll, rightSplit);
        mainSplit.setResizeWeight(0.3);
        mainSplit.setBackground(BG_CONTENT);
        mainSplit.setBorder(null);

        // 从配置恢复分隔线位置
        ConfigUtil.reload();
        if (Config.addTagHorizontalDivider > 0) {
            mainSplit.setDividerLocation(Config.addTagHorizontalDivider);
        } else {
            mainSplit.setDividerLocation(0.3);
        }
        if (Config.addTagVerticalDivider > 0) {
            rightSplit.setDividerLocation(Config.addTagVerticalDivider);
        } else {
            rightSplit.setDividerLocation(0.7);
        }

        // 拖拽分隔线时自动保存位置到配置
        mainSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            Config.addTagHorizontalDivider = mainSplit.getDividerLocation();
            ConfigUtil.save();
        });
        rightSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            Config.addTagVerticalDivider = rightSplit.getDividerLocation();
            ConfigUtil.save();
        });

        panel.add(mainSplit, BorderLayout.CENTER);

        // ==================== 底部按钮区 ====================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton addButton = new JButton("添加标签");
        addButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        addButton.setPreferredSize(new Dimension(addButton.getPreferredSize().width + 20, 36));
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> {
            // 收集自定义输入的标签
            String typed = input.getText();
            if (typed.equals(placeholderText)) {
                typed = "";
            }
            List<String> customTags = splitTags(typed);

            // 收集多选的历史标签
            List<String> selectedHistoryTags = new ArrayList<>();
            for (JToggleButton btn : historyTagButtons) {
                if (btn.isSelected()) {
                    selectedHistoryTags.add(btn.getText().trim());
                }
            }

            // 合并自定义标签和选中的历史标签
            List<String> allTags = new ArrayList<>();
            allTags.addAll(selectedHistoryTags);
            allTags.addAll(customTags);

            if (allTags.isEmpty()) {
                showMessage("请输入或选择要添加的标签");
                return;
            }

            if (selectedFiles.isEmpty()) {
                showMessage("请先选择要添加标签的文件");
                return;
            }

            applyTagToSelectedFiles(path, selectedFiles, allTags, refreshAction);
        });

        JButton addRefreshButton = new JButton("刷新");
        addRefreshButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        addRefreshButton.setPreferredSize(new Dimension(addRefreshButton.getPreferredSize().width + 20, 36));
        addRefreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addRefreshButton.setFocusPainted(false);
        addRefreshButton.addActionListener(e -> refreshAction.run());

        buttonPanel.add(addButton);
        buttonPanel.add(addRefreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 将指定标签添加到选中的文件，并刷新当前标签页。
     *
     * @param path          当前目录路径
     * @param selectedFiles 已选中的文件列表
     * @param tags          要添加的标签列表
     * @param refreshAction 刷新回调
     */
    private static void applyTagToSelectedFiles(String path, List<File> selectedFiles,
                                                List<String> tags, Runnable refreshAction) {
        int renamed = 0;
        List<String> failedFiles = new ArrayList<>();
        for (File file : selectedFiles) {
            try {
                Path filePath = file.toPath();
                if (FileUtil.addTagsToNamePrefix(filePath, tags)) {
                    renamed++;
                }
            } catch (Exception ex) {
                failedFiles.add(file.getName());
            }
        }
        if (!failedFiles.isEmpty()) {
            showError("以下文件添加标签失败：\n" + String.join("\n", failedFiles));
        }
        showSuccess("成功为 " + renamed + " 个文件添加标签");
        TagUtil.rememberTags(tags, new HashSet<>());
        refreshAction.run();
    }

    /**
     * 创建移除标签面板。
     * <p>
     * 采用 JSplitPane 实现左右两区域可拖拽：
     * 左侧为当前目录中已有标签的文件所含标签列表，右侧为有标签的文件列表。
     * 支持双击标签直接从选中文件中移除，也支持多选标签后点击按钮批量移除。
     * </p>
     *
     * @param path         目录路径
     * @param refreshAction 刷新回调
     * @return 移除标签面板
     */
    private static JPanel createRemoveTagTab(String path, Runnable refreshAction) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(BG_CONTENT);

        // 被选中的文件列表，由文件按钮的选中状态同步维护
        final List<File> selectedFiles = new ArrayList<>();
        // 所有标签按钮，用于双击和多选操作
        final List<JToggleButton> tagButtons = new ArrayList<>();

        // ==================== 扫描有标签的文件及其标签 ====================
        Set<String> existingFileTags = new LinkedHashSet<>();
        List<File> taggedFiles = new ArrayList<>();
        File currentDir = new File(path);
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    List<String> tags = FileUtil.parseAllTags(file.getName());
                    if (!tags.isEmpty()) {
                        taggedFiles.add(file);
                        existingFileTags.addAll(tags);
                    }
                }
            }
        }

        // ==================== 左侧：文件标签模块 ====================
        JPanel tagsPanel = new JPanel();
        tagsPanel.setLayout(new BoxLayout(tagsPanel, BoxLayout.Y_AXIS));
        tagsPanel.setBackground(BG_CONTENT);
        for (String tag : existingFileTags) {
            JToggleButton tagButton = new JToggleButton(tag);
            tagButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            tagButton.setFocusPainted(false);
            tagButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            tagButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            tagButton.setHorizontalAlignment(SwingConstants.CENTER);
            tagButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, tagButton.getPreferredSize().height));

            // 双击标签：立即将该标签从所有已选中的文件中移除
            tagButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                        if (selectedFiles.isEmpty()) {
                            showMessage("请先在右侧选择要移除标签的文件");
                            return;
                        }
                        performRemove(path, selectedFiles, Set.of(tag), refreshAction);
                    }
                }
            });

            tagButtons.add(tagButton);
            tagsPanel.add(tagButton);
            tagsPanel.add(Box.createVerticalStrut(5));
        }

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_CONTENT);
        tagsScroll.setBorder(BorderFactory.createTitledBorder("文件标签"));

        // ==================== 右侧：有标签的文件列表模块 ====================
        JPanel filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        filesPanel.setBackground(BG_CONTENT);

        for (File file : taggedFiles) {
            final File currentFile = file;
            JToggleButton fileButton = new JToggleButton("<html><div style='text-align:left;width:100%;'>" + file.getName() + "</div></html>");
            fileButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            fileButton.setFocusPainted(false);
            fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            fileButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            fileButton.setHorizontalAlignment(SwingConstants.LEFT);
            fileButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, fileButton.getPreferredSize().height));

            fileButton.addItemListener(e -> {
                if (fileButton.isSelected()) {
                    selectedFiles.add(currentFile);
                } else {
                    selectedFiles.remove(currentFile);
                }
            });

            filesPanel.add(fileButton);
            filesPanel.add(Box.createVerticalStrut(5));
        }

        JScrollPane filesScroll = new JScrollPane(filesPanel);
        filesScroll.setBackground(BG_CONTENT);
        filesScroll.setBorder(BorderFactory.createTitledBorder("有标签的文件"));

        // ==================== JSplitPane 实现左右可拖拽 ====================
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tagsScroll, filesScroll);
        mainSplit.setResizeWeight(0.3);
        mainSplit.setBackground(BG_CONTENT);
        mainSplit.setBorder(null);

        // 从配置恢复分隔线位置
        ConfigUtil.reload();
        if (Config.addTagHorizontalDivider > 0) {
            mainSplit.setDividerLocation(Config.addTagHorizontalDivider);
        } else {
            mainSplit.setDividerLocation(0.3);
        }

        // 拖拽分隔线时自动保存位置到配置
        mainSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            Config.addTagHorizontalDivider = mainSplit.getDividerLocation();
            ConfigUtil.save();
        });

        panel.add(mainSplit, BorderLayout.CENTER);

        // ==================== 底部按钮区 ====================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton removeButton = new JButton("移除标签");
        removeButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        removeButton.setPreferredSize(new Dimension(removeButton.getPreferredSize().width + 20, 36));
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.setFocusPainted(false);
        removeButton.addActionListener(e -> {
            // 收集多选的标签
            Set<String> selectedTags = new LinkedHashSet<>();
            for (JToggleButton btn : tagButtons) {
                if (btn.isSelected()) {
                    selectedTags.add(btn.getText().trim());
                }
            }

            if (selectedTags.isEmpty()) {
                showMessage("请先选择要移除的标签");
                return;
            }

            if (selectedFiles.isEmpty()) {
                showMessage("请先选择要移除标签的文件");
                return;
            }

            performRemove(path, selectedFiles, selectedTags, refreshAction);
        });

        JButton removeRefreshButton = new JButton("刷新");
        removeRefreshButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        removeRefreshButton.setPreferredSize(new Dimension(removeRefreshButton.getPreferredSize().width + 20, 36));
        removeRefreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeRefreshButton.setFocusPainted(false);
        removeRefreshButton.addActionListener(e -> refreshAction.run());

        buttonPanel.add(removeButton);
        buttonPanel.add(removeRefreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建设置面板。
     * <p>
     * 包含 Everything 工具路径配置和历史标签拖拽排序功能。
     * 支持保存配置和按标签顺序重命名目录中的文件。
     * </p>
     *
     * @param path         目录路径
     * @param refreshAction 刷新回调
     * @return 设置面板
     */
    private static JPanel createSettingsTab(String path, Runnable refreshAction) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(BG_CONTENT);

        ConfigUtil.reload();

        // ==================== 上部：Everything 路径配置 ====================
        JPanel pathPanel = new JPanel(new BorderLayout(8, 0));
        pathPanel.setBackground(BG_CONTENT);
        pathPanel.setBorder(BorderFactory.createTitledBorder("Everything 工具路径"));

        JTextField pathField = new JTextField(Config.everythingPath);
        pathField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        pathField.setPreferredSize(new Dimension(400, 32));
        pathPanel.add(pathField, BorderLayout.CENTER);

        // ==================== 中部：标签排序 ====================
        JPanel tagsContainer = new JPanel(new BorderLayout());
        tagsContainer.setBackground(BG_CONTENT);
        tagsContainer.setBorder(BorderFactory.createTitledBorder("标签排序（拖拽调整）"));

        DefaultListModel<String> tagListModel = new DefaultListModel<>();
        boolean hasFilename = false;
        boolean hasVersion = false;
        for (String tag : Config.tags) {
            tagListModel.addElement(tag);
            if (FileUtil.TAG_ORDER_FILENAME.equals(tag)) {
                hasFilename = true;
            }
            if (FileUtil.TAG_ORDER_VERSION.equals(tag)) {
                hasVersion = true;
            }
        }
        // 首次使用时，添加默认的特殊占位项
        if (!hasFilename) {
            tagListModel.add(0, FileUtil.TAG_ORDER_FILENAME);
        }
        if (!hasVersion) {
            int filenameIndex = tagListModel.indexOf(FileUtil.TAG_ORDER_FILENAME);
            tagListModel.add(filenameIndex + 1, FileUtil.TAG_ORDER_VERSION);
        }

        JList<String> tagJList = new JList<>(tagListModel);
        tagJList.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        tagJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tagJList.setCellRenderer(new TagListCellRenderer());
        tagJList.setDragEnabled(true);
        tagJList.setDropMode(DropMode.INSERT);
        tagJList.setTransferHandler(new TagListTransferHandler(tagListModel));
        tagJList.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JScrollPane tagsScroll = new JScrollPane(tagJList);
        tagsScroll.setBackground(BG_CONTENT);
        tagsContainer.add(tagsScroll, BorderLayout.CENTER);

        // 上下布局：路径 + 标签排序
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(BG_CONTENT);
        pathPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathPanel.getPreferredSize().height + 10));
        centerPanel.add(pathPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(tagsContainer);

        panel.add(centerPanel, BorderLayout.CENTER);

        // ==================== 底部按钮区 ====================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton saveButton = new JButton("保存");
        saveButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        saveButton.setPreferredSize(new Dimension(saveButton.getPreferredSize().width + 20, 36));
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> {
            Config.everythingPath = pathField.getText().trim();
            // 保存完整排序顺序（含"文件名"和"版本号"占位符）
            Config.tags = Collections.list(tagListModel.elements());
            ConfigUtil.save();
            showSuccess("配置已保存");
        });

        JButton reorderButton = new JButton("重排此目录标签顺序");
        reorderButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        reorderButton.setPreferredSize(new Dimension(reorderButton.getPreferredSize().width + 20, 36));
        reorderButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reorderButton.setFocusPainted(false);
        reorderButton.addActionListener(e -> {
            // 先保存当前配置（含完整排序顺序）
            Config.everythingPath = pathField.getText().trim();
            Config.tags = Collections.list(tagListModel.elements());
            ConfigUtil.save();
            // 执行重排
            reorderDirectoryTags(path);
        });

        JButton rescanButton = new JButton("重新扫描历史标签");
        rescanButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        rescanButton.setPreferredSize(new Dimension(rescanButton.getPreferredSize().width + 20, 36));
        rescanButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rescanButton.setFocusPainted(false);
        rescanButton.addActionListener(e -> {
            // 扫描当前目录所有文件的标签，排除版本号标签
            LinkedHashSet<String> scannedTags = new LinkedHashSet<>();
            File currentDir = new File(path);
            File[] files = currentDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        for (String tag : FileUtil.parseAllTags(file.getName())) {
                            // 排除版本号标签
                            if (!FileUtil.VERSION_TAG_PATTERN.matcher(tag).matches()) {
                                scannedTags.add(tag);
                            }
                        }
                    }
                }
            }

            // 保留特殊占位项，重建列表模型
            tagListModel.clear();
            tagListModel.addElement(FileUtil.TAG_ORDER_FILENAME);
            tagListModel.addElement(FileUtil.TAG_ORDER_VERSION);
            for (String tag : scannedTags) {
                tagListModel.addElement(tag);
            }

            // 保存到配置
            Config.everythingPath = pathField.getText().trim();
            Config.tags = Collections.list(tagListModel.elements());
            ConfigUtil.save();
            showSuccess("已扫描到 " + scannedTags.size() + " 个标签");
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(rescanButton);
        buttonPanel.add(reorderButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 按照 Config.tags 的全局顺序重排目录中所有文件的标签顺序。
     *
     * @param dirPath 目录路径
     */
    private static void reorderDirectoryTags(String dirPath) {
        File currentDir = new File(dirPath);
        File[] files = currentDir.listFiles();
        if (files == null) {
            showMessage("目录为空或无法访问");
            return;
        }

        int reordered = 0;
        for (File file : files) {
            if (file.isFile()) {
                try {
                    if (FileUtil.reorderTags(file.toPath())) {
                        reordered++;
                    }
                } catch (Exception e) {
                    // 跳过无法重命名的文件
                }
            }
        }
        showSuccess("已重排 " + reordered + " 个文件的标签顺序");
    }

    /**
     * 标签列表单元格渲染器，为普通标签和特殊占位项提供不同的样式。
     */
    private static class TagListCellRenderer extends DefaultListCellRenderer {
        private static final Color SPECIAL_BG = new Color(255, 248, 225);
        private static final Color SPECIAL_FG = new Color(180, 130, 0);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GRAY),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));

            String text = value != null ? value.toString() : "";
            boolean isSpecial = FileUtil.TAG_ORDER_FILENAME.equals(text) || FileUtil.TAG_ORDER_VERSION.equals(text);

            if (isSelected) {
                // 选中状态保持系统默认
            } else if (isSpecial) {
                label.setBackground(SPECIAL_BG);
                label.setForeground(SPECIAL_FG);
                // 为特殊项添加描述
                if (FileUtil.TAG_ORDER_FILENAME.equals(text)) {
                    label.setText("{文件名}  —  源文件名在标签序列中的位置");
                } else if (FileUtil.TAG_ORDER_VERSION.equals(text)) {
                    label.setText("{版本号}  —  版本号（如 V1、V2）在标签序列中的位置");
                }
            } else {
                label.setBackground(BG_WHITE);
                label.setForeground(TEXT_DARK);
            }
            return label;
        }
    }

    /**
     * 标签列表拖拽传输处理器，支持在 JList 内部拖拽重排序。
     */
    private static class TagListTransferHandler extends TransferHandler {
        private final DefaultListModel<String> model;
        private int dragIndex = -1;

        TagListTransferHandler(DefaultListModel<String> model) {
            this.model = model;
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            JList<?> list = (JList<?>) component;
            dragIndex = list.getSelectedIndex();
            String value = list.getSelectedValue().toString();
            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{DataFlavor.stringFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return DataFlavor.stringFlavor.equals(flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor) {
                    return value;
                }
            };
        }

        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
            int dropIndex = dropLocation.getIndex();

            try {
                String draggedItem = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                // 移除原位置的元素
                if (dragIndex >= 0 && dragIndex < model.size()) {
                    model.remove(dragIndex);
                    // 调整插入位置
                    if (dropIndex > dragIndex) {
                        dropIndex--;
                    }
                }
                // 插入到新位置
                model.add(dropIndex, draggedItem);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        protected void exportDone(JComponent component, Transferable data, int action) {
            dragIndex = -1;
        }
    }

    /**
     *
     * @param path          当前目录路径
     * @param selectedFiles 已选中的文件列表
     * @param tagsToRemove  要移除的标签集合
     * @param refreshAction 刷新回调
     */
    private static void performRemove(String path, List<File> selectedFiles,
                                      Set<String> tagsToRemove, Runnable refreshAction) {
        if (tagsToRemove.isEmpty()) {
            showMessage("请先选择要移除的标签");
            return;
        }

        int renamed = 0;
        List<String> failedFiles = new ArrayList<>();
        for (File file : selectedFiles) {
            try {
                Path filePath = file.toPath();
                if (FileUtil.removeTags(filePath, tagsToRemove)) {
                    renamed++;
                }
            } catch (Exception e) {
                failedFiles.add(file.getName());
            }
        }
        if (!failedFiles.isEmpty()) {
            showError("以下文件移除标签失败：\n" + String.join("\n", failedFiles));
        }

        showSuccess("成功从 " + renamed + " 个文件中移除标签");
        refreshAction.run();
    }

    /**
     * 创建智能标签面板
     *
     * @param icon      图标
     * @param title     标题
     * @param value     值
     * @param tagSupplier 标签提供者
     * @return 智能标签面板
     */
    private static JPanel createSmartTagPanel(String icon, String title, String value, java.util.function.Supplier<String> tagSupplier) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(187, 222, 251));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(180, 80));
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        valueLabel.setForeground(new Color(70, 70, 70));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(valueLabel);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return panel;
    }

    /**
     * 保存窗口位置
     *
     * @param window 窗口
     */
    private static void saveWindowPosition(Window window) {
        Config.groupTagsWindowX = window.getX();
        Config.groupTagsWindowY = window.getY();
        Config.groupTagsWindowWidth = window.getWidth();
        Config.groupTagsWindowHeight = window.getHeight();
        ConfigUtil.save();
    }

    /**
     * 样式化主要按钮
     *
     * @param b 按钮
     */
    private static void stylePrimaryButton(AbstractButton b) {
        b.setForeground(TEXT_LIGHT);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(120, 44));
        b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        b.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
    }

    /**
     * 样式化次要按钮
     *
     * @param b 按钮
     */
    private static void styleSecondaryButton(AbstractButton b) {
        b.setBackground(BG_WHITE);
        b.setForeground(GRADIENT_START);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setBorder(BorderFactory.createLineBorder(GRADIENT_START, 2, true));
        b.setPreferredSize(new Dimension(120, 44));
        b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GRADIENT_START, 2, true), BorderFactory.createEmptyBorder(10, 24, 10, 24)));
    }

    /**
     * 分割标签字符串
     *
     * @param typed 输入的标签字符串
     * @return 标签列表
     */
    private static List<String> splitTags(String typed) {
        if (typed == null) return new ArrayList<>();
        String s = typed.trim();
        if (s.isEmpty()) return new ArrayList<>();
        String[] parts = s.split("\\s+");
        return new ArrayList<>(java.util.Arrays.asList(parts));
    }

    /**
     * 解析文件名开头的标签
     *
     * @param name 文件名
     * @return 标签列表
     */
    private static List<String> parseLeadingTags(String name) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < name.length() && name.charAt(i) == '【') {
            int end = name.indexOf('】', i + 1);
            if (end < 0) break;
            String inner = name.substring(i + 1, end).trim();
            if (!inner.isEmpty()) out.add(inner);
            i = end + 1;
        }
        return out;
    }
}