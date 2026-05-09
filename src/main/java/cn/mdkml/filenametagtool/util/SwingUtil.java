package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.component.BadgeToggleButton;
import cn.mdkml.filenametagtool.component.FileTableModel;
import cn.mdkml.filenametagtool.component.HighlightCellRenderer;
import cn.mdkml.filenametagtool.component.SortableHeaderRenderer;
import cn.mdkml.filenametagtool.component.WrapLayout;
import cn.mdkml.filenametagtool.model.Config;
import cn.mdkml.filenametagtool.model.SearchResult;
import cn.mdkml.filenametagtool.model.TabIndex;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SwingUtil {

    /** 当前显示的通知弹窗，用于确保同一时间只显示一个 */
    private static volatile JDialog currentNotification;

    private static final Color GRADIENT_START = new Color(74, 144, 226);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BG_CONTENT = new Color(249, 249, 249);
    private static final Color BG_MAIN = new Color(240, 240, 240);
    private static final Color BORDER_GRAY = new Color(220, 220, 220);
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color TEXT_GRAY = new Color(150, 150, 150);

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
        refreshSearch[0] = () -> finalTabbedPane.setComponentAt(TabIndex.SEARCH, createSearchTab(finalPath, refreshSearch[0]));
        refreshAddTag[0] = () -> finalTabbedPane.setComponentAt(TabIndex.ADD_TAG, createAddTagTab(finalPath, refreshAddTag[0]));
        refreshRemoveTag[0] = () -> finalTabbedPane.setComponentAt(TabIndex.REMOVE_TAG, createRemoveTagTab(finalPath, refreshRemoveTag[0]));
        refreshSettings[0] = () -> finalTabbedPane.setComponentAt(TabIndex.SETTINGS, createSettingsTab(finalPath, refreshSettings[0]));

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
                case TabIndex.SEARCH -> refreshSearch[0].run();
                case TabIndex.ADD_TAG -> refreshAddTag[0].run();
                case TabIndex.REMOVE_TAG -> refreshRemoveTag[0].run();
                case TabIndex.SETTINGS -> refreshSettings[0].run();
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

        EverythingUtil searcher;
        try {
            searcher = EverythingUtil.getInstance();
        } catch (RuntimeException e) {
            showError(e.getMessage());
            return panel;
        }

        if (!searcher.isEverythingRunning()) {
            JLabel errorLabel = new JLabel("错误：Everything 客户端未运行，请先启动 Everything");
            errorLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
            errorLabel.setForeground(Color.RED);
            panel.add(errorLabel, BorderLayout.CENTER);
            return panel;
        }

        String wrapL = Config.getTagWrapLeft();
        String wrapR = Config.getTagWrapRight();
        List<SearchResult> results;
        try {
            results = searcher.search(wrapL + " " + wrapR, path);
        } catch (RuntimeException e) {
            showError(e.getMessage());
            return panel;
        }

        Map<String, Integer> tagCount = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile(Pattern.quote(wrapL) + "([^" + Pattern.quote(wrapR) + "]+)" + Pattern.quote(wrapR));
        for (SearchResult result : results) {
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
                            String searchQuery = path + " " + Config.getTagWrapLeft() + tag + Config.getTagWrapRight();
                            try {
                                EverythingUtil.launchEverythingUI(searchQuery, Config.everythingPath);
                            } catch (RuntimeException ex) {
                                showError(ex.getMessage());
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
                    String tagText = toggleButton.getText().trim();
                    selectedTags.add(tagText);
                }
            }
            if (!selectedTags.isEmpty()) {
                StringBuilder queryBuilder = new StringBuilder(path);
                for (String tag : selectedTags) {
                    queryBuilder.append(" ")
                                .append(Config.getTagWrapLeft())
                                .append(tag)
                                .append(Config.getTagWrapRight());
                }
                try {
                    EverythingUtil.launchEverythingUI(queryBuilder.toString(), Config.everythingPath);
                } catch (RuntimeException ex) {
                    showError(ex.getMessage());
                }
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

        // ==================== 右侧上部：当前目录文件模块（含搜索和排序） ====================
        File currentDir = new File(path);
        File[] files = currentDir.listFiles();

        final List<File> allFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    allFiles.add(file);
                }
            }
        }

        // 文件表格模型
        FileTableModel tableModel = new FileTableModel();
        tableModel.setFiles(allFiles);
        tableModel.setSort(Config.fileSortColumn, Config.fileSortAscending);

        JTable fileTable = new JTable(tableModel);
        fileTable.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        fileTable.setRowHeight(28);
        fileTable.setShowGrid(false);
        fileTable.setIntercellSpacing(new Dimension(0, 0));
        fileTable.setBackground(BG_CONTENT);
        fileTable.setSelectionBackground(new Color(200, 220, 240));
        fileTable.setSelectionForeground(Color.BLACK);
        fileTable.setFocusable(false);
        fileTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        fileTable.getTableHeader().setReorderingAllowed(false);

        // 搜索高亮渲染器
        HighlightCellRenderer highlightRenderer = new HighlightCellRenderer();
        for (int i = 0; i < fileTable.getColumnCount(); i++) {
            fileTable.getColumnModel().getColumn(i).setCellRenderer(highlightRenderer);
        }

        // 列宽设置（从配置恢复）
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(Config.fileColumnWidths[0]);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(Config.fileColumnWidths[1]);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(Config.fileColumnWidths[2]);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(Config.fileColumnWidths[3]);

        // 列宽拖拽保存
        fileTable.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                for (int i = 0; i < fileTable.getColumnCount(); i++) {
                    Config.fileColumnWidths[i] = fileTable.getColumnModel().getColumn(i).getWidth();
                }
                ConfigUtil.save();
            }

            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
            }
        });

        // 自定义表头渲染器
        SortableHeaderRenderer headerRenderer = new SortableHeaderRenderer();
        headerRenderer.setSortState(Config.fileSortColumn, Config.fileSortAscending);
        fileTable.getTableHeader().setDefaultRenderer(headerRenderer);

        // 表头悬停效果
        fileTable.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = fileTable.getTableHeader().columnAtPoint(e.getPoint());
                headerRenderer.setHoverColumn(col);
                fileTable.getTableHeader().repaint();
            }
        });
        fileTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                headerRenderer.setHoverColumn(-1);
                fileTable.getTableHeader().repaint();
            }
        });

        // 表头点击排序
        fileTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = fileTable.getTableHeader().columnAtPoint(e.getPoint());
                if (col < 0) return;
                boolean ascending;
                if (col == tableModel.getSortColumn()) {
                    ascending = !tableModel.isSortAscending();
                } else {
                    ascending = true;
                }
                tableModel.setSort(col, ascending);
                headerRenderer.setSortState(col, ascending);
                fileTable.getTableHeader().repaint();

                Config.fileSortColumn = col;
                Config.fileSortAscending = ascending;
                ConfigUtil.save();
            }
        });

        JScrollPane filesScroll = new JScrollPane(fileTable);
        filesScroll.getVerticalScrollBar().setUnitIncrement(16);
        filesScroll.setBackground(BG_CONTENT);
        filesScroll.getViewport().setBackground(BG_CONTENT);
        filesScroll.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));

        // 搜索输入框（内嵌清除按钮和搜索图标）
        JLabel searchIconLabel = new JLabel(createSearchIcon());
        searchIconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
        searchIconLabel.setOpaque(false);

        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        searchField.setToolTipText("输入文件名进行搜索，支持中英文及特殊字符（Enter 搜索，Esc 清除）");
        searchField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 2));

        JLabel searchPlaceholder = new JLabel("搜索文件");
        searchPlaceholder.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        searchPlaceholder.setForeground(TEXT_GRAY);
        searchPlaceholder.setOpaque(false);

        JButton clearButton = new JButton("\u00d7");
        clearButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        clearButton.setPreferredSize(new Dimension(26, 26));
        clearButton.setFocusPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setBorderPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.setVisible(false);
        clearButton.setMargin(new Insets(0, 0, 0, 0));
        clearButton.setToolTipText("清除搜索内容");
        clearButton.setForeground(TEXT_GRAY);

        JPanel searchRightPanel = new JPanel(new BorderLayout(0, 0));
        searchRightPanel.setOpaque(false);
        searchRightPanel.add(clearButton, BorderLayout.WEST);
        searchRightPanel.add(searchIconLabel, BorderLayout.EAST);

        JPanel searchFieldPanel = new JPanel(new BorderLayout(0, 0));
        searchFieldPanel.setBackground(Color.WHITE);
        searchFieldPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel searchCenterPanel = new JPanel(new BorderLayout());
        searchCenterPanel.setOpaque(false);
        searchCenterPanel.add(searchField, BorderLayout.CENTER);
        searchCenterPanel.add(searchPlaceholder, BorderLayout.WEST);
        searchFieldPanel.add(searchCenterPanel, BorderLayout.CENTER);
        searchFieldPanel.add(searchRightPanel, BorderLayout.EAST);

        // 原始边框标题
        final TitledBorder originalBorder = BorderFactory.createTitledBorder("当前目录文件");

        // 状态栏
        JLabel statusBar = new JLabel();
        statusBar.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        statusBar.setForeground(new Color(100, 100, 100));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // 更新状态栏方法
        Runnable updateStatusBar = () -> {
            int totalCount = tableModel.getMatchCount();
            long totalSize = tableModel.getTotalSize();
            int selCount = selectedFiles.size();
            long selSize = 0;
            for (File f : selectedFiles) {
                selSize += f.length();
            }
            if (selCount == 0) {
                statusBar.setText(totalCount + " 个项目 | " + formatFileSize(totalSize));
            } else {
                statusBar.setText(totalCount + " 个项目 | " + formatFileSize(totalSize)
                        + " | 选中 " + selCount + " 个项目 | " + formatFileSize(selSize));
            }
        };
        updateStatusBar.run();

        // 表格选择同步到 selectedFiles
        fileTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedFiles.clear();
            int[] rows = fileTable.getSelectedRows();
            for (int row : rows) {
                int modelRow = fileTable.convertRowIndexToModel(row);
                File f = tableModel.getFileAt(modelRow);
                if (f != null) selectedFiles.add(f);
            }
            updateStatusBar.run();
        });

        // 防抖定时器（300ms）
        Timer addTagSearchTimer = new Timer(300, e -> {
            String filterText = searchField.getText().trim();
            boolean isFiltering = !filterText.isEmpty();
            clearButton.setVisible(isFiltering);

            String[] keywords = isFiltering ? filterText.split("\\s+") : new String[0];
            tableModel.setSearchKeywords(keywords);
            highlightRenderer.setKeywords(keywords);

            // 搜索时重置表头排序状态
            if (isFiltering) {
                headerRenderer.setSortState(-1, true);
            } else {
                headerRenderer.setSortState(Config.fileSortColumn, Config.fileSortAscending);
            }
            fileTable.getTableHeader().repaint();

            updateStatusBar.run();
            filesScroll.repaint();
        });
        addTagSearchTimer.setRepeats(false);

        // 实时监听输入
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchPlaceholder.setVisible(false);
                clearButton.setVisible(true);
                addTagSearchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (searchField.getText().isEmpty()) {
                    clearButton.setVisible(false);
                    searchPlaceholder.setVisible(true);
                }
                addTagSearchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                addTagSearchTimer.restart();
            }
        });

        // 键盘支持：Esc 清除
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    searchField.setText("");
                    clearButton.setVisible(false);
                    searchPlaceholder.setVisible(true);
                    searchField.requestFocusInWindow();
                    addTagSearchTimer.restart();
                }
            }
        });

        // 清除按钮
        clearButton.addActionListener(e -> {
            searchField.setText("");
            clearButton.setVisible(false);
            searchPlaceholder.setVisible(true);
            searchField.requestFocusInWindow();
            addTagSearchTimer.restart();
        });

        // 搜索面板包装
        JPanel searchPanel = new JPanel(new BorderLayout(0, 0));
        searchPanel.setBackground(BG_CONTENT);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchPanel.add(searchFieldPanel, BorderLayout.CENTER);

        // 文件容器：搜索框 + 表格 + 状态栏
        JPanel filesContainer = new JPanel(new BorderLayout());
        filesContainer.setBackground(BG_CONTENT);
        filesContainer.add(searchPanel, BorderLayout.NORTH);
        filesContainer.add(filesScroll, BorderLayout.CENTER);
        filesContainer.add(statusBar, BorderLayout.SOUTH);
        filesContainer.setBorder(originalBorder);
        // 自动选中指定的文件
        if (filesToSelect != null && !filesToSelect.isEmpty()) {
            Set<String> selectNames = new HashSet<>();
            for (Path p : filesToSelect) {
                Path fn = p.getFileName();
                if (fn != null) {
                    selectNames.add(fn.toString());
                }
            }
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (selectNames.contains(tableModel.getValueAt(i, 0))) {
                    int viewRow = fileTable.convertRowIndexToView(i);
                    if (viewRow >= 0) fileTable.addRowSelectionInterval(viewRow, viewRow);
                }
            }
        }

        // ==================== 右侧下部：自定义标签输入模块 ====================
        String placeholderText = "输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈";
        JTextArea input = new JTextArea();
        input.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setText(placeholderText);
        input.setForeground(TEXT_GRAY);
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
                    input.setForeground(TEXT_GRAY);
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
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filesContainer, inputScroll);
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

        // ==================== 右侧：有标签的文件列表模块（含搜索和排序） ====================
        FileTableModel tableModel = new FileTableModel();
        tableModel.setFiles(taggedFiles);
        tableModel.setSort(Config.fileSortColumn, Config.fileSortAscending);

        JTable fileTable = new JTable(tableModel);
        fileTable.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        fileTable.setRowHeight(28);
        fileTable.setShowGrid(false);
        fileTable.setIntercellSpacing(new Dimension(0, 0));
        fileTable.setBackground(BG_CONTENT);
        fileTable.setSelectionBackground(new Color(200, 220, 240));
        fileTable.setSelectionForeground(Color.BLACK);
        fileTable.setFocusable(false);
        fileTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        fileTable.getTableHeader().setReorderingAllowed(false);

        // 搜索高亮渲染器
        HighlightCellRenderer highlightRenderer = new HighlightCellRenderer();
        for (int i = 0; i < fileTable.getColumnCount(); i++) {
            fileTable.getColumnModel().getColumn(i).setCellRenderer(highlightRenderer);
        }

        // 列宽设置（从配置恢复）
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(Config.fileColumnWidths[0]);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(Config.fileColumnWidths[1]);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(Config.fileColumnWidths[2]);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(Config.fileColumnWidths[3]);

        // 列宽拖拽保存
        fileTable.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                for (int i = 0; i < fileTable.getColumnCount(); i++) {
                    Config.fileColumnWidths[i] = fileTable.getColumnModel().getColumn(i).getWidth();
                }
                ConfigUtil.save();
            }

            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
            }
        });

        // 自定义表头渲染器
        SortableHeaderRenderer headerRenderer = new SortableHeaderRenderer();
        headerRenderer.setSortState(Config.fileSortColumn, Config.fileSortAscending);
        fileTable.getTableHeader().setDefaultRenderer(headerRenderer);

        // 表头悬停效果
        fileTable.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = fileTable.getTableHeader().columnAtPoint(e.getPoint());
                headerRenderer.setHoverColumn(col);
                fileTable.getTableHeader().repaint();
            }
        });
        fileTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                headerRenderer.setHoverColumn(-1);
                fileTable.getTableHeader().repaint();
            }
        });

        // 表头点击排序
        fileTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = fileTable.getTableHeader().columnAtPoint(e.getPoint());
                if (col < 0) return;
                boolean ascending;
                if (col == tableModel.getSortColumn()) {
                    ascending = !tableModel.isSortAscending();
                } else {
                    ascending = true;
                }
                tableModel.setSort(col, ascending);
                headerRenderer.setSortState(col, ascending);
                fileTable.getTableHeader().repaint();

                Config.fileSortColumn = col;
                Config.fileSortAscending = ascending;
                ConfigUtil.save();
            }
        });

        JScrollPane filesScroll = new JScrollPane(fileTable);
        filesScroll.getVerticalScrollBar().setUnitIncrement(16);
        filesScroll.setBackground(BG_CONTENT);
        filesScroll.getViewport().setBackground(BG_CONTENT);
        filesScroll.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
        // 搜索输入框（内嵌清除按钮和搜索图标）
        JLabel removeSearchIconLabel = new JLabel(createSearchIcon());
        removeSearchIconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
        removeSearchIconLabel.setOpaque(false);

        JTextField removeSearchField = new JTextField();
        removeSearchField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        removeSearchField.setToolTipText("输入文件名进行搜索，支持中英文及特殊字符（Enter 搜索，Esc 清除）");
        removeSearchField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 2));

        JLabel removeSearchPlaceholder = new JLabel("搜索文件");
        removeSearchPlaceholder.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        removeSearchPlaceholder.setForeground(TEXT_GRAY);
        removeSearchPlaceholder.setOpaque(false);

        JButton removeClearButton = new JButton("\u00d7");
        removeClearButton.setForeground(TEXT_GRAY);
        removeClearButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        removeClearButton.setPreferredSize(new Dimension(26, 26));
        removeClearButton.setFocusPainted(false);
        removeClearButton.setContentAreaFilled(false);
        removeClearButton.setBorderPainted(false);
        removeClearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeClearButton.setVisible(false);
        removeClearButton.setMargin(new Insets(0, 0, 0, 0));
        removeClearButton.setToolTipText("清除搜索内容");

        JPanel removeSearchRightPanel = new JPanel(new BorderLayout(0, 0));
        removeSearchRightPanel.setOpaque(false);
        removeSearchRightPanel.add(removeClearButton, BorderLayout.WEST);
        removeSearchRightPanel.add(removeSearchIconLabel, BorderLayout.EAST);

        JPanel removeSearchFieldPanel = new JPanel(new BorderLayout(0, 0));
        removeSearchFieldPanel.setBackground(Color.WHITE);
        removeSearchFieldPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel removeSearchCenterPanel = new JPanel(new BorderLayout());
        removeSearchCenterPanel.setOpaque(false);
        removeSearchCenterPanel.add(removeSearchField, BorderLayout.CENTER);
        removeSearchCenterPanel.add(removeSearchPlaceholder, BorderLayout.WEST);
        removeSearchFieldPanel.add(removeSearchCenterPanel, BorderLayout.CENTER);
        removeSearchFieldPanel.add(removeSearchRightPanel, BorderLayout.EAST);

        // 原始边框标题
        final TitledBorder removeOriginalBorder = BorderFactory.createTitledBorder("有标签的文件");

        // 状态栏
        JLabel removeStatusBar = new JLabel();
        removeStatusBar.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        removeStatusBar.setForeground(new Color(100, 100, 100));
        removeStatusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // 更新状态栏方法
        Runnable updateRemoveStatusBar = () -> {
            int totalCount = tableModel.getMatchCount();
            long totalSize = tableModel.getTotalSize();
            int selCount = selectedFiles.size();
            long selSize = 0;
            for (File f : selectedFiles) {
                selSize += f.length();
            }
            if (selCount == 0) {
                removeStatusBar.setText(totalCount + " 个项目 | " + formatFileSize(totalSize));
            } else {
                removeStatusBar.setText(totalCount + " 个项目 | " + formatFileSize(totalSize)
                        + " | 选中 " + selCount + " 个项目 | " + formatFileSize(selSize));
            }
        };
        updateRemoveStatusBar.run();

        // 表格选择同步到 selectedFiles
        fileTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedFiles.clear();
            int[] rows = fileTable.getSelectedRows();
            for (int row : rows) {
                int modelRow = fileTable.convertRowIndexToModel(row);
                File f = tableModel.getFileAt(modelRow);
                if (f != null) selectedFiles.add(f);
            }
            updateRemoveStatusBar.run();
        });

        // 防抖定时器（300ms）
        Timer removeSearchTimer = new Timer(300, e -> {
            String filterText = removeSearchField.getText().trim();
            boolean isFiltering = !filterText.isEmpty();
            removeClearButton.setVisible(isFiltering);

            String[] keywords = isFiltering ? filterText.split("\\s+") : new String[0];
            tableModel.setSearchKeywords(keywords);
            highlightRenderer.setKeywords(keywords);

            // 搜索时重置表头排序状态
            if (isFiltering) {
                headerRenderer.setSortState(-1, true);
            } else {
                headerRenderer.setSortState(Config.fileSortColumn, Config.fileSortAscending);
            }
            fileTable.getTableHeader().repaint();

            updateRemoveStatusBar.run();
            filesScroll.repaint();
        });
        removeSearchTimer.setRepeats(false);

        // 实时监听输入
        removeSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                removeSearchPlaceholder.setVisible(false);
                removeClearButton.setVisible(true);
                removeSearchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (removeSearchField.getText().isEmpty()) {
                    removeClearButton.setVisible(false);
                    removeSearchPlaceholder.setVisible(true);
                }
                removeSearchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                removeSearchTimer.restart();
            }
        });

        // 键盘支持：Esc 清除
        removeSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    removeSearchField.setText("");
                    removeClearButton.setVisible(false);
                    removeSearchPlaceholder.setVisible(true);
                    removeSearchField.requestFocusInWindow();
                    removeSearchTimer.restart();
                }
            }
        });

        // 清除按钮
        removeClearButton.addActionListener(e -> {
            removeSearchField.setText("");
            removeClearButton.setVisible(false);
            removeSearchPlaceholder.setVisible(true);
            removeSearchField.requestFocusInWindow();
            removeSearchTimer.restart();
        });

        // 搜索面板包装
        JPanel removeSearchPanel = new JPanel(new BorderLayout(0, 0));
        removeSearchPanel.setBackground(BG_CONTENT);
        removeSearchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        removeSearchPanel.add(removeSearchFieldPanel, BorderLayout.CENTER);

        // 文件容器：搜索框 + 表格 + 状态栏
        JPanel filesContainer = new JPanel(new BorderLayout());
        filesContainer.setBackground(BG_CONTENT);
        filesContainer.add(removeSearchPanel, BorderLayout.NORTH);
        filesContainer.add(filesScroll, BorderLayout.CENTER);
        filesContainer.add(removeStatusBar, BorderLayout.SOUTH);
        filesContainer.setBorder(removeOriginalBorder);
        // 自动选中指定的文件
        if (filesToSelect != null && !filesToSelect.isEmpty()) {
            Set<String> selectNames = new HashSet<>();
            for (Path p : filesToSelect) {
                Path fn = p.getFileName();
                if (fn != null) {
                    selectNames.add(fn.toString());
                }
            }
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (selectNames.contains(tableModel.getValueAt(i, 0))) {
                    int viewRow = fileTable.convertRowIndexToView(i);
                    if (viewRow >= 0) fileTable.addRowSelectionInterval(viewRow, viewRow);
                }
            }
        }

        // ==================== JSplitPane 实现左右可拖拽 ====================
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tagsScroll, filesContainer);
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

        // ==================== 标签包裹符号配置 ====================
        JPanel bracketPanel = new JPanel(new BorderLayout(8, 0));
        bracketPanel.setBackground(BG_CONTENT);
        bracketPanel.setBorder(BorderFactory.createTitledBorder("标签包裹符号"));

        JRadioButton fullwidthRadio = new JRadioButton("【】 全角方括号");
        fullwidthRadio.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        fullwidthRadio.setBackground(BG_CONTENT);
        fullwidthRadio.setFocusPainted(false);

        JRadioButton bracketRadio = new JRadioButton("[] 半角方括号");
        bracketRadio.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        bracketRadio.setBackground(BG_CONTENT);
        bracketRadio.setFocusPainted(false);

        ButtonGroup bracketGroup = new ButtonGroup();
        bracketGroup.add(fullwidthRadio);
        bracketGroup.add(bracketRadio);

        if (Config.STYLE_BRACKET.equals(Config.tagBracketStyle)) {
            bracketRadio.setSelected(true);
        } else {
            fullwidthRadio.setSelected(true);
        }

        JLabel previewLabel = new JLabel();
        previewLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        previewLabel.setForeground(TEXT_DARK);

        Runnable updatePreview = () -> {
            String l = fullwidthRadio.isSelected() ? "【" : "[";
            String r = fullwidthRadio.isSelected() ? "】" : "]";
            previewLabel.setText("示例: " + l + "标签名" + r);
        };
        updatePreview.run();

        fullwidthRadio.addActionListener(e -> updatePreview.run());
        bracketRadio.addActionListener(e -> updatePreview.run());

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        radioPanel.setBackground(BG_CONTENT);
        radioPanel.add(fullwidthRadio);
        radioPanel.add(bracketRadio);
        radioPanel.add(Box.createHorizontalStrut(20));
        radioPanel.add(previewLabel);

        bracketPanel.add(radioPanel, BorderLayout.CENTER);

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

        // 上下布局：路径 + 包裹符号 + 标签排序
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(BG_CONTENT);
        pathPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathPanel.getPreferredSize().height + 10));
        bracketPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bracketPanel.getPreferredSize().height + 10));
        centerPanel.add(pathPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(bracketPanel);
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
            Config.tagBracketStyle = bracketRadio.isSelected() ? Config.STYLE_BRACKET : Config.STYLE_FULLWIDTH;
            Config.tags = Collections.list(tagListModel.elements());
            ConfigUtil.save();
            showSuccess("配置已保存");
        });

        JButton applyButton = new JButton("应用到目录文件");
        applyButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        applyButton.setPreferredSize(new Dimension(applyButton.getPreferredSize().width + 20, 36));
        applyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyButton.setFocusPainted(false);
        applyButton.addActionListener(e -> {
            Config.everythingPath = pathField.getText().trim();
            Config.tagBracketStyle = bracketRadio.isSelected() ? Config.STYLE_BRACKET : Config.STYLE_FULLWIDTH;
            Config.tags = Collections.list(tagListModel.elements());
            ConfigUtil.save();
            applyTagSettingsToDirectory(path);
        });

        JButton rescanButton = new JButton("扫描目录标签");
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
            Config.tagBracketStyle = bracketRadio.isSelected() ? Config.STYLE_BRACKET : Config.STYLE_FULLWIDTH;
            Config.tags = Collections.list(tagListModel.elements());
            ConfigUtil.save();
            showSuccess("已扫描到 " + scannedTags.size() + " 个标签");
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(rescanButton);
        buttonPanel.add(applyButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 将当前标签设置（顺序和包裹符号）应用到目录中所有文件。
     *
     * @param dirPath 目录路径
     */
    private static void applyTagSettingsToDirectory(String dirPath) {
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
                    if (FileUtil.applyTagSettings(file.toPath())) {
                        reordered++;
                    }
                } catch (Exception e) {
                    // 跳过无法重命名的文件
                }
            }
        }
        showSuccess("已重排 " + reordered + " 个文件的标签顺序（包裹符号已统一为" +
                (Config.STYLE_BRACKET.equals(Config.tagBracketStyle) ? "[]" : "【】") + "）");
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
     * 检查文件名是否匹配所有关键词（不区分大小写）。
     *
     * @param fileName 文件名
     * @param keywords 关键词数组
     * @return 是否全部匹配
     */
    private static boolean matchesAllKeywords(String fileName, String[] keywords) {
        String lowerName = fileName.toLowerCase();
        for (String keyword : keywords) {
            if (!lowerName.contains(keyword.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算文件名与关键词的匹配相关度（分数越高越相关）。
     * 排序规则：匹配关键词数 > 文件名开头匹配 > 短文件名优先 > 关键词出现次数
     *
     * @param fileName 文件名
     * @param keywords 关键词数组
     * @return 相关度分数
     */
    private static int calculateRelevance(String fileName, String[] keywords) {
        String lowerName = fileName.toLowerCase();
        int score = 0;
        int matchedCount = 0;
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            int idx = lowerName.indexOf(lowerKeyword);
            if (idx >= 0) {
                matchedCount++;
                score += 100;
                if (idx == 0) score += 50;
                if (fileName.length() - keyword.length() < 5) score += 20;
                int count = 0;
                int from = 0;
                while ((from = lowerName.indexOf(lowerKeyword, from)) >= 0) {
                    count++;
                    from += lowerKeyword.length();
                }
                score += (count - 1) * 10;
            }
        }
        score += matchedCount * 200;
        score += Math.max(0, 500 - fileName.length());
        return score;
    }

    /**
     * 对文件名中的关键词进行高亮处理，返回 HTML 字符串。
     * 匹配的关键词会被包裹在黄色背景的 &lt;span&gt; 标签中。
     *
     * @param fileName 文件名
     * @param keywords 关键词数组
     * @return 带高亮的 HTML 字符串
     */
    private static String highlightKeywords(String fileName, String[] keywords) {
        String escaped = fileName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String lowerName = fileName.toLowerCase();

        List<int[]> highlights = new ArrayList<>();
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            int from = 0;
            while ((from = lowerName.indexOf(lowerKeyword, from)) >= 0) {
                highlights.add(new int[]{from, from + keyword.length()});
                from += keyword.length();
            }
        }

        highlights.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] interval : highlights) {
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < interval[0]) {
                merged.add(interval);
            } else {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], interval[1]);
            }
        }

        StringBuilder html = new StringBuilder("<html><nobr>");
        int lastEnd = 0;
        for (int[] interval : merged) {
            int start = interval[0];
            int end = interval[1];
            if (start > lastEnd) {
                html.append(escaped, lastEnd, start);
            }
            html.append("<span style=\"background:#FFEB3B;padding:1px\">");
            html.append(escaped, start, end);
            html.append("</span>");
            lastEnd = end;
        }
        if (lastEnd < escaped.length()) {
            html.append(escaped, lastEnd, escaped.length());
        }
        html.append("</nobr></html>");
        return html.toString();
    }

    /**
     * 创建搜索图标（放大镜）。
     *
     * @return 搜索图标
     */
    private static Icon createSearchIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 2, y + 2, 10, 10);
                g2.drawLine(x + 11, y + 11, x + 15, y + 15);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 17;
            }

            @Override
            public int getIconHeight() {
                return 17;
            }
        };
    }

    /**
     * 格式化文件大小为可读字符串
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
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
