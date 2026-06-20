import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.spec.KeySpec;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import utilities.UniversalFactory;
import utilities.UniversalThemes;
import utilities.PathResolver;

import Menu.FileMenu;
import Menu.EditMenu;
import Menu.SettingsMenu;

import com.google.gson.*;

public class ArkIVv7 implements ActionListener{
    private JFrame frame;
    private JPanel taskPanel;
    private JTextField inputField;
    private int taskCounter = 1;
    private final String FILE_NAME = PathResolver.getDataFilePath();

    private static final String SECRET_KEY = "dataEncryptKey15";
    private static final String SALT = "dataEncryptSalt7";
    private static final String IV = "dataEncryptIV328";
    private JTextArea inputArea;

    private JPanel sidebarPanel;

    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu, settingsMenu, helpMenu;

    private Map<Integer, TaskItem> idToTaskMap = new HashMap<>();
    private List<TaskItem> allTasks = new ArrayList<>();

    private TaskItem selectedTask = null;
    private List<TaskItem> searchResults = new ArrayList<>();
    private int searchIndex = -1;
    private TaskItem highlightedSearchTask = null;

    private JDialog dialog;

    //to call private methods in this class from FileMenu.java
    FileMenu Menu_file = new FileMenu(
            this::saveTasks,
            this::deselectAll,
            () -> SwingUtilities.invokeLater(() -> {
                inputArea.requestFocusInWindow();
                UniversalThemes.flashBorder(inputArea, UniversalThemes.ACCENT_COLOR, UniversalThemes.BORDER_COLOR1);
            })
    );

    EditMenu Menu_edit = new EditMenu(
            this::collapseAll,
            this::expandAll
    );


    SettingsMenu Menu_settings = new SettingsMenu();

    //Search and find functionality
    private JScrollPane taskScrollPane;
    private JTextField searchBar;
    private JButton searchPrevButton, searchNextButton;
    private String lastSearchedQuery = "";

    public ArkIVv7() {

        frame = new JFrame("ArkIV");
        frame.getContentPane().setBackground(UniversalThemes.BG_MAIN);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);

        // ── Task panel + scroll ──────────────────────────────────────────
        taskPanel = new JPanel();
        taskPanel.setBackground(UniversalThemes.BG_MAIN);
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.Y_AXIS));

        taskScrollPane = new JScrollPane(taskPanel);
        taskScrollPane.setBorder(BorderFactory.createLineBorder(UniversalThemes.BORDER_COLOR1, 1));
        taskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        taskScrollPane.getVerticalScrollBar().setUnitIncrement(35);
        taskScrollPane.getViewport().setBackground(UniversalThemes.BG_MAIN);
        UniversalThemes.applyScrollbarTheme(taskScrollPane);

        // ── Input area + scroll ──────────────────────────────────────────
        inputArea = new JTextArea(3, 30);
        inputArea.setFont(UniversalThemes.getCompositeFont(20));
        inputArea.setBackground(UniversalThemes.BG_COMPONENT);
        inputArea.setForeground(UniversalThemes.TXT_PRIMARY);
        inputArea.setCaretColor(UniversalThemes.ACCENT_COLOR);
        inputArea.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, UniversalThemes.BORDER_COLOR1));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.getViewport().setBackground(UniversalThemes.BG_PANEL);
        inputScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UniversalThemes.BORDER_COLOR1));
        UniversalThemes.applyScrollbarTheme(inputScroll);
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setMinimumSize(new Dimension(0, 60));
        inputScroll.setPreferredSize(new Dimension(0, 90));

        // Auto-grow input area
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { adjustHeight(); }
            public void removeUpdate(DocumentEvent e)  { adjustHeight(); }
            public void changedUpdate(DocumentEvent e) { adjustHeight(); }

            private void adjustHeight() {
                int rows = inputArea.getLineCount();
                if (rows > inputArea.getRows()) {
                    inputArea.setRows(Math.min(rows, 10));
                    inputScroll.revalidate();
                }
            }
        });

        // Key bindings: Shift+Enter = new line, Enter = submit
        InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputArea.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-newline");
        am.put("insert-newline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pos = inputArea.getCaretPosition();
                String text = inputArea.getText();
                inputArea.setText(text.substring(0, pos) + "\n" + text.substring(pos));
                inputArea.setCaretPosition(pos + 1);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit-note");
        am.put("submit-note", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createTask();
            }
        });

        // ── Sidebar ──────────────────────────────────────────────────────
        sidebarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(UniversalThemes.BORDER_COLOR2);
                g2.setStroke(new BasicStroke(1f));
