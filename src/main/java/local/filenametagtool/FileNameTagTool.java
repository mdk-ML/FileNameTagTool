package local.filenametagtool;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class FileNameTagTool {
    private static final int PORT = 45678;
    private static final int COLLECT_WINDOW_MS = 450;
    private static final Pattern LEADING_TAGS_PATTERN = Pattern.compile("^(?:【[^】]*】)+");
    private static final String ACTION_LINE_PREFIX = "ACTION:";

    private static final String CONFIG_FILE_NAME = "filename-tagtool.conf";
    private static final String CFG_WINDOW_X = "window.x";
    private static final String CFG_WINDOW_Y = "window.y";
    private static final String CFG_WINDOW_W = "window.w";
    private static final String CFG_WINDOW_H = "window.h";
    private static final String CFG_DIVIDER = "ui.divider";
    private static final String CFG_TAG = "tag";

    private enum Action {
        ADD("add"),
        REMOVE_ALL("removeAll"),
        REMOVE("remove"),
        NEW_VERSION("newVersion"),
        COPY_WITHOUT_TAGS("copyWithoutTags"),
        SEARCH("search");

        final String arg;

        Action(String arg) {
            this.arg = arg;
        }

        static Action fromArg(String s) {
            if (s == null) return null;
            for (Action a : values()) {
                if (a.arg.equalsIgnoreCase(s.trim())) return a;
            }
            return null;
        }
    }

    private static final class Parsed {
        final Action action;
        final List<String> paths;

        Parsed(Action action, List<String> paths) {
            this.action = action;
            this.paths = paths;
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        final Parsed parsed = parseArgs(args);
        if (parsed.action == null) {
            showMessage("缺少动作参数，请用：add | removeAll | newVersion | copyWithoutTags。", "提示");
            return;
        }

        try (ServerSocket server = tryBindServer()) {
            if (server == null) {
                sendArgsToPrimary(parsed);
                return;
            }

            final AtomicReference<Action> actionRef = new AtomicReference<>(parsed.action);
            final Set<String> paths = ConcurrentHashMap.newKeySet();
            paths.addAll(parsed.paths);

            final ExecutorService acceptor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "filename-tagtool-acceptor");
                t.setDaemon(true);
                return t;
            });

            acceptor.submit(() -> acceptLoop(server, actionRef, paths));

            sleepSilently(COLLECT_WINDOW_MS);

            try {
                server.close();
            } catch (IOException ignored) {
            }
            acceptor.shutdownNow();

            final List<Path> existing = paths.stream()
                                             .map(String::trim)
                                             .filter(s -> !s.isEmpty())
                                             .map(FileNameTagTool::safeToPath)
                                             .filter(Objects::nonNull)
                                             .filter(p -> Files.exists(p, LinkOption.NOFOLLOW_LINKS))
                                             .distinct()
                                             .toList();

            if (existing.isEmpty()) {
                showMessage("没有获取到有效的文件/文件夹路径。请先在资源管理器中选中后再点击菜单。", "提示");
                return;
            }

            final Action action = actionRef.get();
            if (action == null) {
                showMessage("无法确定动作（add/removeAll/remove）。", "错误");
                return;
            }

            if (action == Action.SEARCH) {
                groupTags(existing);
                return;
            }

            final List<String> addTags;
            final Set<String> removeTags;
            if (action == Action.ADD) {
                final Object[] result = askTagsWithHistory();
                if (result == null) return;

                final List<String> tags = (List<String>) result[0];
                final Set<String> smartTags = (Set<String>) result[1];

                final List<String> normalized = normalizeTags(tags);
                if (normalized.isEmpty()) return;

                rememberTags(normalized, smartTags);
                addTags = normalized;
                removeTags = null;
            } else if (action == Action.REMOVE) {
                removeTags = askTagsToRemove(existing);
                if (removeTags == null) return;
                addTags = null;
            } else {
                addTags = null;
                removeTags = null;
            }

            int renamed = 0;
            int skipped = 0;
            for (Path p : existing) {
                try {
                    final boolean ok;
                    if (action == Action.ADD) {
                        ok = addTagsToNamePrefix(p, addTags);
                    } else if (action == Action.REMOVE) {
                        ok = removeTags(p, removeTags);
                    } else if (action == Action.NEW_VERSION) {
                        ok = createNewVersion(p);
                    } else if (action == Action.COPY_WITHOUT_TAGS) {
                        ok = copyWithoutTags(p);
                    } else {
                        ok = removeAllTags(p);
                    }
                    if (ok) renamed++;
                    else skipped++;
                } catch (Exception e) {
                    skipped++;
                }
            }

            showMessage("选择项：" + existing.size() + "\n成功重命名：" + renamed + "\n跳过/失败：" + skipped, "完成");
        } catch (Exception e) {
            showMessage(String.valueOf(e), "错误");
        }
    }

    private static ServerSocket tryBindServer() {
        try {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT));
            return ss;
        } catch (IOException e) {
            return null;
        }
    }

    private static void acceptLoop(ServerSocket server, AtomicReference<Action> actionRef, Set<String> paths) {
        while (!server.isClosed()) {
            try (Socket s = server.accept()) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String v = line.trim();
                        if (v.isEmpty()) continue;
                        if (v.startsWith(ACTION_LINE_PREFIX)) {
                            Action incoming = Action.fromArg(v.substring(ACTION_LINE_PREFIX.length()));
                            if (incoming != null) {
                                actionRef.compareAndSet(null, incoming);
                            }
                            continue;
                        }
                        paths.add(v);
                    }
                }
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private static void sendArgsToPrimary(Parsed parsed) {
        if (parsed == null) return;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT), 120);
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
                if (parsed.action != null) {
                    bw.write(ACTION_LINE_PREFIX + parsed.action.arg);
                    bw.newLine();
                }
                for (String p : parsed.paths) {
                    bw.write(p);
                    bw.newLine();
                }
                bw.flush();
            }
        } catch (IOException ignored) {
        }
    }

    private static Parsed parseArgs(String[] args) {
        if (args == null || args.length == 0) return new Parsed(null, List.of());
        Action action = Action.fromArg(args[0]);
        List<String> paths = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a == null) continue;
            String v = a.trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            if (!v.isEmpty()) paths.add(v);
        }
        return new Parsed(action, paths);
    }

    private static Path safeToPath(String s) {
        try {
            return Paths.get(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object[] askTagsWithHistory() {
        AppConfig cfg = loadConfig();
        List<String> history = new ArrayList<>(cfg.tags);

        final JTextArea input = new JTextArea();
        input.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
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
            b.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
            b.setForeground(TEXT_DARK);
            b.setBackground(BG_WHITE);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
            b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
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
                    String updatedText = currentText.replaceAll("(,\\s*)?" + Pattern.quote(t), "");
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

        JScrollPane tagsScroll = new JScrollPane(tagsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBackground(BG_WHITE);

        final Set<String> smartTags = new HashSet<>();

        JPanel smartTagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        smartTagsPanel.setBackground(BG_WHITE);

        JPanel dateTagPanel = new JPanel();
        dateTagPanel.setLayout(new BoxLayout(dateTagPanel, BoxLayout.Y_AXIS));
        dateTagPanel.setBackground(new Color(187, 222, 251));
        dateTagPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        dateTagPanel.setPreferredSize(new Dimension(180, 80));
        JLabel dateIcon = new JLabel("📅");
        dateIcon.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
        dateIcon.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel dateTitle = new JLabel("当前日期");
        dateTitle.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        dateTitle.setForeground(TEXT_DARK);
        dateTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        JLabel dateValue = new JLabel(dateStr);
        dateValue.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 12));
        dateValue.setForeground(new Color(70, 70, 70));
        dateValue.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        dateTagPanel.add(dateIcon);
        dateTagPanel.add(Box.createVerticalStrut(4));
        dateTagPanel.add(dateTitle);
        dateTagPanel.add(Box.createVerticalStrut(4));
        dateTagPanel.add(dateValue);
        dateTagPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        dateTagPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String dateTag = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                smartTags.add(dateTag);
                String currentText = input.getText();
                if (currentText.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈") || currentText.trim()
                                                                                                                              .isEmpty()) {
                    input.setText(dateTag);
                    input.setForeground(TEXT_DARK);
                } else {
                    String lastChar = currentText.substring(currentText.length() - 1);
                    if (lastChar.equals(" ") || lastChar.equals("\n")) {
                        input.setText(currentText + dateTag);
                    } else {
                        input.setText(currentText + " " + dateTag);
                    }
                }
                input.requestFocusInWindow();
            }
        });
        smartTagsPanel.add(dateTagPanel);

        JPanel recentTagPanel = new JPanel();
        recentTagPanel.setLayout(new BoxLayout(recentTagPanel, BoxLayout.Y_AXIS));
        recentTagPanel.setBackground(new Color(197, 239, 204));
        recentTagPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        recentTagPanel.setPreferredSize(new Dimension(180, 80));
        JLabel recentIcon = new JLabel("🔄");
        recentIcon.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
        recentIcon.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel recentTitle = new JLabel("最近使用");
        recentTitle.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        recentTitle.setForeground(TEXT_DARK);
        recentTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel recentValue = new JLabel("工作, 重要");
        recentValue.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 12));
        recentValue.setForeground(new Color(70, 70, 70));
        recentValue.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        recentTagPanel.add(recentIcon);
        recentTagPanel.add(Box.createVerticalStrut(4));
        recentTagPanel.add(recentTitle);
        recentTagPanel.add(Box.createVerticalStrut(4));
        recentTagPanel.add(recentValue);
        recentTagPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        recentTagPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                smartTags.add("工作");
                smartTags.add("重要");
                String currentText = input.getText();
                if (currentText.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈") || currentText.trim()
                                                                                                                              .isEmpty()) {
                    input.setText("工作 重要");
                    input.setForeground(TEXT_DARK);
                } else {
                    String lastChar = currentText.substring(currentText.length() - 1);
                    if (lastChar.equals(" ") || lastChar.equals("\n")) {
                        input.setText(currentText + "工作 重要");
                    } else {
                        input.setText(currentText + " 工作 重要");
                    }
                }
                input.requestFocusInWindow();
            }
        });
        smartTagsPanel.add(recentTagPanel);

        JPanel hotTagPanel = new JPanel();
        hotTagPanel.setLayout(new BoxLayout(hotTagPanel, BoxLayout.Y_AXIS));
        hotTagPanel.setBackground(new Color(245, 191, 231));
        hotTagPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        hotTagPanel.setPreferredSize(new Dimension(180, 80));
        JLabel hotIcon = new JLabel("🔥");
        hotIcon.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
        hotIcon.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel hotTitle = new JLabel("热门标签");
        hotTitle.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        hotTitle.setForeground(TEXT_DARK);
        hotTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel hotValue = new JLabel("项目A 会议");
        hotValue.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 12));
        hotValue.setForeground(new Color(70, 70, 70));
        hotValue.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        hotTagPanel.add(hotIcon);
        hotTagPanel.add(Box.createVerticalStrut(4));
        hotTagPanel.add(hotTitle);
        hotTagPanel.add(Box.createVerticalStrut(4));
        hotTagPanel.add(hotValue);
        hotTagPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        hotTagPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                smartTags.add("项目A");
                smartTags.add("会议");
                String currentText = input.getText();
                if (currentText.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈") || currentText.trim()
                                                                                                                              .isEmpty()) {
                    input.setText("项目A 会议");
                    input.setForeground(TEXT_DARK);
                } else {
                    String lastChar = currentText.substring(currentText.length() - 1);
                    if (lastChar.equals(" ") || lastChar.equals("\n")) {
                        input.setText(currentText + "项目A 会议");
                    } else {
                        input.setText(currentText + " 项目A 会议");
                    }
                }
                input.requestFocusInWindow();
            }
        });
        smartTagsPanel.add(hotTagPanel);

        JPanel recommendTagPanel = new JPanel();
        recommendTagPanel.setLayout(new BoxLayout(recommendTagPanel, BoxLayout.Y_AXIS));
        recommendTagPanel.setBackground(new Color(255, 229, 180));
        recommendTagPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        recommendTagPanel.setPreferredSize(new Dimension(180, 80));
        JLabel recommendIcon = new JLabel("💡");
        recommendIcon.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
        recommendIcon.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel recommendTitle = new JLabel("推荐标签");
        recommendTitle.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        recommendTitle.setForeground(TEXT_DARK);
        recommendTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel recommendValue = new JLabel("文档 计划");
        recommendValue.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 12));
        recommendValue.setForeground(new Color(70, 70, 70));
        recommendValue.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        recommendTagPanel.add(recommendIcon);
        recommendTagPanel.add(Box.createVerticalStrut(4));
        recommendTagPanel.add(recommendTitle);
        recommendTagPanel.add(Box.createVerticalStrut(4));
        recommendTagPanel.add(recommendValue);
        recommendTagPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        recommendTagPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                smartTags.add("文档");
                smartTags.add("计划");
                String currentText = input.getText();
                if (currentText.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈") || currentText.trim()
                                                                                                                              .isEmpty()) {
                    input.setText("文档 计划");
                    input.setForeground(TEXT_DARK);
                } else {
                    String lastChar = currentText.substring(currentText.length() - 1);
                    if (lastChar.equals(" ") || lastChar.equals("\n")) {
                        input.setText(currentText + "文档 计划");
                    } else {
                        input.setText(currentText + " 文档 计划");
                    }
                }
                input.requestFocusInWindow();
            }
        });
        smartTagsPanel.add(recommendTagPanel);

        JPanel inputLabelPanel = new JPanel();
        inputLabelPanel.setLayout(new BoxLayout(inputLabelPanel, BoxLayout.Y_AXIS));
        inputLabelPanel.setBackground(BG_WHITE);
        JLabel inputLabel = new JLabel("自定义标签内容");
        inputLabel.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        inputLabel.setForeground(TEXT_DARK);
        inputLabelPanel.add(inputLabel);
        inputLabelPanel.add(Box.createVerticalStrut(4));
        JLabel inputHint = new JLabel("请输入或要增加的标签内容（支持多个标签，用空格分隔）：");
        inputHint.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 12));
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
        ok.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 14));
        ok.setForeground(TEXT_LIGHT);
        ok.setBackground(GRADIENT_START);
        ok.setFocusPainted(false);
        ok.setOpaque(true);
        ok.setBorderPainted(false);
        ok.setPreferredSize(new Dimension(100, 40));
        ok.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        cancel.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 14));
        cancel.setForeground(TEXT_DARK);
        cancel.setBackground(BG_WHITE);
        cancel.setFocusPainted(false);
        cancel.setOpaque(true);
        cancel.setBorderPainted(true);
        cancel.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
        cancel.setPreferredSize(new Dimension(100, 40));
        cancel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        final JDialog dialog = new JDialog((Frame) null, "", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(BG_WHITE);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new java.awt.BorderLayout());
        headerPanel.setBackground(BG_WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        headerPanel.setPreferredSize(new Dimension(600, 60));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setBackground(BG_WHITE);
        JLabel iconLabel = new JLabel("💎");
        iconLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 20));
        JLabel titleLabel = new JLabel("增加标签");
        titleLabel.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.BOLD, 18));
        titleLabel.setForeground(TEXT_DARK);
        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        JPanel closePanel = new JPanel();
        closePanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closePanel.setBackground(BG_WHITE);
        JButton closeButton = new JButton("×");
        closeButton.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 18));
        closeButton.setForeground(TEXT_DARK);
        closeButton.setBackground(BG_WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(true);
        closeButton.setBorderPainted(false);
        closeButton.setPreferredSize(new Dimension(30, 30));
        closeButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            input.setText("");
            input.putClientProperty("cancelled", Boolean.TRUE);
            dialog.dispose();
        });
        closePanel.add(closeButton);

        headerPanel.add(titlePanel, java.awt.BorderLayout.WEST);
        headerPanel.add(closePanel, java.awt.BorderLayout.EAST);

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
        contentPanel.setLayout(new java.awt.BorderLayout());
        contentPanel.setBackground(BG_WHITE);
        contentPanel.add(headerPanel, java.awt.BorderLayout.NORTH);
        contentPanel.add(split, java.awt.BorderLayout.CENTER);

        dialog.getContentPane().add(contentPanel);

        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(800, 600));

        if (cfg.windowW > 0 && cfg.windowH > 0) {
            dialog.setBounds(cfg.windowX, cfg.windowY, cfg.windowW, cfg.windowH);
        } else {
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setSize(new Dimension(840, 680));
        }
        if (cfg.divider > 0) split.setDividerLocation(cfg.divider);
        else split.setDividerLocation(0.5);

        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            input.selectAll();
        });

        cfg.windowX = dialog.getX();
        cfg.windowY = dialog.getY();
        cfg.windowW = dialog.getWidth();
        cfg.windowH = dialog.getHeight();
        cfg.divider = split.getDividerLocation();
        saveConfig(cfg);

        Object cancelled = input.getClientProperty("cancelled");
        if (Boolean.TRUE.equals(cancelled)) return null;

        String typed = input.getText();
        if (typed.equals("输入自定义标签，多个标签请用空格分隔，例如：紧急任务 Q2季度报告 客户反馈")) {
            typed = "";
        }
        List<String> tags = splitTags(typed);
        return new Object[]{tags, smartTags};
    }

    private static Set<String> askTagsToRemove(List<Path> files) {
        Set<String> existingFileTags = new LinkedHashSet<>();
        for (Path file : files) {
            Path fileName = file.getFileName();
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
            b.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
            b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

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

        JScrollPane tagsScroll = new JScrollPane(tagsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tagsScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0),
                "文件标签（单击标记待移除，双击直接移除）",
                javax.swing.SwingConstants.LEFT,
                javax.swing.SwingConstants.TOP,
                new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13),
                TEXT_DARK
        ));
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

    private static String joinSelectedTags(List<JToggleButton> toggles) {
        StringBuilder sb = new StringBuilder();
        for (JToggleButton tb : toggles) {
            if (!tb.isSelected()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(tb.getText());
        }
        if (sb.length() > 0) sb.append(' ');
        return sb.toString();
    }

    private static List<String> splitTags(String typed) {
        if (typed == null) return new ArrayList<>();
        String s = typed.trim();
        if (s.isEmpty()) return new ArrayList<>();
        String[] parts = s.split("\\s+");
        return new ArrayList<>(Arrays.asList(parts));
    }

    private static boolean addTagsToNamePrefix(Path path, List<String> addTags) throws IOException {
        if (addTags == null || addTags.isEmpty()) return false;

        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = addTagsToLeafPreserveExt(leaf, addTags);
        if (newLeaf.equals(leaf)) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.move(path, target);
        return true;
    }

    private static boolean removeAllTags(Path path) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = removeTagsFromLeafPreserveExt(leaf);
        if (newLeaf.equals(leaf)) return false;
        if (newLeaf.trim().isEmpty()) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.move(path, target);
        return true;
    }

    private static boolean removeTags(Path path, Set<String> tagsToRemove) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = removeSpecificTagsFromLeafPreserveExt(leaf, tagsToRemove);
        if (newLeaf.equals(leaf)) return false;
        if (newLeaf.trim().isEmpty()) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.move(path, target);
        return true;
    }

    private static String removeSpecificTagsFromLeafPreserveExt(String leaf, Set<String> tagsToRemove) {
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            String base = leaf.substring(0, dot);
            String ext = leaf.substring(dot);
            String cleaned = removeSpecificLeadingTags(base, tagsToRemove);
            return cleaned + ext;
        }
        return removeSpecificLeadingTags(leaf, tagsToRemove);
    }

    private static String removeSpecificLeadingTags(String name, Set<String> tagsToRemove) {
        List<String> existingTags = parseLeadingTags(name);
        List<String> remainingTags = new ArrayList<>();
        for (String tag : existingTags) {
            if (!tagsToRemove.contains(tag)) {
                remainingTags.add(tag);
            }
        }
        if (remainingTags.isEmpty()) {
            return LEADING_TAGS_PATTERN.matcher(name).replaceFirst("");
        }
        String prefix = buildTagPrefix(remainingTags);
        String rest = LEADING_TAGS_PATTERN.matcher(name).replaceFirst("");
        return prefix + rest;
    }

    private static String addTagsToLeafPreserveExt(String leaf, List<String> addTags) {
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            String base = leaf.substring(0, dot);
            String ext = leaf.substring(dot);
            return addTagsToBase(base, addTags) + ext;
        }
        return addTagsToBase(leaf, addTags);
    }

    private static String addTagsToBase(String base, List<String> addTags) {
        List<String> existing = parseLeadingTags(base);
        String rest = removeLeadingTags(base);

        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String t : addTags) {
            if (!containsIgnoreCase(merged, t)) merged.add(t);
        }
        for (String t : existing) {
            if (!containsIgnoreCase(merged, t)) merged.add(t);
        }

        String prefix = buildTagPrefix(new ArrayList<>(merged));
        return prefix + rest;
    }

    private static boolean containsIgnoreCase(Iterable<String> list, String value) {
        for (String s : list) {
            if (s != null && value != null && s.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

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

    private static String removeTagsFromLeafPreserveExt(String leaf) {
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            String base = leaf.substring(0, dot);
            String ext = leaf.substring(dot);
            String cleaned = removeLeadingTags(base);
            return cleaned + ext;
        }
        return removeLeadingTags(leaf);
    }

    private static String removeLeadingTags(String name) {
        return LEADING_TAGS_PATTERN.matcher(name).replaceFirst("");
    }

    private static void rememberTags(List<String> tags, Set<String> smartTags) {
        if (tags == null || tags.isEmpty()) return;

        List<String> incoming = normalizeTags(tags);
        if (incoming.isEmpty()) return;

        List<String> filteredIncoming = new ArrayList<>();
        for (String tag : incoming) {
            if (smartTags == null || !smartTags.contains(tag)) {
                filteredIncoming.add(tag);
            }
        }
        incoming = filteredIncoming;
        if (incoming.isEmpty()) return;

        AppConfig cfg = loadConfig();
        List<String> old = new ArrayList<>(cfg.tags);
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String t : old) {
            if (t == null) continue;
            String tt = t.trim();
            if (tt.isEmpty()) continue;
            boolean isDup = false;
            for (String v : incoming) {
                if (tt.equalsIgnoreCase(v)) {
                    isDup = true;
                    break;
                }
            }
            if (!isDup) merged.add(tt);
        }
        merged.addAll(incoming);

        cfg.tags = new ArrayList<>(merged);
        saveConfig(cfg);
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String t : tags) {
            if (t == null) continue;
            String v = t.trim();
            if (v.isEmpty()) continue;
            v = v.replace("【", "").replace("】", "").trim();
            if (v.isEmpty()) continue;
            out.add(v);
        }
        return new ArrayList<>(out);
    }

    private static final class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(java.awt.Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(java.awt.Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(java.awt.Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) {
                    java.awt.Container parent = target.getParent();
                    if (parent != null) targetWidth = parent.getSize().width;
                }
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap();
                int vgap = getVgap();
                java.awt.Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    java.awt.Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;

                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    if (rowWidth + d.width > maxWidth) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) rowWidth += hgap;
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }

                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight;

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }

    private static String buildTagPrefix(List<String> tags) {
        StringBuilder sb = new StringBuilder();
        for (String t : tags) {
            sb.append("【").append(t).append("】");
        }
        return sb.toString();
    }

    private static final class AppConfig {
        int windowX;
        int windowY;
        int windowW;
        int windowH;
        int divider;
        int groupTagsWindowX;
        int groupTagsWindowY;
        int groupTagsWindowWidth;
        int groupTagsWindowHeight;
        List<String> tags = new ArrayList<>();
    }

    private static AppConfig loadConfig() {
        AppConfig cfg = new AppConfig();
        Path file = configFilePath();
        if (!Files.exists(file)) return cfg;

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null) continue;
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#") || s.startsWith(";")) continue;
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String k = s.substring(0, eq).trim();
                String v = s.substring(eq + 1).trim();

                if (CFG_TAG.equalsIgnoreCase(k)) {
                    if (!v.isEmpty()) cfg.tags.add(v);
                    continue;
                }

                if (CFG_WINDOW_X.equalsIgnoreCase(k)) cfg.windowX = parseIntSafe(v);
                else if (CFG_WINDOW_Y.equalsIgnoreCase(k)) cfg.windowY = parseIntSafe(v);
                else if (CFG_WINDOW_W.equalsIgnoreCase(k)) cfg.windowW = parseIntSafe(v);
                else if (CFG_WINDOW_H.equalsIgnoreCase(k)) cfg.windowH = parseIntSafe(v);
                else if (CFG_DIVIDER.equalsIgnoreCase(k)) cfg.divider = parseIntSafe(v);
                else if ("groupTagsWindowX".equalsIgnoreCase(k)) cfg.groupTagsWindowX = parseIntSafe(v);
                else if ("groupTagsWindowY".equalsIgnoreCase(k)) cfg.groupTagsWindowY = parseIntSafe(v);
                else if ("groupTagsWindowWidth".equalsIgnoreCase(k)) cfg.groupTagsWindowWidth = parseIntSafe(v);
                else if ("groupTagsWindowHeight".equalsIgnoreCase(k)) cfg.groupTagsWindowHeight = parseIntSafe(v);
            }
        } catch (Exception ignored) {
        }

        cfg.tags = normalizeTags(cfg.tags);
        return cfg;
    }

    private static void saveConfig(AppConfig cfg) {
        if (cfg == null) return;
        try {
            Path file = configFilePath();
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            List<String> out = new ArrayList<>();
            out.add("# FileNameTagTool config (auto-generated)");
            out.add(CFG_WINDOW_X + "=" + cfg.windowX);
            out.add(CFG_WINDOW_Y + "=" + cfg.windowY);
            out.add(CFG_WINDOW_W + "=" + cfg.windowW);
            out.add(CFG_WINDOW_H + "=" + cfg.windowH);
            out.add(CFG_DIVIDER + "=" + cfg.divider);
            out.add("groupTagsWindowX=" + cfg.groupTagsWindowX);
            out.add("groupTagsWindowY=" + cfg.groupTagsWindowY);
            out.add("groupTagsWindowWidth=" + cfg.groupTagsWindowWidth);
            out.add("groupTagsWindowHeight=" + cfg.groupTagsWindowHeight);
            out.add("");
            for (String t : normalizeTags(cfg.tags)) {
                out.add(CFG_TAG + "=" + t);
            }

            Files.write(file, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    private static int parseIntSafe(String v) {
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return 0;
        }
    }

    private static Path configFilePath() {
        return getAppDir().resolve(CONFIG_FILE_NAME);
    }

    private static final Color GRADIENT_START = new Color(74, 144, 226);
    private static final Color GRADIENT_END = new Color(123, 104, 238);
    private static final Color BG_LIGHT = new Color(248, 249, 250);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BORDER_GRAY = new Color(220, 220, 220);
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color TEXT_LIGHT = new Color(255, 255, 255);

    private static void stylePrimaryButton(AbstractButton b) {
        b.setForeground(TEXT_LIGHT);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(120, 44));
        b.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 14));
        b.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
    }

    private static void styleSecondaryButton(AbstractButton b) {
        b.setBackground(BG_WHITE);
        b.setForeground(GRADIENT_START);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setBorder(BorderFactory.createLineBorder(GRADIENT_START, 2, true));
        b.setPreferredSize(new Dimension(120, 44));
        b.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 14));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GRADIENT_START, 2, true),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));
    }

    private static void groupTags(List<Path> paths) {
        EverythingSearcher searcher = EverythingSearcher.getInstance();
        if (!searcher.isEverythingRunning()) {
            showMessage("错误：Everything 客户端未运行，请先启动 Everything", "搜索标签");
            return;
        }
        List<EverythingSearcher.SearchResult> results = searcher.search("【 】", paths.get(0).toString());

        java.util.Map<String, Integer> tagCount = new java.util.LinkedHashMap<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("【([^】]+)】");
        for (EverythingSearcher.SearchResult result : results) {
            String fileName = result.getFileName();
            java.util.regex.Matcher matcher = pattern.matcher(fileName);
            while (matcher.find()) {
                String tag = matcher.group(1);
                tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
            }
        }

        AppConfig cfg = loadConfig();
        String currentPath = paths.get(0).toString();


        JFrame frame = new JFrame(currentPath);
        List<Image> icons = new ArrayList<>();
        icons.add(new ImageIcon("C:\\Users\\MU\\Documents\\FileNameTagTool\\ico\\tags-16.png").getImage());
        icons.add(new ImageIcon("C:\\Users\\MU\\Documents\\FileNameTagTool\\ico\\tags-32.png").getImage());
        icons.add(new ImageIcon("C:\\Users\\MU\\Documents\\FileNameTagTool\\ico\\tags-48.png").getImage());
        icons.add(new ImageIcon("C:\\Users\\MU\\Documents\\FileNameTagTool\\ico\\tags-64.png").getImage());
        frame.setIconImages(icons);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(true);
        
        if (cfg.groupTagsWindowWidth > 0 && cfg.groupTagsWindowHeight > 0) {
            frame.setSize(cfg.groupTagsWindowWidth, cfg.groupTagsWindowHeight);
        } else {
            frame.setSize(450, 400);
        }

        if (cfg.groupTagsWindowX > 0 && cfg.groupTagsWindowY > 0) {
            frame.setLocation(cfg.groupTagsWindowX, cfg.groupTagsWindowY);
        } else {
            frame.setLocationRelativeTo(null);
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(BG_WHITE);

        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.setBackground(BG_WHITE);
        mainPanel.add(pathPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(BG_WHITE);

        java.util.List<JToggleButton> toggleButtons = new java.util.ArrayList<>();

        if (tagCount.isEmpty()) {
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            JLabel emptyLabel = new JLabel("未找到任何标签");
            emptyLabel.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(102, 102, 102));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(Box.createVerticalStrut(20));
            contentPanel.add(emptyLabel);
        } else {
            contentPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 12, 12));

            java.util.List<java.util.Map.Entry<String, Integer>> sortedTags = new java.util.ArrayList<>(tagCount.entrySet());
            sortedTags.sort(java.util.Map.Entry.comparingByValue());

            for (java.util.Map.Entry<String, Integer> entry : sortedTags) {
                String tag = entry.getKey();
                int count = entry.getValue();

                String tagText = tag + " (" + count + ")";
                JToggleButton toggleButton = new JToggleButton(tagText);
                toggleButton.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
                toggleButton.setForeground(TEXT_DARK);
                toggleButton.setBackground(BG_WHITE);
                toggleButton.setBorderPainted(true);
                toggleButton.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
                toggleButton.setFocusPainted(false);
                toggleButton.setOpaque(true);
                toggleButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                // toggleButton.setSize(toggleButton.getPreferredSize());
                Dimension d = toggleButton.getPreferredSize();
                toggleButton.setPreferredSize(new Dimension(d.width + 20, 36));
                toggleButton.setHorizontalAlignment(SwingConstants.CENTER);
                toggleButtons.add(toggleButton);

                toggleButton.addItemListener(e -> {
                    styleTagToggle(toggleButton, toggleButton.isSelected());
                });

                toggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            for (JToggleButton btn : toggleButtons) {
                                if (btn != toggleButton && btn.isSelected()) {
                                    btn.setSelected(false);
                                    styleTagToggle(btn, false);
                                }
                            }
                            String searchQuery = currentPath + " 【" + tag + "】";
                            try {
                                EverythingSearcher.launchEverythingUI(searchQuery);
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
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        scrollPane.setBackground(BG_WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BG_WHITE);

        JButton searchButton = new JButton("搜索选中");
        searchButton.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        searchButton.setForeground(Color.WHITE);
        searchButton.setBackground(GRADIENT_START);
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> {
            java.util.List<String> selectedTags = new java.util.ArrayList<>();
            for (JToggleButton toggleButton : toggleButtons) {
                if (toggleButton.isSelected()) {
                    String tagText = toggleButton.getText();
                    if (tagText.contains(" (")) {
                        String tag = tagText.substring(0, tagText.indexOf(" ("));
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
                    EverythingSearcher.launchEverythingUI(queryBuilder.toString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JButton refreshButton = new JButton("刷新");
        refreshButton.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(GRADIENT_START);
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> {
            saveWindowPosition(frame);
            frame.dispose();
            groupTags(paths);
        });


        buttonPanel.add(searchButton);
        buttonPanel.add(refreshButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveWindowPosition(frame);
            }
        });

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    private static void saveWindowPosition(Window window) {
        AppConfig cfg = loadConfig();
        cfg.groupTagsWindowX = window.getX();
        cfg.groupTagsWindowY = window.getY();
        cfg.groupTagsWindowWidth = window.getWidth();
        cfg.groupTagsWindowHeight = window.getHeight();
        saveConfig(cfg);
    }

    private static void styleTagToggle(JToggleButton b, boolean selected) {
        if (selected) {
            b.setForeground(TEXT_LIGHT);
            b.setBackground(GRADIENT_START);
            b.setBorderPainted(false);
        } else {
            b.setForeground(TEXT_DARK);
            b.setBackground(BG_WHITE);
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
        }
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private static void styleSmartTagButton(JButton b) {
        b.setForeground(TEXT_DARK);
        b.setBackground(BG_WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
        b.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 12));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private static Path getAppDir() {
        try {
            URL url = FileNameTagTool.class.getProtectionDomain().getCodeSource().getLocation();
            if (url != null) {
                Path p = Paths.get(url.toURI());
                if (Files.isRegularFile(p)) {
                    Path parent = p.getParent();
                    if (parent != null) return parent;
                }
                if (Files.isDirectory(p)) {
                    return p;
                }
            }
        } catch (Exception ignored) {
        }
        return Paths.get(System.getProperty("user.dir", "."));
    }

    private static Path ensureNonExisting(Path target) {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return target;

        Path parent = target.getParent();
        String leaf = target.getFileName().toString();

        String base = leaf;
        String ext = "";
        int dot = leaf.lastIndexOf('.');
        if (dot > 0) {
            base = leaf.substring(0, dot);
            ext = leaf.substring(dot);
        }

        for (int i = 1; i < 10000; i++) {
            Path candidate = parent.resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) return candidate;
        }
        return target;
    }

    private static void showMessage(String msg, String title) {
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
        titleLabel.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.BOLD, 16));
        titleLabel.setForeground(GRADIENT_START);
        titleLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        JLabel msgLabel = new JLabel("<html><body style='text-align:center;'>" + msg.replace("\n", "<br>") + "</body></html>");
        msgLabel.setFont(new java.awt.Font("Microsoft YaHei UI", java.awt.Font.PLAIN, 13));
        msgLabel.setForeground(TEXT_DARK);
        msgLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
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
            SwingUtilities.invokeLater(() -> {
                window.dispose();
            });
        }).start();
    }

    private static boolean createNewVersion(Path path) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = generateNewVersionName(leaf);
        if (newLeaf.equals(leaf)) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
        return true;
    }

    private static boolean copyWithoutTags(Path path) throws IOException {
        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null) return false;

        String leaf = fileName.toString();
        String newLeaf = removeTagsFromLeafPreserveExt(leaf);
        if (newLeaf.equals(leaf)) return false;

        Path target = parent.resolve(newLeaf);
        target = ensureNonExisting(target);

        Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
        return true;
    }

    private static String generateNewVersionName(String leaf) {
        int dot = leaf.lastIndexOf('.');
        String base, ext;
        if (dot > 0) {
            base = leaf.substring(0, dot);
            ext = leaf.substring(dot);
        } else {
            base = leaf;
            ext = "";
        }

        java.util.regex.Pattern versionPattern = java.util.regex.Pattern.compile("^(?:【[^】]*】)*【V(\\d+)】");
        java.util.regex.Matcher matcher = versionPattern.matcher(base);

        if (matcher.find()) {
            try {
                String versionStr = matcher.group(1);
                int version = Integer.parseInt(versionStr);
                int newVersion = version + 1;
                String newBase = matcher.replaceFirst("【V" + newVersion + "】");
                return newBase + ext;
            } catch (NumberFormatException e) {
                String prefix = matcher.replaceFirst("");
                return prefix + "【V2】" + ext;
            }
        } else {
            return "【V2】" + base + ext;
        }
    }

    private static void sleepSilently(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}