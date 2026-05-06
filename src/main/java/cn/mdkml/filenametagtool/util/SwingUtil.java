package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.component.BadgeToggleButton;
import cn.mdkml.filenametagtool.component.WrapLayout;
import cn.mdkml.filenametagtool.model.Config;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SwingUtil {

    /** 当前显示的通知弹窗，用于确保同一时间只显示一个 */
    private static volatile JDialog currentNotification;

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
            UIManager.put("SplitPane.background", BG_CONTENT);
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
    private enum MessageType {INFO, SUCCESS, ERROR}

    /**
     * 通知弹窗内部实现。
     *
     * @param msg 消息内容
     * @param title 标题
     * @param type 弹窗类型
     */
    private static void showNotification(String msg, String title, MessageType type) {
        boolean isError = type == MessageType.ERROR;

        // 关闭之前的通知弹窗
        if (currentNotification != null) {
            currentNotification.dispose();
            currentNotification = null;
        }

        JDialog dialog = new JDialog((Frame) null, title, false);
        currentNotification = dialog;
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

        // 弹窗关闭时清除引用
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (currentNotification == dialog) {
                    currentNotification = null;
                }
            }
        });

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
     * 创建标签管理窗口。
     *
     * @param path 目录路径
     */
    public static void createTagManagerWindow(String path) {
        createTagManagerWindow(path, -1, null);
    }

    /**
     * 创建标签管理窗口。
     *
     * @param path          目录路径
     * @param initialTab    初始选中的标签页索引（-1 使用默认）
     * @param filesToSelect 要自动选中的文件路径列表（null 表示不自动选中）
     */
    public static void createTagManagerWindow(String path, int initialTab, List<Path> filesToSelect) {
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
        tabbedPane.addTab("添加标签", null, createAddTagTab(path, refreshAddTag[0], filesToSelect));
        tabbedPane.addTab("移除标签", null, createRemoveTagTab(path, refreshRemoveTag[0], filesToSelect));
        tabbedPane.addTab("设置", null, createSettingsTab(path, refreshSettings[0]));

        // 设置初始标签页
        if (initialTab >= 0 && initialTab < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(initialTab);
        }

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

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowPosition(frame);
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                saveWindowPosition(frame);
            }

            @Override
            public void componentResized(ComponentEvent e) {
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

        Map<String, Integer> tagCount = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("【([^】]+)】");
        for (EverythingUtil.SearchResult result : results) {
            String fileName = result.getFileName();
            Matcher matcher = pattern.matcher(fileName);
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
            List<Map.Entry<String, Integer>> sortedTags = new ArrayList<>(tagCount.entrySet());
            sortedTags.sort(Map.Entry.comparingByValue());

            for (Map.Entry<String, Integer> entry : sortedTags) {
                String tag = entry.getKey();
                int count = entry.getValue();

                BadgeToggleButton toggleButton = new BadgeToggleButton(tag);
                toggleButton.setBadgeNumber(count);
                toggleButton.setBadgeColor(BG_MAIN, TEXT_DARK);
                toggleButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
                toggleButton.setFocusPainted(false);
                toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                toggleButtons.add(toggleButton);

                toggleButton.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
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
                    String tagText = toggleButton.getText().trim();
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
     *
     * @param path          目录路径
     * @param refreshAction 刷新回调
     * @return 添加标签面板
     */
    private static JPanel createAddTagTab(String path, Runnable refreshAction) {
        return createAddTagTab(path, refreshAction, null);
    }

    /**
     * 创建添加标签面板。
     *
     * @param path          目录路径
     * @param refreshAction 刷新回调
     * @param filesToSelect 要自动选中的文件路径列表（null 表示不自动选中）
     * @return 添加标签面板
     */
    private static JPanel createAddTagTab(String path, Runnable refreshAction, List<Path> filesToSelect) {
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
            if (FileUtil.TAG_ORDER_FILENAME.equals(tag)) {
                continue;
            }
            if (FileUtil.TAG_ORDER_VERSION.equals(tag)) {
                continue;
            }
            if (FileUtil.TAG_ORDER_DATE.equals(tag)) {
                continue;
            }
            history.add(tag);
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
                        applyTagToSelectedFiles(selectedFiles, List.of(tag), refreshAction);
                    }
                }
            });

            historyTagButtons.add(tagButton);
            tagsPanel.add(tagButton);
            tagsPanel.add(Box.createVerticalStrut(5));
        }
        enableRangeSelection(historyTagButtons);

        // ==================== 左侧上部：智能标签模块 ====================
        JPanel smartPanel = new JPanel();
        smartPanel.setLayout(new BoxLayout(smartPanel, BoxLayout.Y_AXIS));
        smartPanel.setBackground(BG_CONTENT);

        // 当前日期智能标签
        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JToggleButton dateButton = new JToggleButton("当前日期 (" + todayDate + ")");
        dateButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        dateButton.setFocusPainted(false);
        dateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateButton.setHorizontalAlignment(SwingConstants.CENTER);
        dateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, dateButton.getPreferredSize().height));

        // 双击日期标签：立即将该标签添加到所有已选中的文件
        dateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    if (selectedFiles.isEmpty()) {
                        showMessage("请先在右侧选择要添加标签的文件");
                        return;
                    }
                    applyTagToSelectedFiles(selectedFiles, List.of(todayDate), refreshAction);
                }
            }
        });

        smartPanel.add(dateButton);

        JScrollPane smartScroll = new JScrollPane(smartPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        smartScroll.setBackground(BG_CONTENT);
        smartScroll.setBorder(BorderFactory.createTitledBorder("智能标签"));
        smartScroll.setPreferredSize(new Dimension(0, 80));

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_CONTENT);
        tagsScroll.setBorder(BorderFactory.createTitledBorder("历史标签"));

        // 智能标签与历史标签垂直分隔
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, smartScroll, tagsScroll);
        leftSplit.setResizeWeight(0.2);
        leftSplit.setBackground(BG_CONTENT);
        leftSplit.setBorder(null);

        // ==================== 右侧上部：当前目录文件模块 ====================
        File currentDir = new File(path);
        File[] files = currentDir.listFiles();

        JPanel filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        filesPanel.setBackground(BG_CONTENT);

        final List<JToggleButton> fileButtons = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
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

                    fileButtons.add(fileButton);
                    filesPanel.add(fileButton);
                    filesPanel.add(Box.createVerticalStrut(5));
                }
            }
        }
        enableRangeSelection(fileButtons);

        // 自动选中指定的文件
        if (filesToSelect != null && !filesToSelect.isEmpty()) {
            Set<String> selectNames = new HashSet<>();
            for (Path p : filesToSelect) {
                Path fn = p.getFileName();
                if (fn != null) {
                    selectNames.add(fn.toString());
                }
            }
            for (int i = 0; i < fileButtons.size(); i++) {
                File f = files[i];
                if (f.isFile() && selectNames.contains(f.getName())) {
                    fileButtons.get(i).setSelected(true);
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
        input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setText(placeholderText);
        input.setForeground(new Color(160, 160, 160));
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (input.getText().equals(placeholderText)) {
                    input.setText("");
                    input.setForeground(TEXT_DARK);
                }
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (input.getText().isEmpty()) {
                    input.setText(placeholderText);
                    input.setForeground(new Color(160, 160, 160));
                }
            }
        });

        // 自定义标签输入区域用 TitledBorder 包裹，与历史标签和文件列表保持一致
        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setBackground(BG_CONTENT);
        inputScroll.setBorder(BorderFactory.createTitledBorder("自定义标签（多个标签用空格分隔）"));
        inputScroll.setPreferredSize(new Dimension(400, 120));

        // ==================== 嵌套 JSplitPane 实现三区域可拖拽 ====================
        // 右侧上下分隔：文件列表 / 自定义标签输入
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filesScroll, inputScroll);
        rightSplit.setResizeWeight(0.7);
        rightSplit.setBackground(BG_CONTENT);
        rightSplit.setBorder(null);

        // 左右分隔：历史标签 / (文件列表 + 自定义标签)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit);
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
        if (Config.addTagSmartHistoryDivider > 0) {
            leftSplit.setDividerLocation(Config.addTagSmartHistoryDivider);
        } else {
            leftSplit.setDividerLocation(0.2);
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
        leftSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            Config.addTagSmartHistoryDivider = leftSplit.getDividerLocation();
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

            // 收集智能标签
            List<String> smartTags = new ArrayList<>();
            if (dateButton.isSelected()) {
                smartTags.add(todayDate);
            }

            // 合并所有标签
            List<String> allTags = new ArrayList<>();
            allTags.addAll(smartTags);
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

            applyTagToSelectedFiles(selectedFiles, allTags, refreshAction);
        });

        // 回车键触发添加标签按钮
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && !event.isControlDown() && !event.isShiftDown()) {
                    event.consume();
                    addButton.doClick();
                }
            }
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

        // 面板显示后自动聚焦到自定义标签输入框
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                SwingUtilities.invokeLater(input::requestFocusInWindow);
            }
        });

        return panel;
    }

    /**
     * 将指定标签添加到选中的文件，并刷新当前标签页。
     *
     * @param selectedFiles 已选中的文件列表
     * @param tags          要添加的标签列表
     * @param refreshAction 刷新回调
     */
    private static void applyTagToSelectedFiles(List<File> selectedFiles, List<String> tags, Runnable refreshAction) {
        // 先记录新标签到配置，确保 Config.tags 已更新
        TagUtil.rememberTags(tags, new HashSet<>());

        int renamed = 0;
        List<String> failedFiles = new ArrayList<>();
        for (File file : selectedFiles) {
            try {
                Path filePath = file.toPath();
                if (FileUtil.addTags(filePath, tags)) {
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
        refreshAction.run();
    }

    /**
     * 创建移除标签面板。
     *
     * @param path         目录路径
     * @param refreshAction 刷新回调
     * @return 移除标签面板
     */
    private static JPanel createRemoveTagTab(String path, Runnable refreshAction) {
        return createRemoveTagTab(path, refreshAction, null);
    }

    /**
     * 创建移除标签面板。
     *
     * @param path          目录路径
     * @param refreshAction 刷新回调
     * @param filesToSelect 要自动选中的文件路径列表（null 表示不自动选中）
     * @return 移除标签面板
     */
    private static JPanel createRemoveTagTab(String path, Runnable refreshAction, List<Path> filesToSelect) {
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
                        performRemove(selectedFiles, Set.of(tag), refreshAction);
                    }
                }
            });

            tagButtons.add(tagButton);
            tagsPanel.add(tagButton);
            tagsPanel.add(Box.createVerticalStrut(5));
        }
        enableRangeSelection(tagButtons);

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_CONTENT);
        tagsScroll.setBorder(BorderFactory.createTitledBorder("文件标签"));

        // ==================== 右侧：有标签的文件列表模块 ====================
        JPanel filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        filesPanel.setBackground(BG_CONTENT);

        final List<JToggleButton> fileButtons = new ArrayList<>();
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

            fileButtons.add(fileButton);
            filesPanel.add(fileButton);
            filesPanel.add(Box.createVerticalStrut(5));
        }
        enableRangeSelection(fileButtons);

        // 自动选中指定的文件
        if (filesToSelect != null && !filesToSelect.isEmpty()) {
            Set<String> selectNames = new HashSet<>();
            for (Path p : filesToSelect) {
                Path fn = p.getFileName();
                if (fn != null) {
                    selectNames.add(fn.toString());
                }
            }
            for (int i = 0; i < fileButtons.size(); i++) {
                File f = taggedFiles.get(i);
                if (selectNames.contains(f.getName())) {
                    fileButtons.get(i).setSelected(true);
                }
            }
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

            performRemove(selectedFiles, selectedTags, refreshAction);
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
        boolean hasDate = false;
        for (String tag : Config.tags) {
            tagListModel.addElement(tag);
            if (FileUtil.TAG_ORDER_FILENAME.equals(tag)) {
                hasFilename = true;
            }
            if (FileUtil.TAG_ORDER_VERSION.equals(tag)) {
                hasVersion = true;
            }
            if (FileUtil.TAG_ORDER_DATE.equals(tag)) {
                hasDate = true;
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
        if (!hasDate) {
            int versionIndex = tagListModel.indexOf(FileUtil.TAG_ORDER_VERSION);
            tagListModel.add(versionIndex + 1, FileUtil.TAG_ORDER_DATE);
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
            Config.everythingPath = pathField.getText().trim();
            Config.tags = Collections.list(tagListModel.elements());
            ConfigUtil.save();
            reorderDirectoryTags(path);
        });

        JButton rescanButton = new JButton("重新扫描历史标签");
        rescanButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        rescanButton.setPreferredSize(new Dimension(rescanButton.getPreferredSize().width + 20, 36));
        rescanButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rescanButton.setFocusPainted(false);
        rescanButton.addActionListener(e -> {
            LinkedHashSet<String> scannedTags = new LinkedHashSet<>();
            File currentDir = new File(path);
            File[] files = currentDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        for (String tag : FileUtil.parseAllTags(file.getName())) {
                            if (FileUtil.VERSION_TAG_PATTERN.matcher(tag).matches()) {
                                continue;
                            }
                            if (FileUtil.DATE_TAG_PATTERN.matcher(tag).matches()) {
                                continue;
                            }
                            scannedTags.add(tag);
                        }
                    }
                }
            }

            tagListModel.clear();
            tagListModel.addElement(FileUtil.TAG_ORDER_FILENAME);
            tagListModel.addElement(FileUtil.TAG_ORDER_VERSION);
            tagListModel.addElement(FileUtil.TAG_ORDER_DATE);
            for (String tag : scannedTags) {
                tagListModel.addElement(tag);
            }

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
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GRAY), BorderFactory.createEmptyBorder(8, 12, 8, 12)));

            String text = value != null ? value.toString() : "";
            boolean isSpecial = FileUtil.TAG_ORDER_FILENAME.equals(text)
                    || FileUtil.TAG_ORDER_VERSION.equals(text)
                    || FileUtil.TAG_ORDER_DATE.equals(text);

            if (isSelected) {
                // 选中状态保持系统默认
            } else if (isSpecial) {
                label.setBackground(SPECIAL_BG);
                label.setForeground(SPECIAL_FG);
                if (FileUtil.TAG_ORDER_FILENAME.equals(text)) {
                    label.setText("{文件名}  —  源文件名在标签序列中的位置");
                } else if (FileUtil.TAG_ORDER_VERSION.equals(text)) {
                    label.setText("{版本号}  —  版本号（如 V1、V2）在标签序列中的位置");
                } else if (FileUtil.TAG_ORDER_DATE.equals(text)) {
                    label.setText("{当前日期}  —  日期标签（如 20260506）在标签序列中的位置");
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
                if (dragIndex >= 0 && dragIndex < model.size()) {
                    model.remove(dragIndex);
                    if (dropIndex > dragIndex) {
                        dropIndex--;
                    }
                }
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
     * 执行移除标签操作。
     *
     * @param selectedFiles 已选中的文件列表
     * @param tagsToRemove  要移除的标签集合
     * @param refreshAction 刷新回调
     */
    private static void performRemove(List<File> selectedFiles, Set<String> tagsToRemove, Runnable refreshAction) {
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
     * @param icon        图标
     * @param title       标题
     * @param value       值
     * @param tagSupplier 标签提供者
     * @return 智能标签面板
     */
    private static JPanel createSmartTagPanel(String icon, String title, String value, Supplier<String> tagSupplier) {
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
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GRADIENT_START, 2, true), BorderFactory.createEmptyBorder(10, 24, 10, 24)));
    }

    /**
     * 分割标签字符串
     *
     * @param typed 输入的标签字符串
     * @return 标签列表
     */
    private static List<String> splitTags(String typed) {
        if (typed == null) {
            return new ArrayList<>();
        }
        String s = typed.trim();
        if (s.isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = s.split("\\s+");
        return new ArrayList<>(Arrays.asList(parts));
    }

    /**
     * 为 JToggleButton 列表启用 Shift+点击范围选择。
     *
     * @param buttons 按钮列表
     */
    private static void enableRangeSelection(List<JToggleButton> buttons) {
        final int[] anchorIndex = {-1};
        for (int i = 0; i < buttons.size(); i++) {
            final int currentIndex = i;
            final JToggleButton button = buttons.get(i);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.isShiftDown() && anchorIndex[0] >= 0) {
                        int start = Math.min(anchorIndex[0], currentIndex);
                        int end = Math.max(anchorIndex[0], currentIndex);
                        for (JToggleButton toggleButton : buttons) {
                            toggleButton.setSelected(false);
                        }
                        for (int j = start; j <= end; j++) {
                            buttons.get(j).setSelected(true);
                        }
                    } else {
                        anchorIndex[0] = currentIndex;
                    }
                }
            });
        }
    }

}