//                g2.drawLine(0, 80, getWidth() , 80);

                g2.dispose();
            }
        };
        sidebarPanel.setBackground(UniversalThemes.BG_SIDEBAR);
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UniversalThemes.BORDER_COLOR2));
        createMenuBar();
        createSearchBar();




        // ── Inner split: tasks (top) + input (bottom) ────────────────────
        JSplitPane innerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, taskScrollPane, inputScroll);
        innerSplitPane.setResizeWeight(0.96);   // tasks absorb all extra space
        innerSplitPane.setDividerSize(0);
        innerSplitPane.setBorder(null);
        innerSplitPane.setBackground(UniversalThemes.BG_MAIN);
        innerSplitPane.setEnabled(false);      // lock divider, input height is fixed

        // ── Outer split: sidebar (left) + inner split (right) ────────────
        JSplitPane outerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, innerSplitPane);
        outerSplitPane.setResizeWeight(0.225);
        outerSplitPane.setDividerSize(0);
        outerSplitPane.setBorder(null);
        outerSplitPane.setBackground(UniversalThemes.BG_MAIN);
        outerSplitPane.setEnabled(false);

        frame.add(outerSplitPane, BorderLayout.CENTER);

        // ── Window close handler ─────────────────────────────────────────
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                deselectAll();
                saveTasks();
            }
        });

        loadTasks();
        frame.setVisible(true);
    }



    private void createTask() {
        String text = inputArea.getText().trim();
        if (!text.isEmpty()) {
            addTaskFromInput(text);
            inputArea.setText("");
        }
    }

    private void deselectAll() {
        // Reset global selected task
        selectedTask = null;


        // Loop through all tasks and deselect each (stops flickers, resets borders, clears isSelected)
        for (TaskItem task : allTasks) {
            task.deselectThisTask();  // Calls stopFlicker(), sets isSelected=false, resets border
            // Note: We DON'T uncheck checkboxes here -- preserves "done" state if intended
            // If a checkbox was checked only for selection (not true "done"), it will stay checked on reload,
            // but since isSelected=false, moves won't work until re-interaction.
        }

        // Optional: Repaint panel to ensure visuals update immediately
        taskPanel.revalidate();
        taskPanel.repaint();
    }

    private void performSearch(String query) {
        // Clear any highlight from a previous search before starting a new one
        if (highlightedSearchTask != null) {
            highlightedSearchTask.clearSearchHighlight();
            highlightedSearchTask = null;
        }

        searchResults.clear();
        searchIndex = -1;

        String q = query.trim().toLowerCase();
        lastSearchedQuery = q;
        if (q.isEmpty()) {
            return;
        }

        List<TaskItem> matched = new ArrayList<>();
        Map<TaskItem, Integer> matchPosition = new HashMap<>();

        for (TaskItem task : allTasks) {
            String text = task.getRawText().toLowerCase();
            int pos = text.indexOf(q);
            if (pos != -1) {
                matched.add(task);
                matchPosition.put(task, pos);
            }
        }

        // Earlier match position in the text = closer/more relevant
        matched.sort(Comparator.comparingInt(matchPosition::get));
        searchResults.addAll(matched);
    }

    private void jumpToResult(int index) {
        if (searchResults.isEmpty()) return;

        // Wrap around in both directions
        if (index < 0) index = searchResults.size() - 1;
        if (index >= searchResults.size()) index = 0;
        searchIndex = index;

        TaskItem target = searchResults.get(searchIndex);

        // Clear previous highlight if it's a different task
        if (highlightedSearchTask != null && highlightedSearchTask != target) {
            highlightedSearchTask.clearSearchHighlight();
        }

        // If the match is a subtask hidden under a collapsed parent, expand it
        if (target.isSubtask()) {
            TaskItem parent = idToTaskMap.get(target.getParentId());
            if (parent != null && parent.isCollapsed()) {
                expandTask(parent);
            }
        }

        // Avoid flicker-vs-highlight border conflict (see step 2 notes)
        if (selectedTask != null) {
            selectedTask.deselectThisTask();
        }

        target.applySearchHighlight();
        highlightedSearchTask = target;

        SwingUtilities.invokeLater(() -> {
            taskPanel.scrollRectToVisible(target.getBounds());
        });
    }

    private void updateSearchButtonStates() {
        boolean hasResults = !searchResults.isEmpty();
        searchPrevButton.setEnabled(hasResults);
        searchNextButton.setEnabled(hasResults);
        searchPrevButton.repaint();
        searchNextButton.repaint();
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();
        menuBar.setBackground(UniversalThemes.BG_SIDEBAR);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UniversalThemes.BORDER_COLOR2));

        //Menu
        fileMenu     = UniversalFactory.createMenuBar("File");
        editMenu     = UniversalFactory.createMenuBar("Edit");
        settingsMenu = UniversalFactory.createMenuBar("Settings");
        helpMenu     = UniversalFactory.createMenuBar("Help");

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

        createFileMenu();
        createEditMenu();
        createSettingsMenu();

        int menuHeight = menuBar.getPreferredSize().height;
        menuBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, menuHeight));
        menuBar.setMinimumSize(new Dimension(0, menuHeight));
        menuBar.setPreferredSize(new Dimension(0, menuHeight));  // 0 width lets BoxLayout control it

        JPanel menuWrapper = new JPanel(new BorderLayout());
        menuWrapper.setBackground(UniversalThemes.BG_SIDEBAR);
        menuWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, menuHeight));
        menuWrapper.setMinimumSize(new Dimension(0, menuHeight));
        menuWrapper.setPreferredSize(new Dimension(0, menuHeight));
        menuWrapper.add(menuBar, BorderLayout.CENTER);

        sidebarPanel.add(menuWrapper);
    }

    private void createFileMenu(){

        JMenuItem newEntry    = UniversalFactory.createMenuBarItem("New Entry");
        newEntry.addActionListener(this);
        newEntry.setActionCommand("New Entry");

        JMenuItem importData  = UniversalFactory.createMenuBarItem("Import Data");
        importData.addActionListener(this);
        importData.setActionCommand("Import Data");

        JMenuItem exportData  = UniversalFactory.createMenuBarItem("Export Data");
        exportData.addActionListener(this);
        exportData.setActionCommand("Export Data");

        JMenuItem backupData  = UniversalFactory.createMenuBarItem("Backup Data");
        backupData.addActionListener(this);
        backupData.setActionCommand("Backup Data");

        JMenuItem restoreData = UniversalFactory.createMenuBarItem("Restore Data");
        restoreData.addActionListener(this);
        restoreData.setActionCommand("Restore Data");

        JMenuItem clearAll    = UniversalFactory.createMenuBarItem("Clear All");
        clearAll.addActionListener(this);
        clearAll.setActionCommand("Clear All");

        JMenuItem exit        = UniversalFactory.createMenuBarItem("Exit");
        exit.addActionListener(this);
        exit.setActionCommand("Exit");

        fileMenu.add(newEntry);
        fileMenu.add(importData);
        fileMenu.add(exportData);
        fileMenu.add(backupData);
        fileMenu.add(restoreData);
        fileMenu.add(clearAll);
        fileMenu.add(exit);

    }
    
    private void createEditMenu(){
        JMenuItem undo        = UniversalFactory.createMenuBarItem("Undo");
        undo.addActionListener(this);
        undo.setActionCommand("Undo");

        JMenuItem redo        = UniversalFactory.createMenuBarItem("Redo");
        redo.addActionListener(this);
        redo.setActionCommand("Redo");

        JMenuItem collapse    = UniversalFactory.createMenuBarItem("Collapse All");
        collapse.addActionListener(this);
        collapse.setActionCommand("Collapse All");

        JMenuItem expand      = UniversalFactory.createMenuBarItem("Expand All");
        expand.addActionListener(this);
        expand.setActionCommand("Expand All");

        editMenu.add(undo);
        editMenu.add(redo);
        editMenu.add(collapse);
        editMenu.add(expand);
    }
    
    private void createSettingsMenu(){
        JMenuItem preferences = UniversalFactory.createMenuBarItem("Preferences");
        preferences.addActionListener(this);
        preferences.setActionCommand("Preferences");

        settingsMenu.add(preferences);
    }

    private void createSearchBar(){
        searchBar = new JTextField();
        searchBar.setBackground(UniversalThemes.BG_SIDEBAR);
        searchBar.setForeground(UniversalThemes.TXT_PRIMARY);
        searchBar.setCaretColor(UniversalThemes.ACCENT_COLOR);
        searchBar.setFont(UniversalThemes.UI_FONT_BIG);
        searchBar.setBorder(BorderFactory.createMatteBorder(1,1,1,1, UniversalThemes.BORDER_COLOR2));
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); // cap searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); // cap height
        searchBar.setPreferredSize(new Dimension(0, 30)); // match button height; width controlled by BorderLayout.CENTER

        JButton searchButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2 - 2, cy = getHeight() / 2 - 2;

                g2.setColor(UniversalThemes.BG_COMPONENT);
                g2.fillOval(cx - 6, cy - 6, 12, 12);

                g2.setColor(UniversalThemes.TXT_PRIMARY);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(cx - 6, cy - 6, 12, 12);

                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx + 4, cy + 4, cx + 9, cy + 9);

                g2.dispose();
            }
        };
