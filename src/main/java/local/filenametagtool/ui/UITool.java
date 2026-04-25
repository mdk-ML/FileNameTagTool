package local.filenametagtool.ui;

import local.filenametagtool.model.Config;
import local.filenametagtool.component.BadgeToggleButton;
import local.filenametagtool.component.WrapLayout;
import local.filenametagtool.util.ConfigUtil;
import local.filenametagtool.util.EverythingUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class UITool {

    private static final Color GRADIENT_START = new Color(74, 144, 226);
    private static final Color BG_LIGHT = new Color(248, 249, 250);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BG_CONTENT = new Color(249, 249, 249);
    private static final Color BG_MAIN = new Color(240, 240, 240);
    private static final Color BORDER_GRAY = new Color(220, 220, 220);
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color TEXT_LIGHT = Color.WHITE;

    /**
     * 显示消息弹窗
     *
     * @param msg   消息内容
     * @param title 标题
     */
    public static void showMessage(String msg, String title) {
        final JWindow window = new JWindow();
        window.setAlwaysOnTop(true);
        window.setSize(360, 160);
        window.setLocationRelativeTo(null);
        window.setOpacity(0.95f);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        titleLabel.setForeground(GRADIENT_START);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msgLabel = new JLabel("<html><body style='text-align:center;'>" + msg.replace("\n", "<br>") + "</body></html>");
        msgLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        msgLabel.setForeground(TEXT_DARK);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(msgLabel);

        window.getContentPane().add(panel);
        window.setVisible(true);

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SwingUtilities.invokeLater(window::dispose);
        }).start();
    }

    /**
     * 询问用户要添加的标签，支持历史标签和智能标签
     *
     * @return 包含标签列表和智能标签集合的数组
     */
    public static Object[] askTagsWithHistory() {
        Config cfg = ConfigUtil.reload();
        List<String> history = new ArrayList<>(cfg.getTags());

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
                    if (currentText.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈") || currentText.trim().isEmpty()) {
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
        JPanel dateTagPanel = createSmartTagPanel("📅", "当前日期", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                () -> {
                    String dateTag = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                    smartTags.add(dateTag);
                    return dateTag;
                });
        smartTagsPanel.add(dateTagPanel);

        // 最近使用标签
        JPanel recentTagPanel = createSmartTagPanel("🔄", "最近使用", "工作, 重要",
                () -> {
                    smartTags.add("工作");
                    smartTags.add("重要");
                    return "工作 重要";
                });
        smartTagsPanel.add(recentTagPanel);

        // 热门标签
        JPanel hotTagPanel = createSmartTagPanel("🔥", "热门标签", "项目A 会议",
                () -> {
                    smartTags.add("项目A");
                    smartTags.add("会议");
                    return "项目A 会议";
                });
        smartTagsPanel.add(hotTagPanel);

        // 推荐标签
        JPanel recommendTagPanel = createSmartTagPanel("💡", "推荐标签", "文档 计划",
                () -> {
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

        if (cfg.getWindowW() > 0 && cfg.getWindowH() > 0) {
            dialog.setBounds(cfg.getWindowX(), cfg.getWindowY(), cfg.getWindowW(), cfg.getWindowH());
        } else {
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setSize(new Dimension(840, 680));
        }
        if (cfg.getDivider() > 0) split.setDividerLocation(cfg.getDivider());
        else split.setDividerLocation(0.5);

        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            input.selectAll();
        });

        cfg.setWindowX(dialog.getX());
        cfg.setWindowY(dialog.getY());
        cfg.setWindowW(dialog.getWidth());
        cfg.setWindowH(dialog.getHeight());
        cfg.setDivider(split.getDividerLocation());
        ConfigUtil.saveFromConfig(cfg, "FileNameTagTool config (auto-generated)");

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
                List<String> tags = parseLeadingTags(name);
                existingFileTags.addAll(tags);
            }
        }

        if (existingFileTags.isEmpty()) {
            showMessage("所选文件没有标签可移除。", "提示");
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

        JButton remove = new JButton("移除选中标签");
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
     * 创建标签管理窗口
     *
     * @param paths 文件路径列表
     */
    public static void createTagManagerWindow(List<java.nio.file.Path> paths) {
        Config cfg = ConfigUtil.reload();
        String currentPath = paths.get(0).toString();

        JFrame frame = new JFrame("文件标签管理 " + currentPath);
        List<Image> icons = new ArrayList<>();
        try {
            String iconBasePath = cfg.getIconPath();
            icons.add(new ImageIcon(iconBasePath + "tags-16.png").getImage());
            icons.add(new ImageIcon(iconBasePath + "tags-32.png").getImage());
            icons.add(new ImageIcon(iconBasePath + "tags-48.png").getImage());
            icons.add(new ImageIcon(iconBasePath + "tags-64.png").getImage());
            frame.setIconImages(icons);
        } catch (Exception e) {
            System.err.println("Failed to load icons: " + e.getMessage());
        }
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(true);

        if (cfg.getGroupTagsWindowWidth() > 0 && cfg.getGroupTagsWindowHeight() > 0) {
            frame.setSize(cfg.getGroupTagsWindowWidth(), cfg.getGroupTagsWindowHeight());
        } else {
            frame.setSize(800, 600);
        }

        if (cfg.getGroupTagsWindowX() > 0 && cfg.getGroupTagsWindowY() > 0) {
            frame.setLocation(cfg.getGroupTagsWindowX(), cfg.getGroupTagsWindowY());
        } else {
            frame.setLocationRelativeTo(null);
        }

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        tabbedPane.addTab("搜索", null, createSearchTab(paths, frame));
        tabbedPane.addTab("添加标签", null, createAddTagTab(paths, frame));
        tabbedPane.addTab("移除标签", null, createRemoveTagTab(paths, frame));

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
     * 创建搜索标签面板
     *
     * @param paths      文件路径列表
     * @param parentFrame 父窗口
     * @return 搜索面板
     */
    private static JPanel createSearchTab(List<java.nio.file.Path> paths, JFrame parentFrame) {
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

        List<EverythingUtil.SearchResult> results = searcher.search("【 】", paths.get(0).toString());

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

        String currentPath = paths.get(0).toString();
        Config cfg = ConfigUtil.reload();

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 4, 4));
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
                            String searchQuery = currentPath + " 【" + tag + "】";
                            try {
                                EverythingUtil.launchEverythingUI(searchQuery, cfg.getEverythingPath());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
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
                    String tagText = toggleButton.getText();
                    if (tagText.contains(" (")) {
                        String tag = tagText.substring(0, tagText.indexOf(" "));
                        selectedTags.add(tag);
                    }
                }
            }
            if (!selectedTags.isEmpty()) {
                StringBuilder queryBuilder = new StringBuilder(currentPath);
                for (String tag : selectedTags) {
                    queryBuilder.append(" 【").append(tag).append("】");
                }
                try {
                    EverythingUtil.launchEverythingUI(queryBuilder.toString(), cfg.getEverythingPath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JButton refreshButton = new JButton("刷新");
        refreshButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        refreshButton.setPreferredSize(new Dimension(refreshButton.getPreferredSize().width + 20, 36));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> {
            parentFrame.dispose();
            createTagManagerWindow(paths);
        });

        buttonPanel.add(searchButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建添加标签面板
     *
     * @param paths      文件路径列表
     * @param parentFrame 父窗口
     * @return 添加标签面板
     */
    private static JPanel createAddTagTab(List<java.nio.file.Path> paths, JFrame parentFrame) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(BG_CONTENT);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_CONTENT);

        Config appConfig = ConfigUtil.reload();
        List<String> history = new ArrayList<>(appConfig.getTags());

        JPanel tagsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        tagsPanel.setBackground(BG_CONTENT);
        for (String tag : history) {
            JToggleButton b = new JToggleButton(tag);
            b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            tagsPanel.add(b);
        }

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_CONTENT);
        tagsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        tagsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        TitledBorder historyBorder = BorderFactory.createTitledBorder("历史标签");
        tagsScroll.setBorder(historyBorder);
        contentPanel.add(tagsScroll);

        JTextArea input = new JTextArea();
        input.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        input.setPreferredSize(new Dimension(400, 80));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setText("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈");
        input.setForeground(new Color(160, 160, 160));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        contentPanel.add(input);

        panel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton addButton = new JButton("添加标签");
        addButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        addButton.setPreferredSize(new Dimension(addButton.getPreferredSize().width + 20, 36));
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.setFocusPainted(false);
        addButton.setForeground(TEXT_LIGHT);
        addButton.setBackground(GRADIENT_START);
        addButton.setOpaque(true);
        addButton.setBorderPainted(false);
        addButton.addActionListener(e -> {
            String typed = input.getText();
            if (typed.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈")) {
                typed = "";
            }
            List<String> tags = splitTags(typed);
            if (tags.isEmpty()) {
                showMessage("请输入有效的标签", "提示");
                return;
            }

            int renamed = 0;
            for (java.nio.file.Path p : paths) {
                try {
                    if (FileOperation.addTagsToNamePrefix(p, tags)) {
                        renamed++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            showMessage("成功为 " + renamed + " 个文件添加标签", "完成");
            TagManager.rememberTags(tags, new HashSet<>());
            parentFrame.dispose();
            createTagManagerWindow(paths);
        });

        buttonPanel.add(addButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建移除标签面板
     *
     * @param paths      文件路径列表
     * @param parentFrame 父窗口
     * @return 移除标签面板
     */
    private static JPanel createRemoveTagTab(List<java.nio.file.Path> paths, JFrame parentFrame) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(BG_CONTENT);

        Set<String> existingFileTags = new LinkedHashSet<>();
        for (java.nio.file.Path file : paths) {
            java.nio.file.Path fileName = file.getFileName();
            if (fileName != null) {
                String name = fileName.toString();
                List<String> tags = parseLeadingTags(name);
                existingFileTags.addAll(tags);
            }
        }

        if (existingFileTags.isEmpty()) {
            JLabel emptyLabel = new JLabel("所选文件没有标签可移除");
            emptyLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(102, 102, 102));
            panel.add(emptyLabel, BorderLayout.CENTER);
            return panel;
        }

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_CONTENT);

        JLabel headerLabel = new JLabel("选择要移除的标签");
        headerLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        headerLabel.setForeground(TEXT_DARK);
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(headerLabel);
        contentPanel.add(Box.createVerticalStrut(15));

        final Set<String> tagsToRemove = new HashSet<>();
        final Color REMOVE_RED = new Color(244, 67, 54);

        JPanel tagsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
        tagsPanel.setBackground(BG_CONTENT);
        for (String tag : existingFileTags) {
            final String t = tag;
            JToggleButton b = new JToggleButton(tag);
            b.setBackground(BG_CONTENT);
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
                            b.setBackground(BG_CONTENT);
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
                        performRemove(paths, tagsToRemove, parentFrame);
                    }
                }
            });
            tagsPanel.add(b);
        }

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_CONTENT);
        TitledBorder tagsBorder = BorderFactory.createTitledBorder("文件标签");
        tagsScroll.setBorder(tagsBorder);
        contentPanel.add(tagsScroll);

        JScrollPane mainScroll = new JScrollPane(contentPanel);
        mainScroll.setBackground(BG_CONTENT);
        panel.add(mainScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton removeButton = new JButton("移除选中标签");
        removeButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        removeButton.setPreferredSize(new Dimension(removeButton.getPreferredSize().width + 20, 36));
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.setFocusPainted(false);
        removeButton.setForeground(TEXT_LIGHT);
        removeButton.setBackground(REMOVE_RED);
        removeButton.setOpaque(true);
        removeButton.setBorderPainted(false);
        removeButton.addActionListener(e -> performRemove(paths, tagsToRemove, parentFrame));

        buttonPanel.add(removeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 执行移除标签操作
     *
     * @param paths         文件路径列表
     * @param tagsToRemove  要移除的标签集合
     * @param parentFrame   父窗口
     */
    private static void performRemove(List<java.nio.file.Path> paths, Set<String> tagsToRemove, JFrame parentFrame) {
        if (tagsToRemove.isEmpty()) {
            showMessage("请先选择要移除的标签", "提示");
            return;
        }

        int renamed = 0;
        for (java.nio.file.Path p : paths) {
            try {
                if (FileOperation.removeTags(p, tagsToRemove)) {
                    renamed++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        showMessage("成功从 " + renamed + " 个文件中移除标签", "完成");
        parentFrame.dispose();
        createTagManagerWindow(paths);
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
        Config cfg = ConfigUtil.reload();
        cfg.setGroupTagsWindowX(window.getX());
        cfg.setGroupTagsWindowY(window.getY());
        cfg.setGroupTagsWindowWidth(window.getWidth());
        cfg.setGroupTagsWindowHeight(window.getHeight());
        ConfigUtil.saveFromConfig(cfg, "FileNameTagTool config (auto-generated)");
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