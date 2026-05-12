package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.component.FileTableModel;
import cn.mdkml.filenametagtool.component.HighlightCellRenderer;
import cn.mdkml.filenametagtool.component.SortableHeaderRenderer;
import cn.mdkml.filenametagtool.component.TagCellRenderer;
import cn.mdkml.filenametagtool.model.Config;
import cn.mdkml.filenametagtool.model.TabIndex;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
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

/**
 * Swing 工具类
 * <p>
 * 提供标签管理窗口的创建和各种 UI 工具方法
 * </p>
 */
public final class SwingUtil {

    /** 当前显示的通知弹窗，用于确保同一时间只显示一个 */
    private static volatile JDialog currentNotification;
    /** 统一标签面板的搜索关键字（内存暂存，刷新后恢复） */
    private static String unifiedTagSearchKeyword = "";

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
        final Runnable[] refreshUnifiedTag = new Runnable[1];
        final Runnable[] refreshSettings = new Runnable[1];
        refreshUnifiedTag[0] = () -> finalTabbedPane.setComponentAt(TabIndex.UNIFIED_TAG, createUnifiedTagTab(finalPath, refreshUnifiedTag[0], filesToSelect));
        refreshSettings[0] = () -> finalTabbedPane.setComponentAt(TabIndex.SETTINGS, createSettingsTab(finalPath, refreshSettings[0]));

        tabbedPane.addTab("标签管理", null, createUnifiedTagTab(path, refreshUnifiedTag[0], filesToSelect));
        tabbedPane.addTab("设置", null, createSettingsTab(path, refreshSettings[0]));

        // 设置初始标签页
        if (initialTab >= 0 && initialTab < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(initialTab);
        }

        // 切换标签时刷新对应面板内容
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = finalTabbedPane.getSelectedIndex();
            switch (selectedIndex) {
                case TabIndex.UNIFIED_TAG -> refreshUnifiedTag[0].run();
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
     * 创建统一标签管理面板。
     * <p>
     * 整合了原搜索、添加标签、移除标签三个面板的功能。
     * 布局：左侧（智能标签+历史标签），右侧（文件列表+自定义标签输入）
     * 支持右键菜单进行标签的添加和移除操作
     * </p>
     *
     * @param path          目录路径
     * @param refreshAction 刷新回调
     * @param filesToSelect 要自动选中的文件路径列表（null 表示不自动选中）
     * @return 统一标签管理面板
     */
    private static JPanel createUnifiedTagTab(String path, Runnable refreshAction, List<Path> filesToSelect) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(BG_CONTENT);

        // 被选中的文件列表，由文件表格的选中状态同步维护
        final List<File> selectedFiles = new ArrayList<>();
        // 所有历史标签，用于多选操作
        final List<JLabel> historyTagLabels = new ArrayList<>();
        // 搜索框引用（使用数组包装以便在 lambda 中引用）
        final JTextField[] searchFieldRef = new JTextField[1];

        // ==================== 统计标签数量 ====================
        File currentDirForCount = new File(path);
        File[] filesForCount = currentDirForCount.listFiles();
        final Map<String, Integer> tagCounts = new HashMap<>();
        if (filesForCount != null) {
            for (File file : filesForCount) {
                if (file.isFile()) {
                    List<String> fileTags = FileUtil.parseAllTags(file.getName());
                    for (String tag : fileTags) {
                        tagCounts.merge(tag, 1, Integer::sum);
                    }
                }
            }
        }

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

        // 创建右键菜单
        JPopupMenu tagPopupMenu = new JPopupMenu();
        JMenuItem addItem = new JMenuItem("添加标签到选中文件");
        JMenuItem removeItem = new JMenuItem("从选中文件移除标签");
        tagPopupMenu.add(addItem);
        tagPopupMenu.add(removeItem);

        // 当前右键点击的标签
        final String[] contextMenuTag = {null};

        addItem.addActionListener(e -> {
            if (contextMenuTag[0] != null && !selectedFiles.isEmpty()) {
                applyTagToSelectedFiles(selectedFiles, List.of(contextMenuTag[0]), refreshAction);
            } else if (selectedFiles.isEmpty()) {
                showMessage("请先在右侧选择文件");
            }
        });

        removeItem.addActionListener(e -> {
            if (contextMenuTag[0] != null && !selectedFiles.isEmpty()) {
                performRemove(selectedFiles, Set.of(contextMenuTag[0]), refreshAction);
            } else if (selectedFiles.isEmpty()) {
                showMessage("请先在右侧选择文件");
            }
        });

        for (String tag : history) {
            // 使用 JLabel 替代 JToggleButton，实现与文件表格标签列一致的渲染效果
            int tagCount = tagCounts.getOrDefault(tag, 0);
            JLabel tagLabel = new JLabel(tag) {
                @Override
                protected void paintComponent(Graphics g) {
                    // 使用 TagCellRenderer 的静态方法绘制带数量的标签
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setFont(getFont());
                    TagCellRenderer.paintTagWithCount(g2d, getWidth(), getHeight(), tag, tagCount);
                    g2d.dispose();
                }
            };
            tagLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            tagLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            tagLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            tagLabel.setPreferredSize(new Dimension(0, 30));
            tagLabel.setMinimumSize(new Dimension(0, 30));
            tagLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            // 单击历史标签：在搜索框中填充标签进行搜索
            tagLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        // 单击搜索：填充搜索框
                        if (searchFieldRef[0] != null) {
                            searchFieldRef[0].setText(Config.getTagWrapLeft() + tag + Config.getTagWrapRight());
                            searchFieldRef[0].requestFocusInWindow();
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent event) {
                    if (SwingUtilities.isRightMouseButton(event)) {
                        contextMenuTag[0] = tag;
                        tagPopupMenu.show(tagLabel, event.getX(), event.getY());
                    }
                }
            });