//        searchButton.setFont(UniversalThemes.UI_FONT_EMOJI1);
        searchButton.setPreferredSize(new Dimension(30, 30));
        searchButton.setBackground(UniversalThemes.BG_SIDEBAR);
        searchButton.setForeground(UniversalThemes.TXT_SELECTED);
        searchButton.setBorder(new LineBorder(UniversalThemes.BORDER_COLOR2, 1));
        searchButton.setFocusable(false);

        searchButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (!searchButton.isEnabled()) return;
            searchButton.setBackground(UniversalThemes.BORDER_COLOR1);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!searchButton.isEnabled()) return;
            searchButton.setBackground(UniversalThemes.BG_SIDEBAR);
        }
        });

        searchButton.setUI(new UniversalThemes.NoPressedButtonUI());

        searchPrevButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2, cy = getHeight() / 2;
                int arm = 5;

                g2.setColor(isEnabled() ? UniversalThemes.TXT_PRIMARY : UniversalThemes.DISABLED_TEXT);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - arm, cy + 3, cx, cy - 3);
                g2.drawLine(cx, cy - 3, cx + arm, cy + 3);

                g2.dispose();
            }
        };
        searchPrevButton.setPreferredSize(new Dimension(26, 30));
        searchPrevButton.setBackground(UniversalThemes.BG_SIDEBAR);
        searchPrevButton.setBorder(new LineBorder(UniversalThemes.BORDER_COLOR2, 1));
        searchPrevButton.setFocusable(false);
        searchPrevButton.setUI(new UniversalThemes.NoPressedButtonUI());
        searchPrevButton.setEnabled(false);
        searchPrevButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (searchPrevButton.isEnabled()) searchPrevButton.setBackground(UniversalThemes.BORDER_COLOR1);
            }
            public void mouseReleased(MouseEvent e) {
                if (searchPrevButton.isEnabled()) searchPrevButton.setBackground(UniversalThemes.BG_SIDEBAR);
            }
        });

        searchNextButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2, cy = getHeight() / 2;
                int arm = 5;

                g2.setColor(isEnabled() ? UniversalThemes.TXT_PRIMARY : UniversalThemes.DISABLED_TEXT);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - arm, cy - 3, cx, cy + 3);
                g2.drawLine(cx, cy + 3, cx + arm, cy - 3);

                g2.dispose();
            }
        };
        searchNextButton.setPreferredSize(new Dimension(26, 30));
        searchNextButton.setBackground(UniversalThemes.BG_SIDEBAR);
        searchNextButton.setBorder(new LineBorder(UniversalThemes.BORDER_COLOR2, 1));
        searchNextButton.setFocusable(false);
        searchNextButton.setUI(new UniversalThemes.NoPressedButtonUI());
        searchNextButton.setEnabled(false);
        searchNextButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (searchNextButton.isEnabled()) searchNextButton.setBackground(UniversalThemes.BORDER_COLOR1);
            }
            public void mouseReleased(MouseEvent e) {
                if (searchNextButton.isEnabled()) searchNextButton.setBackground(UniversalThemes.BG_SIDEBAR);
            }
        });

//        UniversalThemes.ClickEffect(searchButton);

        JPanel searchRow = new JPanel(new BorderLayout(5, 0)); // 4px gap
        searchRow.setBackground(UniversalThemes.BG_SIDEBAR);
        searchRow.add(searchBar, BorderLayout.CENTER);

        JPanel searchButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        searchButtonsPanel.setBackground(UniversalThemes.BG_SIDEBAR);
        searchButtonsPanel.add(searchPrevButton);
        searchButtonsPanel.add(searchNextButton);
        searchButtonsPanel.add(searchButton);
        searchRow.add(searchButtonsPanel, BorderLayout.EAST);

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setBackground(UniversalThemes.BG_SIDEBAR);
        searchWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); // padding
        searchWrapper.add(searchRow, BorderLayout.CENTER);
        searchWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        searchButton.addActionListener(e -> {
            performSearch(searchBar.getText());
            if (!searchResults.isEmpty()) {
                jumpToResult(0);
            }
            updateSearchButtonStates();
        });

        searchBar.addActionListener(e -> {
            // If we already have results for this exact search, Enter advances to next match
            String query = searchBar.getText().trim();
            if (!searchResults.isEmpty() && searchIndex != -1) {
                jumpToResult(searchIndex + 1);
            } else {
                performSearch(query);
                if (!searchResults.isEmpty()) {
                    jumpToResult(0);
                }
            }
            updateSearchButtonStates();
        });

        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { checkForStaleSearch(); }
            public void removeUpdate(DocumentEvent e)  { checkForStaleSearch(); }
            public void changedUpdate(DocumentEvent e) { checkForStaleSearch(); }

            private void checkForStaleSearch() {
                String current = searchBar.getText().trim().toLowerCase();
                if (!current.equals(lastSearchedQuery)) {
                    if (highlightedSearchTask != null) {
                        highlightedSearchTask.clearSearchHighlight();
                        highlightedSearchTask = null;
                    }
                    searchResults.clear();
                    searchIndex = -1;
                    updateSearchButtonStates();
                }
            }
        });

        searchPrevButton.addActionListener(e -> {
            jumpToResult(searchIndex - 1);
            updateSearchButtonStates();
        });

        searchNextButton.addActionListener(e -> {
            jumpToResult(searchIndex + 1);
            updateSearchButtonStates();
        });

        sidebarPanel.add(searchWrapper);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch(command){
            //File Menu
            case "New Entry"  : Menu_file.newEntry();   break;
            case "Import Data": Menu_file.importData(); break;
            case "Export Data": Menu_file.exportData(); break;
            case "Backup Data": Menu_file.backupData(); break;
            case "Restore Data":Menu_file.restoreData();break;
            case "Clear All": Menu_file.clearAll();break;
            case "Exit": Menu_file.exit(); break;

            //Edit
            case "Undo": Menu_edit.undo(); break;
            case "Redo": Menu_edit.redo(); break;
            case "Collapse All": Menu_edit.collapseAll(); break;
            case "Expand All": Menu_edit.expandAll(); break;

            //Settings
            case "Preferences": Menu_settings.preferences();break;


        }

    }

    private void collapseAll() {
        for (TaskItem task : allTasks) {
            if (!task.isSubtask() && !task.isCollapsed()) {
                task.isCollapsed = true;
                Border newOuter = BorderFactory.createMatteBorder(1, 0, 10, 0, UniversalThemes.BORDER_COLOR1);
                Border currentInner = (task.getBorder() instanceof CompoundBorder)
                        ? ((CompoundBorder) task.getBorder()).getInsideBorder()
                        : BorderFactory.createLineBorder(UniversalThemes.BG_PANEL, 2);
                task.setBorder(BorderFactory.createCompoundBorder(newOuter, currentInner));
                hideSubtasks(task);
            }
        }
        saveTasks();
    }

    private void expandAll() {
        for (TaskItem task : allTasks) {
            if (!task.isSubtask() && task.isCollapsed()) {
                task.isCollapsed = false;
                Border newOuter = BorderFactory.createMatteBorder(1, 0, 0, 0, UniversalThemes.BORDER_COLOR1);
                Border currentInner = (task.getBorder() instanceof CompoundBorder)
                        ? ((CompoundBorder) task.getBorder()).getInsideBorder()
                        : BorderFactory.createLineBorder(UniversalThemes.BG_PANEL, 2);
                task.setBorder(BorderFactory.createCompoundBorder(newOuter, currentInner));
                showSubtasks(task);
            }
        }
        saveTasks();
    }

    private void expandTask(TaskItem task) {
        if (task.isCollapsed()) {
            task.isCollapsed = false;
            Border newOuter = BorderFactory.createMatteBorder(1, 0, 0, 0, UniversalThemes.BORDER_COLOR1);
            Border currentInner = (task.getBorder() instanceof CompoundBorder)
                    ? ((CompoundBorder) task.getBorder()).getInsideBorder()
                    : BorderFactory.createLineBorder(UniversalThemes.BG_PANEL, 2);
            task.setBorder(BorderFactory.createCompoundBorder(newOuter, currentInner));
            showSubtasks(task);
            saveTasks();
        }
    }

    private void addTaskFromInput(String text) {
        text = text.trim();
        if (!text.isEmpty()) {
            TaskItem task;
            if (selectedTask != null && !selectedTask.isSubtask()) {
                task = new TaskItem(taskCounter++, text, false, true, false, selectedTask.getId());
            }
            else {
                task = new TaskItem(taskCounter++, text, false, false, false, -1);
            }
            idToTaskMap.put(task.getId(), task);
            allTasks.add(task);
            taskPanel.add(task);
            taskPanel.revalidate();
            saveTasks();
        }
    }

    private void loadTasks() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }

                JsonArray array = JsonParser.parseString(sb.toString()).getAsJsonArray();
                allTasks.clear();
                idToTaskMap.clear();

                for (JsonElement el : array) {
                    JsonObject obj = el.getAsJsonObject();
                    int id            = obj.get("ID").getAsInt();                //ID - id
                    int parentId      = obj.get("parentID").getAsInt();             //parentID - parent id
                    String text       = obj.get("TXT").getAsString();             //TXT - text
                    boolean done      = obj.get("isDone").getAsBoolean();            //isDone - done
                    boolean isSubtask = obj.get("isSub").getAsBoolean();            //isSub - is Subtask
                    boolean isCollapsed = obj.get("isCollapsed").getAsBoolean();          //isCollapsed - is Collapsed

                    taskCounter = Math.max(taskCounter, id + 1);
                    TaskItem task = new TaskItem(id, text, done, isSubtask, isCollapsed, parentId);
                    allTasks.add(task);
                    idToTaskMap.put(id, task);
                }

                List<TaskItem> mainTasks = new ArrayList<>();
                for (TaskItem task : allTasks) {
                    if (!task.isSubtask()) {
                        mainTasks.add(task);
                        taskPanel.add(task);
                    }
                }

                for (TaskItem task : mainTasks) {
                    if (!task.isCollapsed()) {
                        showSubtasks(task);
                    }
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Error loading tasks");
                e.printStackTrace();
            }
        }
    }




    private void saveTasks() {
        try {
            JsonArray array = new JsonArray();
            for (TaskItem t : allTasks) {
                JsonObject obj = new JsonObject();
                obj.addProperty("ID", t.getId());
                obj.addProperty("parentID", t.getParentId());
                obj.addProperty("TXT", t.getRawText());
                obj.addProperty("isDone", t.isDone());
                obj.addProperty("isSub", t.isSubtask());
                obj.addProperty("isCollapsed", t.isCollapsed());
                array.add(obj);
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
                writer.print(new GsonBuilder().setPrettyPrinting().create().toJson(array));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error saving tasks");
        }
    }



    private void hideSubtasks(TaskItem parent) {
        for (TaskItem task : allTasks) {
            if (task.isSubtask() && task.getParentId() == parent.getId()) {
                taskPanel.remove(task);
            }
        }
        taskPanel.revalidate();
        taskPanel.repaint();
    }

    private void showSubtasks(TaskItem parent) {
        int parentIndex = taskPanel.getComponentZOrder(parent);

        // Collect all subtasks of this parent IN THE ORDER THEY APPEAR IN allTasks (preserves moved/saved order)
        List<TaskItem> subtasks = new ArrayList<>();
        for (TaskItem task : allTasks) {  // Loop over allTasks to get saved order
            if (task.isSubtask() && task.getParentId() == parent.getId()) {
                subtasks.add(task);
                // NO SORTING BY ID HERE -- this was the bug causing revert!
            }
        }

        // Add them just after the parent (in the collected order)
        for (int i = 0; i < subtasks.size(); i++) {
            taskPanel.add(subtasks.get(i), parentIndex + i + 1);
        }

        taskPanel.revalidate();
        taskPanel.repaint();
    }


    private String encrypt(String plainText) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivspec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decrypt(String encryptedText) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivspec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
        byte[] decodedEncrypted = Base64.getMimeDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decodedEncrypted);
        return new String(decrypted, "UTF-8");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ArkIVv7::new);
    }



    public class TaskItem extends JPanel {
        private int id;
        private int parentId;
        private boolean isSubtask;
        private boolean isCollapsed;
        private JCheckBox checkBox;
        private JTextArea textArea;
        private Timer flickerTimer;
        private boolean isSelected = false;
        private boolean isSearchHighlighted = false;

        public TaskItem(int id, String text, boolean done, boolean isSubtask, boolean isCollapsed, int parentId) {
            this.id = id;
            this.parentId = parentId;
            this.isSubtask = isSubtask;
            this.isCollapsed = isCollapsed;


            setLayout(new BorderLayout());
            Color cardBg = isSubtask
                    ? UniversalThemes.BG_COMPONENT
                    : UniversalThemes.BG_PANEL;

            setBackground(cardBg);

            // Outer border (same as original)
            Border outerBorder;
            if (isSubtask) {
                outerBorder = BorderFactory.createMatteBorder(5, 30, 2, 30, UniversalThemes.BORDER_COLOR1);
            } else {
                outerBorder = BorderFactory.createMatteBorder(1, 0, isCollapsed ? 10 : 0, 0, UniversalThemes.BORDER_COLOR1);
            }

            // Inner border (2 pixels thick now)
            Border innerBorder;
            if (isSubtask) {
                innerBorder = BorderFactory.createLineBorder(cardBg, 2);
            } else {
                innerBorder = BorderFactory.createLineBorder(cardBg, 2);
            }

            // Combine borders
            setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

            setOpaque(true);
            setAlignmentX(Component.LEFT_ALIGNMENT);

            checkBox = new JCheckBox();
            checkBox.setPreferredSize(new Dimension(30, 30));
            checkBox.setBackground(UniversalThemes.BG_MAIN);
            UniversalThemes.applyCheckBoxTheme(checkBox);
            checkBox.setSelected(done);

            // ActionListener for flicker effect and selection management
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    selectThisTask();
                } else {
                    deselectThisTask();
                }
            });

            textArea = new JTextArea(text) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    int minHeight = 68;
                    if (d.height < minHeight) d.height = minHeight;
                    return d;
                }
            };
            textArea.setFont(UniversalThemes.getCompositeFont(20));
            textArea.setForeground(UniversalThemes.TXT_PRIMARY);
            textArea.setCaretColor(UniversalThemes.ACCENT_COLOR);
            textArea.setOpaque(false);

            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setBorder(null);

            if (done) {

            }

            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setOpaque(false);
            leftPanel.add(checkBox, BorderLayout.WEST);
            leftPanel.add(textArea, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setOpaque(false);

//            JButton editButton = new JButton(" ✎ ");
//            editButton.setText("<html><div style='margin-top:3px;'>✎</div></html>");
//
//            editButton.setFont(UniversalThemes.UI_FONT_EMOJI);
//            editButton.setBackground(UniversalThemes.ACCENT_COLOR);
//            editButton.setForeground(UniversalThemes.TXT_SELECTED);
//            editButton.setBorder(new LineBorder(UniversalThemes.ACCENT_COLOR_DARK, 2));
//            editButton.setPreferredSize(new Dimension(40, 29));
//            UniversalThemes.ClickEffect(editButton);
//            editButton.addActionListener(e -> editTask());

//            JButton deleteButton = new JButton(" \uD83D\uDDD1\uFE0F ");
//            deleteButton.setText("<html><div style='margin-top:3px;'>🗑️</div></html>");
//            deleteButton.setFont(UniversalThemes.UI_FONT_EMOJI);
//            deleteButton.setBackground(UniversalThemes.ACCENT_COLOR);
//            deleteButton.setForeground(UniversalThemes.TXT_SELECTED);
//            deleteButton.setBorder(new LineBorder(UniversalThemes.ACCENT_COLOR_DARK, 2));
//            deleteButton.setPreferredSize(new Dimension(40, 29));
//
//            UniversalThemes.ClickEffect(deleteButton);

//            deleteButton.addActionListener(e -> confirmDeleteTask());
//
//            buttonPanel.add(editButton);
//            buttonPanel.add(deleteButton);

            if (!isSubtask) {
                JButton createSubtaskButton = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        int arm = 7;

                        g2.setColor(UniversalThemes.TXT_SELECTED);
                        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(cx - arm, cy, cx + arm, cy);
                        g2.drawLine(cx, cy - arm, cx, cy + arm);

                        g2.dispose();
                    }
                };