            historyTagLabels.add(tagLabel);
            tagsPanel.add(tagLabel);
            tagsPanel.add(Box.createVerticalStrut(5));
        }

        // ==================== 左侧上部：智能标签模块 ====================
        JPanel smartPanel = new JPanel();
        smartPanel.setLayout(new BoxLayout(smartPanel, BoxLayout.Y_AXIS));
        smartPanel.setBackground(BG_CONTENT);

        // 当前日期智能标签
        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String todayLabel = "当前日期";

        // 使用 JLabel 实现与历史标签一致的渲染效果
        JLabel dateLabel = new JLabel(todayLabel) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setFont(getFont());
                TagCellRenderer.paintTagWithCount(g2d, getWidth(), getHeight(), todayLabel, 0);
                g2d.dispose();
            }
        };
        dateLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        dateLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateLabel.setPreferredSize(new Dimension(0, 30));
        dateLabel.setMinimumSize(new Dimension(0, 30));
        dateLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // 单击日期标签：在搜索框中填充标签进行搜索
        dateLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    if (searchFieldRef[0] != null) {
                        searchFieldRef[0].setText(Config.getTagWrapLeft() + todayDate + Config.getTagWrapRight());
                        searchFieldRef[0].requestFocusInWindow();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    contextMenuTag[0] = todayDate;
                    tagPopupMenu.show(dateLabel, event.getX(), event.getY());
                }
            }
        });

        smartPanel.add(dateLabel);

        JScrollPane smartScroll = new JScrollPane(smartPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        smartScroll.setBackground(BG_CONTENT);
        smartScroll.setBorder(BorderFactory.createTitledBorder("智能标签"));
        smartScroll.setPreferredSize(new Dimension(0, 80));

        JScrollPane tagsScroll = new JScrollPane(tagsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_CONTENT);
        tagsScroll.setBorder(BorderFactory.createTitledBorder("历史标签（右键菜单可添加/移除）"));

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
        fileTable.setFocusable(true);
        fileTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        fileTable.getTableHeader().setReorderingAllowed(false);

        // 搜索高亮渲染器（用于非标签列）
        HighlightCellRenderer highlightRenderer = new HighlightCellRenderer();

        // 标签列渲染器
        TagCellRenderer tagCellRenderer = new TagCellRenderer();
        // 设置搜索回调：点击标签区域时在搜索框中填充标签
        tagCellRenderer.setSearchCallback(tagName -> {
            if (searchFieldRef[0] != null) {
                searchFieldRef[0].setText(Config.getTagWrapLeft() + tagName + Config.getTagWrapRight());
                searchFieldRef[0].requestFocusInWindow();
            }
        });

        // 设置删除回调：点击标签上的删除按钮时移除该标签
        tagCellRenderer.setDeleteCallback(tagName -> {
            int hoveredRow = tagCellRenderer.getHoveredRow();
            if (hoveredRow >= 0) {
                int modelRow = fileTable.convertRowIndexToModel(hoveredRow);
                File file = tableModel.getFileAt(modelRow);
                if (file != null) {
                    performRemove(List.of(file), Set.of(tagName), refreshAction);
                }
            }
        });

        // 设置列渲染器
        for (int i = 0; i < fileTable.getColumnCount(); i++) {
            if (i == 1) {
                // 标签列使用专用渲染器
                fileTable.getColumnModel().getColumn(i).setCellRenderer(tagCellRenderer);
            } else {
                fileTable.getColumnModel().getColumn(i).setCellRenderer(highlightRenderer);
            }
        }

        // 列宽设置（从配置恢复）
        for (int i = 0; i < Math.min(fileTable.getColumnCount(), Config.fileColumnWidths.length); i++) {
            fileTable.getColumnModel().getColumn(i).setPreferredWidth(Config.fileColumnWidths[i]);
        }

        // 列宽拖拽保存
        fileTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
                for (int i = 0; i < Math.min(fileTable.getColumnCount(), Config.fileColumnWidths.length); i++) {
                    Config.fileColumnWidths[i] = fileTable.getColumnModel().getColumn(i).getWidth();
                }
                ConfigUtil.save();
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
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
                if (col < 0) {
                    return;
                }
                boolean ascending;
                if (col == tableModel.getSortColumn()) {
                    ascending = !tableModel.isSortAscending();
                } else {
                    ascending = true;
                }

                // 排序前保存当前所有选中的文件对象
                List<File> previouslySelectedFiles = new ArrayList<>();
                int[] selectedRows = fileTable.getSelectedRows();
                for (int row : selectedRows) {
                    int modelRow = fileTable.convertRowIndexToModel(row);
                    File file = tableModel.getFileAt(modelRow);
                    if (file != null) {
                        previouslySelectedFiles.add(file);
                    }
                }

                // 执行排序操作
                tableModel.setSort(col, ascending);
                headerRenderer.setSortState(col, ascending);
                fileTable.getTableHeader().repaint();

                // 恢复之前保存的选中状态
                fileTable.clearSelection();
                for (File selectedFile : previouslySelectedFiles) {
                    int modelRow = tableModel.findRowByFile(selectedFile);
                    if (modelRow >= 0) {
                        int viewRow = fileTable.convertRowIndexToView(modelRow);
                        if (viewRow >= 0) {
                            fileTable.addRowSelectionInterval(viewRow, viewRow);
                        }
                    }
                }

                Config.fileSortColumn = col;
                Config.fileSortAscending = ascending;
                ConfigUtil.save();
            }
        });

        // 标签列点击事件（搜索/删除）
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = fileTable.rowAtPoint(e.getPoint());
                int col = fileTable.columnAtPoint(e.getPoint());
                if (col == 1 && row >= 0) {
                    Point cellPoint = new Point(e.getX() - fileTable.getCellRect(row, col, false).x,
                            e.getY() - fileTable.getCellRect(row, col, false).y);
                    tagCellRenderer.handleClick(row, cellPoint, fileTable);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tagCellRenderer.setHoveredTag(-1, -1);
                fileTable.repaint();
            }
        });

        // 标签列鼠标悬停效果（显示删除按钮）
        fileTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = fileTable.rowAtPoint(e.getPoint());
                int col = fileTable.columnAtPoint(e.getPoint());

                int prevHoveredRow = tagCellRenderer.getHoveredRow();
                int prevHoveredTagIndex = tagCellRenderer.getHoveredTagIndex();

                if (col == 1 && row >= 0) {
                    Point cellPoint = new Point(e.getX() - fileTable.getCellRect(row, col, false).x,
                            e.getY() - fileTable.getCellRect(row, col, false).y);
                    int tagIndex = tagCellRenderer.getTagIndexAtPoint(row, cellPoint, fileTable);
                    tagCellRenderer.setHoveredTag(row, tagIndex);
                } else {
                    tagCellRenderer.setHoveredTag(-1, -1);
                }

                if (prevHoveredRow != tagCellRenderer.getHoveredRow()
                        || prevHoveredTagIndex != tagCellRenderer.getHoveredTagIndex()) {
                    fileTable.repaint();
                }
            }
        });

        // F2 重命名文件
        fileTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2) {
                    int selectedRow = fileTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        int modelRow = fileTable.convertRowIndexToModel(selectedRow);
                        File file = tableModel.getFileAt(modelRow);
                        if (file != null && file.exists()) {
                            showRenameDialog(file, fileTable, tableModel, refreshAction);
                        }
                    }
                }
            }
        });

        // 双击文件行打开文件
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = fileTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        File file = tableModel.getFileAt(fileTable.convertRowIndexToModel(row));
                        if (file != null && file.exists()) {
                            openFile(file);
                        }
                    }
                }
            }
        });

        // 右键菜单：重命名文件
        JPopupMenu filePopupMenu = new JPopupMenu();
        JMenuItem renameFileItem = new JMenuItem("重命名");
        renameFileItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        filePopupMenu.add(renameFileItem);
        renameFileItem.addActionListener(e -> {
            int selectedRow = fileTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = fileTable.convertRowIndexToModel(selectedRow);
                File file = tableModel.getFileAt(modelRow);
                if (file != null && file.exists()) {
                    showRenameDialog(file, fileTable, tableModel, refreshAction);
                }
            }
        });
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = fileTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        fileTable.setRowSelectionInterval(row, row);
                        filePopupMenu.show(fileTable, e.getX(), e.getY());
                    }
                }
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
        searchFieldRef[0] = searchField;
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
            if (e.getValueIsAdjusting()) {
                return;
            }
            selectedFiles.clear();
            int[] rows = fileTable.getSelectedRows();
            for (int row : rows) {
                int modelRow = fileTable.convertRowIndexToModel(row);
                File f = tableModel.getFileAt(modelRow);
                if (f != null) {
                    selectedFiles.add(f);
                }
            }
            updateStatusBar.run();
        });

        // 防抖定时器（300ms）
        Timer searchTimer = new Timer(300, e -> {
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
        searchTimer.setRepeats(false);

        // 实时监听输入
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                unifiedTagSearchKeyword = searchField.getText();
                searchPlaceholder.setVisible(false);
                clearButton.setVisible(true);
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                unifiedTagSearchKeyword = searchField.getText();
                if (searchField.getText().isEmpty()) {
                    clearButton.setVisible(false);
                    searchPlaceholder.setVisible(true);
                }
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                unifiedTagSearchKeyword = searchField.getText();
                searchTimer.restart();
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
                    searchTimer.restart();
                }
            }
        });

        // 清除按钮
        clearButton.addActionListener(e -> {
            searchField.setText("");
            clearButton.setVisible(false);
            searchPlaceholder.setVisible(true);
            searchField.requestFocusInWindow();
            searchTimer.restart();
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
                    if (viewRow >= 0) {
                        fileTable.addRowSelectionInterval(viewRow, viewRow);
                    }
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

            if (customTags.isEmpty()) {
                showMessage("请输入要添加的标签");
                return;
            }

            if (selectedFiles.isEmpty()) {
                showMessage("请先选择要添加标签的文件");
                return;
            }

            applyTagToSelectedFiles(selectedFiles, customTags, refreshAction);
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

        JButton removeButton = new JButton("移除标签");
        removeButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        removeButton.setPreferredSize(new Dimension(removeButton.getPreferredSize().width + 20, 36));
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.setFocusPainted(false);
        removeButton.addActionListener(e -> {
            // 收集自定义输入的标签
            String typed = input.getText();
            if (typed.equals(placeholderText)) {
                typed = "";
            }
            List<String> customTags = splitTags(typed);

            if (customTags.isEmpty()) {
                showMessage("请输入要移除的标签");
                return;
            }

            if (selectedFiles.isEmpty()) {
                showMessage("请先选择要移除标签的文件");
                return;
            }

            performRemove(selectedFiles, new LinkedHashSet<>(customTags), refreshAction);
        });

        JButton refreshButton = new JButton("刷新");
        refreshButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        refreshButton.setPreferredSize(new Dimension(refreshButton.getPreferredSize().width + 20, 36));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshAction.run());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 面板显示后自动聚焦到自定义标签输入框
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                SwingUtilities.invokeLater(input::requestFocusInWindow);
            }
        });

        // 恢复搜索关键字（面板刷新后直接应用过滤，避免闪烁）
        if (!unifiedTagSearchKeyword.isEmpty()) {
            String[] keywords = unifiedTagSearchKeyword.split("\\s+");
            tableModel.setSearchKeywords(keywords);
            highlightRenderer.setKeywords(keywords);
            headerRenderer.setSortState(-1, true);
            searchField.setText(unifiedTagSearchKeyword);
            clearButton.setVisible(true);
            searchPlaceholder.setVisible(false);
            updateStatusBar.run();
        }

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

        // ==================== 中部：标签管理（排序+颜色） ====================
        JPanel tagsContainer = new JPanel(new BorderLayout());
        tagsContainer.setBackground(BG_CONTENT);
        tagsContainer.setBorder(BorderFactory.createTitledBorder("标签管理"));

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
        tagJList.setCellRenderer(new TagManageListCellRenderer());
        tagJList.setDragEnabled(true);
        tagJList.setDropMode(DropMode.INSERT);
        tagJList.setTransferHandler(new TagListTransferHandler(tagListModel));
        tagJList.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 双击设置颜色（仅普通标签）
        tagJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = tagJList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String tag = tagListModel.getElementAt(index);
                        // 特殊标签不可设置颜色
                        boolean isSpecial = FileUtil.TAG_ORDER_FILENAME.equals(tag)
                                || FileUtil.TAG_ORDER_VERSION.equals(tag)
                                || FileUtil.TAG_ORDER_DATE.equals(tag);
                        if (isSpecial) {
                            return;
                        }
                        Color currentColor = TagUtil.getTagColor(tag);
                        Color newColor = JColorChooser.showDialog(panel, "选择标签 \"" + tag + "\" 的颜色", currentColor);
                        if (newColor != null) {
                            TagUtil.setTagColor(tag, newColor);
                            ConfigUtil.save();
                            tagJList.repaint();
                            refreshAction.run();
                        }
                    }
                }
            }
        });

        // 右键菜单：清除颜色 / 重命名 / 删除标签
        JPopupMenu tagMenu = new JPopupMenu();
        JMenuItem clearColorItem = new JMenuItem("恢复自动颜色");
        tagMenu.add(clearColorItem);
        tagMenu.addSeparator();
        JMenuItem renameTagItem = new JMenuItem("重命名");
        renameTagItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        tagMenu.add(renameTagItem);
        tagMenu.addSeparator();
        JMenuItem deleteTagItem = new JMenuItem("删除标签");
        deleteTagItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        tagMenu.add(deleteTagItem);
        String[] rightClickedTag = new String[1];
        clearColorItem.addActionListener(clearAction -> {
            String tag = rightClickedTag[0];
            if (tag != null) {
                TagUtil.clearColor(tag);
                ConfigUtil.save();
                tagJList.repaint();
                refreshAction.run();
            }
        });
        renameTagItem.addActionListener(e -> {
            String oldTag = rightClickedTag[0];
            if (oldTag != null) {
                showTagRenameDialog(oldTag, tagJList, tagListModel, path, refreshAction);
            }
        });
        deleteTagItem.addActionListener(e -> {
            String tag = rightClickedTag[0];
            if (tag == null) {
                return;
            }
            // 弹出确认对话框，防止误操作
            int choice = JOptionPane.showConfirmDialog(
                    tagJList,
                    "确定要删除标签 \"" + tag + "\" 吗？\n此操作仅从配置中移除该标签，不会重命名任何文件。",
                    "确认删除标签",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                // 从全局配置中移除该标签
                Config.tags.remove(tag);
                // 从列表模型中移除
                tagListModel.removeElement(tag);
                // 清除该标签的自定义颜色
                TagUtil.clearColor(tag);
                // 保存配置
                ConfigUtil.save();
                // 刷新界面
                tagJList.repaint();
                refreshAction.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        tagJList,
                        "删除标签时发生错误: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
        tagJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = tagJList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String tag = tagListModel.getElementAt(index);
                        boolean isSpecial = FileUtil.TAG_ORDER_FILENAME.equals(tag)
                                || FileUtil.TAG_ORDER_VERSION.equals(tag)
                                || FileUtil.TAG_ORDER_DATE.equals(tag);
                        if (!isSpecial) {
                            rightClickedTag[0] = tag;
                            tagMenu.show(tagJList, e.getX(), e.getY());
                        }
                    }
                }
            }
        });

        // F2 重命名标签
        tagJList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2) {
                    String selectedTag = tagJList.getSelectedValue();
                    if (selectedTag != null) {
                        boolean isSpecial = FileUtil.TAG_ORDER_FILENAME.equals(selectedTag)
                                || FileUtil.TAG_ORDER_VERSION.equals(selectedTag)
                                || FileUtil.TAG_ORDER_DATE.equals(selectedTag);
                        if (!isSpecial) {
                            showTagRenameDialog(selectedTag, tagJList, tagListModel, path, refreshAction);
                        }
                    }
                }
            }
        });

        // 提示标签
        JLabel tipLabel = new JLabel("提示：拖拽调整标签顺序，双击普通标签设置颜色，右键恢复自动颜色/重命名/删除标签，F2重命名标签");
        tipLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        tipLabel.setForeground(TEXT_GRAY);
        tipLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane tagsScroll = new JScrollPane(tagJList);
        tagsScroll.setBackground(BG_CONTENT);
        tagsContainer.add(tagsScroll, BorderLayout.CENTER);
        tagsContainer.add(tipLabel, BorderLayout.SOUTH);

        // 上下布局：路径 + 包裹符号 + 标签管理
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

        // 包裹在 JScrollPane 中
        JScrollPane centerScroll = new JScrollPane(centerPanel);
        centerScroll.setBackground(BG_CONTENT);
        centerScroll.setBorder(null);
        centerScroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(centerScroll, BorderLayout.CENTER);

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
     * 标签管理列表单元格渲染器，集成排序和颜色设置功能。
     * <p>
     * 普通标签：显示颜色预览块 + 标签名 + 颜色来源
     * 特殊标签：显示说明文字
     * </p>
     */
    private static class TagManageListCellRenderer extends DefaultListCellRenderer {
        private static final Color SPECIAL_BG = new Color(255, 248, 225);
        private static final Color SPECIAL_FG = new Color(180, 130, 0);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String text = value != null ? value.toString() : "";
            boolean isSpecial = FileUtil.TAG_ORDER_FILENAME.equals(text)
                    || FileUtil.TAG_ORDER_VERSION.equals(text)
                    || FileUtil.TAG_ORDER_DATE.equals(text);

            if (isSpecial) {
                // 特殊标签使用简单样式
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GRAY),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)));

                if (isSelected) {
                    // 选中状态保持系统默认
                } else {
                    label.setBackground(SPECIAL_BG);
                    label.setForeground(SPECIAL_FG);
                }

                if (FileUtil.TAG_ORDER_FILENAME.equals(text)) {
                    label.setText("  {文件名}  —  源文件名在标签序列中的位置");
                } else if (FileUtil.TAG_ORDER_VERSION.equals(text)) {
                    label.setText("  {版本号}  —  版本号（如 V1、V2）在标签序列中的位置");
                } else if (FileUtil.TAG_ORDER_DATE.equals(text)) {
                    label.setText("  {当前日期}  —  日期标签（如 20260506）在标签序列中的位置");
                }
                return label;
            }

            // 普通标签使用带颜色预览的样式
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GRAY),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));

            Color bgColor = TagUtil.getTagColor(text);

            // 颜色预览块
            JPanel colorPreview = new JPanel();
            colorPreview.setBackground(bgColor);
            colorPreview.setPreferredSize(new Dimension(24, 24));
            colorPreview.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

            // 标签名
            JLabel tagLabel = new JLabel(text);
            tagLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));

            // 颜色来源提示
            boolean isCustom = TagUtil.hasCustomColor(text);
            JLabel sourceLabel = new JLabel(isCustom ? "自定义" : "自动");
            sourceLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
            sourceLabel.setForeground(Color.GRAY);

            panel.add(colorPreview, BorderLayout.WEST);
            panel.add(tagLabel, BorderLayout.CENTER);
            panel.add(sourceLabel, BorderLayout.EAST);

            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
            } else {
                panel.setBackground(BG_WHITE);
            }

            return panel;
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
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
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

    /**
     * 显示重命名文件对话框
     *
     * @param file        要重命名的文件
     * @param table       文件表格
     * @param tableModel  表格模型
     * @param refreshAction 刷新回调
     */
    private static void showRenameDialog(File file, JTable table, FileTableModel tableModel, Runnable refreshAction) {
        String rawName = file.getName();
        int dot = rawName.lastIndexOf('.');
        String base = dot > 0 ? rawName.substring(0, dot) : rawName;
        String ext = dot > 0 ? rawName.substring(dot) : "";

        // 提取纯文件名（去除标签）
        String cleanName = FileUtil.getAllTagsPattern().matcher(base).replaceAll("").trim();

        // 创建对话框
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(table), "重命名文件", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(BG_CONTENT);

        // 信息面板
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        infoPanel.setBackground(BG_CONTENT);

        JLabel titleLabel = new JLabel("原文件名：" + rawName);
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));

        JLabel tagLabel = new JLabel("标    签：" + (base.equals(cleanName) ? "无" : FileUtil.parseAllTags(rawName).toString()));
        tagLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));

        JLabel tipLabel = new JLabel("提示：标签将自动保留，只需输入纯文件名");
        tipLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        tipLabel.setForeground(TEXT_GRAY);

        infoPanel.add(titleLabel);
        infoPanel.add(tagLabel);
        infoPanel.add(tipLabel);

        // 输入面板
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(BG_CONTENT);

        JTextField nameField = new JTextField(cleanName);
        nameField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.setCaretPosition(nameField.getText().length());

        JLabel extLabel = new JLabel(ext.isEmpty() ? "" : " " + ext);
        extLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));

        inputPanel.add(nameField, BorderLayout.CENTER);
        inputPanel.add(extLabel, BorderLayout.EAST);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton cancelButton = new JButton("取消");
        cancelButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        cancelButton.setPreferredSize(new Dimension(80, 32));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton confirmButton = new JButton("确定");
        confirmButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        confirmButton.setPreferredSize(new Dimension(80, 32));
        confirmButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmButton.setFocusPainted(false);
        confirmButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                showError("文件名不能为空");
                return;
            }

            // 检查文件名是否包含非法字符
            if (newName.matches(".*[\\\\/:*?\"<>|].*")) {
                showError("文件名包含非法字符");
                return;
            }

            // 提取标签
            List<String> tags = FileUtil.parseAllTags(rawName);

            // 构建新文件名：标签 + 纯文件名 + 扩展名
            StringBuilder newFullName = new StringBuilder();
            for (String tag : tags) {
                newFullName.append(Config.getTagWrapLeft()).append(tag).append(Config.getTagWrapRight());
            }
            newFullName.append(newName).append(ext);

            // 执行重命名
            try {
                Path parent = file.toPath().getParent();
                if (parent != null) {
                    Path newPath = parent.resolve(newFullName.toString());
                    if (Files.exists(newPath) && !newPath.equals(file.toPath())) {
                        showError("文件名已存在");
                        return;
                    }
                    Files.move(file.toPath(), newPath);
                    dialog.dispose();
                    refreshAction.run();
                    showSuccess("重命名成功");
                }
            } catch (IOException ex) {
                showError("重命名失败：" + ex.getMessage());
            }
        });

        // 回车键确认 / ESC键取消
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmButton.doClick();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                }
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(table);
        dialog.setVisible(true);
    }

    /**
     * 显示标签重命名对话框。
     * 重命名后自动更新配置中的标签名，并将当前目录下所有文件中的旧标签替换为新标签。
     *
     * @param oldTag       旧标签名
     * @param tagJList     标签列表组件
     * @param tagListModel 标签列表模型
     * @param dirPath      当前目录路径
     * @param refreshAction 刷新回调
     */
    private static void showTagRenameDialog(String oldTag, JList<String> tagJList, DefaultListModel<String> tagListModel, String dirPath, Runnable refreshAction) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(tagJList), "重命名标签", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(BG_CONTENT);

        // 信息面板
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        infoPanel.setBackground(BG_CONTENT);

        JLabel titleLabel = new JLabel("原标签名：" + oldTag);
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));

        JLabel tipLabel = new JLabel("提示：重命名后自动更新目录下所有文件中的旧标签");
        tipLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        tipLabel.setForeground(TEXT_GRAY);

        infoPanel.add(titleLabel);
        infoPanel.add(tipLabel);

        // 输入面板
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(BG_CONTENT);

        JTextField nameField = new JTextField(oldTag);
        nameField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.setCaretPosition(nameField.getText().length());

        inputPanel.add(nameField, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_CONTENT);

        JButton cancelButton = new JButton("取消");
        cancelButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        cancelButton.setPreferredSize(new Dimension(80, 32));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton confirmButton = new JButton("确定");
        confirmButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        confirmButton.setPreferredSize(new Dimension(80, 32));
        confirmButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmButton.setFocusPainted(false);
        confirmButton.addActionListener(e -> {
            String newTag = nameField.getText().trim();
            if (newTag.isEmpty()) {
                showError("标签名不能为空");
                return;
            }
            if (newTag.equals(oldTag)) {
                dialog.dispose();
                return;
            }
            if (newTag.matches(".*[\\\\/:*?\"<>|\\[\\]【】].*")) {
                showError("标签名包含非法字符");
                return;
            }

            // 更新配置中的标签名
            ConfigUtil.reload();
            List<String> configTags = new ArrayList<>(Config.tags);
            boolean found = false;
            for (int i = 0; i < configTags.size(); i++) {
                if (configTags.get(i).equals(oldTag)) {
                    configTags.set(i, newTag);
                    found = true;
                    break;
                }
            }
            if (!found) {
                dialog.dispose();
                return;
            }
            Config.tags = configTags;

            // 更新自定义颜色映射
            if (TagUtil.hasCustomColor(oldTag)) {
                Color color = TagUtil.getTagColor(oldTag);
                TagUtil.setTagColor(newTag, color);
                TagUtil.clearColor(oldTag);
            }

            ConfigUtil.save();

            // 更新列表模型
            for (int i = 0; i < tagListModel.size(); i++) {
                if (tagListModel.get(i).equals(oldTag)) {
                    tagListModel.set(i, newTag);
                    break;
                }
            }

            // 批量替换目录下所有文件中的旧标签
            File dir = new File(dirPath);
            File[] files = dir.listFiles();
            int renamed = 0;
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            if (FileUtil.replaceTag(file.toPath(), oldTag, newTag)) {
                                renamed++;
                            }
                        } catch (IOException ex) {
                            // 单个文件失败不影响其他文件
                        }
                    }
                }
            }

            dialog.dispose();
            refreshAction.run();
            showSuccess("标签已重命名，已更新 " + renamed + " 个文件");
        });

        // 回车键确认 / ESC键取消
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmButton.doClick();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                }
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(tagJList);
        dialog.setVisible(true);
    }

    /**
     * 使用系统默认程序打开文件。
     *
     * @param file 要打开的文件
     */
    private static void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showError("当前系统不支持自动打开文件");
            }
        } catch (Exception ex) {
            showError("无法打开文件: " + ex.getMessage());
        }
    }
}