//                createSubtaskButton.setFont(UniversalThemes.UI_FONT_EMOJI1);
                createSubtaskButton.setBackground(UniversalThemes.ACCENT_COLOR);
                createSubtaskButton.setForeground(UniversalThemes.TXT_SELECTED);
                createSubtaskButton.setBorder(new LineBorder(UniversalThemes.ACCENT_COLOR_DARK, 2));
                createSubtaskButton.setPreferredSize(new Dimension(40, 29));
                createSubtaskButton.setUI(new UniversalThemes.NoPressedButtonUI());
                UniversalThemes.ClickEffect(createSubtaskButton);

                createSubtaskButton.addActionListener(e -> createSubtask());
                buttonPanel.add(createSubtaskButton);

                addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            TaskItem.this.isCollapsed = !TaskItem.this.isCollapsed;
                            Border newOuter = BorderFactory.createMatteBorder(1, 0, TaskItem.this.isCollapsed ? 10 : 0, 0, UniversalThemes.BORDER_COLOR1);
                            Border currentBorder = getBorder();
                            Border currentInner;
                            if (currentBorder instanceof CompoundBorder) {
                                currentInner = ((CompoundBorder) currentBorder).getInsideBorder();
                            } else {
                                currentInner = innerBorder;
                            }
                            setBorder(BorderFactory.createCompoundBorder(newOuter, currentInner));
                            if (TaskItem.this.isCollapsed) hideSubtasks(TaskItem.this);
                            else showSubtasks(TaskItem.this);
                            saveTasks();
                        } else {
                            //Do nothing
                        }
                    }
                });
            } else {
                addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        //Do nothing
                    }
                });
            }

            add(leftPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.EAST);


            //Shortcuts for convenience

            InputMap im = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap am = this.getActionMap();

            //Edit Task
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "Edit");
            am.put("Edit", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(isSelected){
                        editTask();
                    }
                }
            });

            //Delete Task
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
            am.put("Delete", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isSelected) {
                        confirmDeleteTask();
                    }
                }
            });

            //add subtask to task
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "addSubTask");
            am.put("addSubTask", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isSelected) {
                        createSubtask();
                    }
                }
            });

            //Deselect selected tasks
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Deselect");
            am.put("Deselect", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isSelected) {
                        deselectAll();
                    }
                }
            });

            //Move Up selected task
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "moveUp");
            am.put("moveUp", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isSelected) {
                        moveTaskUp();
                    }
                }
            });

            //Move Down selected task
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "moveDown");
            am.put("moveDown", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isSelected) {
                        moveTaskDown();
                    }
                }
            });

        }

        public int getId() { return id; }
        public int getParentId() { return parentId; }
        public boolean isSubtask() { return isSubtask; }
        public boolean isCollapsed() { return isCollapsed; }
        public boolean isDone() { return checkBox.isSelected(); }
        public String getRawText() { return textArea.getText(); }

        private void selectThisTask() {
            if (selectedTask != null && selectedTask != this) {
                selectedTask.deselectThisTask();
            }
            selectedTask = this;
            isSelected = true;
            startFlicker();
            // request focus so key bindings on this panel can fire
            SwingUtilities.invokeLater(() -> this.requestFocusInWindow());
        }

        private void deselectThisTask() {
            isSelected = false;
            stopFlicker();
            checkBox.setSelected(false);
            if (selectedTask == this) {
                selectedTask = null;
            }
            resetInnerBorder();
        }

        private void startFlicker() {
            if (flickerTimer != null && flickerTimer.isRunning()) {
                return; // Already flickering
            }

            Border currentBorder = getBorder();
            if (!(currentBorder instanceof CompoundBorder)) return;
            Border outerBorder = ((CompoundBorder) currentBorder).getOutsideBorder();
            Border originalInner = ((CompoundBorder) currentBorder).getInsideBorder();

            Border flickerBorder = BorderFactory.createLineBorder(UniversalThemes.ACCENT_COLOR, 2);

            flickerTimer = new Timer(300, null); // 300 ms delay for slower flicker
            final int[] count = {0};
            flickerTimer.addActionListener(ev -> {
                if (!isSelected) {
                    // Stop flickering if deselected
                    setBorder(BorderFactory.createCompoundBorder(outerBorder, originalInner));
                    flickerTimer.stop();
                    return;
                }
                if (count[0] % 2 == 0) {
                    setBorder(BorderFactory.createCompoundBorder(outerBorder, flickerBorder));
                } else {
                    setBorder(BorderFactory.createCompoundBorder(outerBorder, originalInner));
                }
                count[0]++;
            });
            flickerTimer.start();
        }

        private void stopFlicker() {
            if (flickerTimer != null) {
                flickerTimer.stop();
                flickerTimer = null;
            }
        }

        private void resetInnerBorder() {
            Border currentBorder = getBorder();
            if (!(currentBorder instanceof CompoundBorder)) return;
            Border outerBorder = ((CompoundBorder) currentBorder).getOutsideBorder();

            Border innerBorder;
            if (isSubtask) {
                innerBorder = BorderFactory.createLineBorder(UniversalThemes.BG_COMPONENT, 2);
            } else {
                innerBorder = BorderFactory.createLineBorder(UniversalThemes.BG_COMPONENT, 2);
            }
            setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
        }

        public void applySearchHighlight() {
            isSearchHighlighted = true;
            Border currentBorder = getBorder();
            Border outerBorder = (currentBorder instanceof CompoundBorder)
                    ? ((CompoundBorder) currentBorder).getOutsideBorder()
                    : BorderFactory.createEmptyBorder();
            Border highlightInner = BorderFactory.createLineBorder(UniversalThemes.SEARCH_HIGHLIGHT_COLOR, 2);
            setBorder(BorderFactory.createCompoundBorder(outerBorder, highlightInner));
        }

        public void clearSearchHighlight() {
            isSearchHighlighted = false;
            resetInnerBorder();
        }


        private void editTask() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(UniversalThemes.BG_MAIN);

            JTextArea field = new JTextArea(getRawText(), 4, 40); // Start with current raw text
            field.setBackground(UniversalThemes.BG_PANEL);
            field.setForeground(UniversalThemes.TXT_PRIMARY);
            field.setCaretColor(UniversalThemes.ACCENT_COLOR);


            // Select all text when field gains focus (better than invokeLater)
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.selectAll();
                }
            });

            // Auto-sizing and line wrapping
            field.setLineWrap(true);
            field.setWrapStyleWord(true);

            // Auto-grow (no forced scrolling) - improved to resize dialog dynamically
            field.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    adjustTextAreaHeight();
                    autoScrollIfAtEnd(field);
                }

                public void removeUpdate(DocumentEvent e) {
                    adjustTextAreaHeight();
                    autoScrollIfAtEnd(field); // Added for consistency
                }

                public void changedUpdate(DocumentEvent e) {
                    adjustTextAreaHeight();
                    autoScrollIfAtEnd(field); // Added for consistency
                }

                private void adjustTextAreaHeight() {
                    int rows = field.getLineCount();
                    if (rows > field.getRows()) {
                        field.setRows(Math.min(rows, 10)); // Max 10 visible rows
                        // Dynamically resize dialog
                        SwingUtilities.invokeLater(() -> {
                            JDialog dialog = (JDialog) SwingUtilities.getWindowAncestor(panel);
                            if (dialog != null && dialog.isVisible()) {
                                dialog.pack(); // Repack to grow dialog based on new preferred size
                            }
                        });
                    }
                }

                private void autoScrollIfAtEnd(JTextArea ta) { // Renamed param for clarity
                    if (ta.getCaretPosition() == ta.getDocument().getLength()) {
                        SwingUtilities.invokeLater(() -> {
                            ta.setCaretPosition(ta.getDocument().getLength());
                        });
                    }
                }
            });

            // Improved key bindings
            InputMap im = field.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = field.getActionMap();

            // Shift+Enter: Insert new line
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-newline");
            am.put("insert-newline", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int caretPos = field.getCaretPosition();
                    String text = field.getText();
                    field.setText(text.substring(0, caretPos) + "\n" + text.substring(caretPos));
                    field.setCaretPosition(caretPos + 1);
                }
            });

            // Enter: Submit edit
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit-edit");
            am.put("submit-edit", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newText = field.getText(); // Don't trim here to preserve whitespace/newlines
                    if (!newText.trim().isEmpty()) {
                        // Update the task's textArea with new text
                        textArea.setText(newText);
                        boolean isDone = checkBox.isSelected();
                        if (isDone) {
                            // Apply strikethrough and gray if "done"
//                             textArea.setText(newText.replaceAll(".", "̶$0"));
                        } else {
                            textArea.setForeground(UniversalThemes.TXT_PRIMARY);
                        }
                        deselectThisTask();
                        saveTasks(); // Now saves the updated (raw or decorated) text
                    } else {
                        // Optional: Confirm deletion if text is empty
                        boolean confirmed = UniversalThemes.showConfirmPopup(
                                panel,
                                "Text is empty. Delete this task?",
                                "Confirm Delete"
                        );

                        if (confirmed) {
                            deselectThisTask();
                            JDialog dialog = (JDialog) SwingUtilities.getWindowAncestor(panel);
                            if (dialog != null) dialog.dispose();
                            DeleteEmptyTask();
                            return; // Don't dispose below
                        }

                        // If no, just close without changes
                    }
                    // Dispose dialog
                    JDialog dialog = (JDialog) SwingUtilities.getWindowAncestor(panel);
                    if (dialog != null) {
                        dialog.dispose();
                    }
                }
            });

            // Scroll pane for field
            JScrollPane scrollPane = new JScrollPane(field);
            scrollPane.setBackground(UniversalThemes.BG_PANEL);
            scrollPane.setBorder(BorderFactory.createLineBorder(UniversalThemes.BORDER_COLOR1, 1));

            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            UniversalThemes.applyScrollbarTheme(scrollPane);

            // UI styling
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            field.setFont(UniversalThemes.getCompositeFont(20));
            field.setMargin(new Insets(10, 10, 10, 10));


            panel.add(scrollPane, BorderLayout.CENTER);

            // Create and show dialog
            JDialog dialog = new JDialog(frame, "Edit Task", true);
            dialog.getContentPane().add(panel);
            dialog.pack();
            dialog.setMinimumSize(new Dimension(600, 200));
            dialog.setLocationRelativeTo(frame);
            dialog.addWindowListener(new WindowAdapter() {  //Handles dialog window closing
                @Override
                public void windowClosing(WindowEvent e) {
                    deselectThisTask();
                }
            });
            dialog.setVisible(true);



        }

        private void DeleteEmptyTask() {
            List<Component> toRemove = new ArrayList<>();
            toRemove.add(this); // Always remove the clicked task
            boolean hasSubtasks = false;

            if (!isSubtask) {
                // It's a main task, so find and mark its subtasks
                for (TaskItem task : allTasks) {
                    if (task.isSubtask() && task.getParentId() == this.id) {
                        toRemove.add(task);
                        hasSubtasks = true;
                    }
                }
            }

                for (Component c : toRemove) {
                    taskPanel.remove(c);
                    allTasks.remove(c);
                }
                saveTasks();
                taskPanel.revalidate();
                taskPanel.repaint();

        }

        private void confirmDeleteTask() {
            List<Component> toRemove = new ArrayList<>();
            toRemove.add(this); // Always remove the clicked task
            boolean hasSubtasks = false;

            if (!isSubtask) {
                // It's a main task, so find and mark its subtasks
                for (TaskItem task : allTasks) {
                    if (task.isSubtask() && task.getParentId() == this.id) {
                        toRemove.add(task);
                        hasSubtasks = true;
                    }
                }
            }

            String message = hasSubtasks
                    ? " Delete this Entry and its sub-Entries ?"
                    : " Delete this Entry ? ";

            boolean confirmed = UniversalThemes.showConfirmPopup(
                    frame,
                    message,
                    "Confirm"
            );

            if (confirmed) {
                for (Component c : toRemove) {
                    taskPanel.remove(c);
                    allTasks.remove(c);
                }
                saveTasks();
                taskPanel.revalidate();
                taskPanel.repaint();
            }

        }

        private void createSubtask() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(UniversalThemes.BG_MAIN);

            JTextArea field = new JTextArea(4, 40); // Start with 3 visible rows
            field.setBackground(UniversalThemes.BG_PANEL);
            field.setForeground(UniversalThemes.TXT_PRIMARY);
            field.setCaretColor(UniversalThemes.ACCENT_COLOR);

            JScrollPane scrollPane = new JScrollPane(field);
            scrollPane.setBorder(BorderFactory.createLineBorder(UniversalThemes.BORDER_COLOR1, 1));
            UniversalThemes.applyScrollbarTheme(scrollPane);

            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            field.setFont(UniversalThemes.getCompositeFont(20));
            field.setLineWrap(true);
            field.setWrapStyleWord(true);
            field.setMargin(new Insets(10, 10, 10, 10));

            // Auto-grow implementation
            field.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    adjustTextAreaHeight();
                }

                public void removeUpdate(DocumentEvent e) {
                    adjustTextAreaHeight();
                }

                public void changedUpdate(DocumentEvent e) {
                    adjustTextAreaHeight();
                }

                private void adjustTextAreaHeight() {
                    int rows = field.getLineCount();
                    if (rows > field.getRows()) {
                        field.setRows(Math.min(rows, 10)); // Max 10 visible rows
                        panel.revalidate();
                    }
                }
            });

            // Key bindings for Shift+Enter (new line) and Enter (submit)
            InputMap im = field.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = field.getActionMap();

            // SHIFT + ENTER → New Line
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-newline");
            am.put("insert-newline", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int caretPos = field.getCaretPosition();
                    String text = field.getText();
                    field.setText(text.substring(0, caretPos) + "\n" + text.substring(caretPos));
                    field.setCaretPosition(caretPos + 1); // Move cursor to new line
                }
            });

            // ENTER → Create Subtask
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit-subtask");
            am.put("submit-subtask", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String subtaskText = field.getText().trim();
                    if (!subtaskText.isEmpty()) {
                        ((JDialog) SwingUtilities.getWindowAncestor(panel)).dispose();
                        TaskItem subtask = new TaskItem(taskCounter++, subtaskText, false, true, false, getId());
                        idToTaskMap.put(subtask.getId(), subtask);
                        allTasks.add(subtask);

                        int insertIndex = -1;
                        for (int i = 0; i < taskPanel.getComponentCount(); i++) {
                            Component comp = taskPanel.getComponent(i);
                            if (comp == TaskItem.this) {
                                insertIndex = i;
                            } else if (insertIndex != -1 && comp instanceof TaskItem) {
                                TaskItem t = (TaskItem) comp;
                                if (!t.isSubtask() || t.getParentId() != TaskItem.this.id) break;
                                insertIndex = i;
                            }
                        }
                        taskPanel.add(subtask, insertIndex + 1);
                        taskPanel.revalidate();
                        taskPanel.repaint();
                        saveTasks();
                    }
                }
            });

            panel.add(scrollPane, BorderLayout.CENTER);

            JDialog dialog = new JDialog(frame, "New Subtask", true);
            dialog.getContentPane().add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        }
        // New method: Move this task up within its allowed range, allowing repeated moves while selected
        private void moveTaskUp() {
            int currentIndex = -1;
            int count = taskPanel.getComponentCount();

            // Find current index in taskPanel
            for (int i = 0; i < count; i++) {
                if (taskPanel.getComponent(i) == this) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex <= 0) return; // Already at top or not found

            // If main task and open, collapse it first
            if (!isSubtask && !isCollapsed) {
                isCollapsed = true;
                Border newOuter = BorderFactory.createMatteBorder(1, 0, 20, 0, Color.LIGHT_GRAY);
                Border currentBorder = getBorder();
                Border currentInner;
                if (currentBorder instanceof CompoundBorder) {
                    currentInner = ((CompoundBorder) currentBorder).getInsideBorder();
                } else {
                    Color subtaskBgColor = new Color(240, 240, 240);
                    currentInner = isSubtask ? BorderFactory.createLineBorder(subtaskBgColor, 2) : BorderFactory.createLineBorder(Color.WHITE, 2);
                }
                setBorder(BorderFactory.createCompoundBorder(newOuter, currentInner));
                hideSubtasks(this);
            }

            // Find the index of the task above to swap with, respecting main/subtask boundaries
            int swapIndex = -1;
            for (int i = currentIndex - 1; i >= 0; i--) {
                Component comp = taskPanel.getComponent(i);
                if (comp instanceof TaskItem) {
                    TaskItem t = (TaskItem) comp;
                    if (this.isSubtask) {
                        if (t.isSubtask() && t.getParentId() == this.parentId) {
                            swapIndex = i;
                            break;
                        } else if (!t.isSubtask()) {
                            break;
                        }
                    } else { // main task
                        if (!t.isSubtask()) {
                            swapIndex = i;
                            break;
                        }
                    }
                }
            }

            if (swapIndex == -1) return; // No valid task above to swap with

            // Swap components in taskPanel
            Component aboveComp = taskPanel.getComponent(swapIndex);
            taskPanel.remove(this);
            taskPanel.remove(aboveComp);

            taskPanel.add(this, swapIndex);
            taskPanel.add(aboveComp, currentIndex);

            taskPanel.revalidate();
            taskPanel.repaint();

            // Update allTasks list to keep consistent
            int allTasksIndexThis = allTasks.indexOf(this);
            int allTasksIndexAbove = allTasks.indexOf(aboveComp);
            if (allTasksIndexThis != -1 && allTasksIndexAbove != -1) {
                allTasks.set(allTasksIndexThis, (TaskItem) aboveComp);
                allTasks.set(allTasksIndexAbove, this);
            }

            saveTasks();

            // Ensure this task remains selected and focused for continuous moves
            if (!isSelected) {
                selectThisTask();
            }
            // Request focus on checkbox or text area to keep key events firing
            SwingUtilities.invokeLater(() -> {
                if (isSelected) {
                    textArea.requestFocusInWindow();
                }
            });
        }

        // New method: Move this task down within its allowed range, allowing repeated moves while selected
        private void moveTaskDown() {
            int currentIndex = -1;
            int count = taskPanel.getComponentCount();

            // Find current index in taskPanel
            for (int i = 0; i < count; i++) {
                if (taskPanel.getComponent(i) == this) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex == -1 || currentIndex >= count - 1) return; // Already at bottom or not found

            // If main task and open, collapse it first
            if (!isSubtask && !isCollapsed) {
                isCollapsed = true;
                Border newOuter = BorderFactory.createMatteBorder(1, 0, 20, 0, Color.LIGHT_GRAY);
                Border currentBorder = getBorder();
                Border currentInner;
                if (currentBorder instanceof CompoundBorder) {
                    currentInner = ((CompoundBorder) currentBorder).getInsideBorder();
                } else {
                    Color subtaskBgColor = new Color(240, 240, 240);
                    currentInner = isSubtask ? BorderFactory.createLineBorder(subtaskBgColor, 2) : BorderFactory.createLineBorder(Color.WHITE, 2);
                }
                setBorder(BorderFactory.createCompoundBorder(newOuter, currentInner));
                hideSubtasks(this);
            }

            // Find the index of the task below to swap with, respecting main/subtask boundaries
            int swapIndex = -1;
            for (int i = currentIndex + 1; i < count; i++) {
                Component comp = taskPanel.getComponent(i);
                if (comp instanceof TaskItem) {
                    TaskItem t = (TaskItem) comp;
                    if (this.isSubtask) {
                        if (t.isSubtask() && t.getParentId() == this.parentId) {
                            swapIndex = i;
                            break;
                        } else if (!t.isSubtask()) {
                            break;
                        }
                    } else { // main task
                        if (!t.isSubtask()) {
                            swapIndex = i;
                            break;
                        }
                    }
                }
            }

            if (swapIndex == -1) return; // No valid task below to swap with

            // Swap components in taskPanel
            Component belowComp = taskPanel.getComponent(swapIndex);
            taskPanel.remove(this);
            taskPanel.remove(belowComp);

            taskPanel.add(belowComp, currentIndex);
            taskPanel.add(this, swapIndex);

            taskPanel.revalidate();
            taskPanel.repaint();

            // Update allTasks list to keep consistent
            int allTasksIndexThis = allTasks.indexOf(this);
            int allTasksIndexBelow = allTasks.indexOf(belowComp);
            if (allTasksIndexThis != -1 && allTasksIndexBelow != -1) {
                allTasks.set(allTasksIndexThis, (TaskItem) belowComp);
                allTasks.set(allTasksIndexBelow, this);
            }

            saveTasks();

            // Ensure this task remains selected and focused for continuous moves
            if (!isSelected) {
                selectThisTask();
            }
            // Request focus on checkbox or text area to keep key events firing
            SwingUtilities.invokeLater(() -> {
                if (isSelected) {
                    textArea.requestFocusInWindow();
                }
            });
        }

    }
}


