

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

import utilities.UniversalThemes;
import utilities.AssetPathResolver;


public class ArkIVv7 {
    private JFrame frame;
    private JPanel taskPanel;
    private JTextField inputField;
    private int taskCounter = 1;
    private final String FILE_NAME = AssetPathResolver.getDataFilePath();

    private static final String SECRET_KEY = "dataEncryptKey15";
    private static final String SALT = "dataEncryptSalt7";
    private static final String IV = "dataEncryptIV328";
    private JTextArea inputArea;


    private Map<Integer, TaskItem> idToTaskMap = new HashMap<>();
    private List<TaskItem> allTasks = new ArrayList<>();

    private TaskItem selectedTask = null;

    public ArkIVv7() {

        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = g.getMaximumWindowBounds();
        GraphicsDevice gd = g.getDefaultScreenDevice();

        int width = 1200;
        int height = bounds.height;

        frame = new JFrame("ArkIV");
        //frame.setSize(width, height);
        frame.getContentPane().setBackground(UniversalThemes.BG_MAIN);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);

        taskPanel = new JPanel();
        taskPanel.setBackground(UniversalThemes.BG_MAIN);

        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(taskPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(UniversalThemes.BORDER_COLOR, 1));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(35);
        scrollPane.getViewport().setBackground(UniversalThemes.BG_MAIN);
        UniversalThemes.applyScrollbarTheme(scrollPane);
        frame.add(scrollPane, BorderLayout.CENTER);

        JTextArea inputArea = new JTextArea(3, 30);
//      inputArea.setFont(UniversalThemes.UI_FONT_BIG2);
        inputArea.setFont(UniversalThemes.getCompositeFont(20));  //Provides Emoji support for inputArea, but when entered the taskItem does not recognise it
        inputArea.setBackground(UniversalThemes.BG_COMPONENT);
        inputArea.setForeground(UniversalThemes.TXT_PRIMARY);
        inputArea.setCaretColor(UniversalThemes.ACCENT_COLOR);
        inputArea.setBorder(
                BorderFactory.createLineBorder(UniversalThemes.BORDER_COLOR, 2)
        );

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setMargin(new Insets(8, 8, 8, 8)); // Padding inside text area

        // Set up scroll pane
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.getViewport().setBackground(UniversalThemes.BG_PANEL);
        inputScroll.setBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UniversalThemes.BORDER_COLOR)
        );
        UniversalThemes.applyScrollbarTheme(inputScroll);

        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setPreferredSize(new Dimension(700, 90)); // Fixed initial size

        // Auto-grow implementation
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
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
                int rows = inputArea.getLineCount();
                if (rows > inputArea.getRows()) {
                    inputArea.setRows(Math.min(rows, 10)); // Max 10 visible rows
                    inputScroll.revalidate();
                }
            }
        });

        // Key bindings for Shift+Enter (new line) and Enter (submit)
        InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputArea.getActionMap();

        // SHIFT + ENTER → New Line
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-newline");
        am.put("insert-newline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int caretPos = inputArea.getCaretPosition();
                String text = inputArea.getText();
                inputArea.setText(text.substring(0, caretPos) + "\n" + text.substring(caretPos));
                inputArea.setCaretPosition(caretPos + 1); // Move cursor to new line
            }
        });

        // ENTER → Submit Note
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit-note");
        am.put("submit-note", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createTask(); // existing task creation method
            }
        });

        frame.add(inputScroll, BorderLayout.SOUTH);

        // NEW: Add window listener to deselect everything on close (resets transient selection state)
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                deselectAll();  // Clean up selection before exit (no impact on saved "done" state)
                // Save is already called on moves/edits, but force one last save if needed
                saveTasks();
            }
        });

        loadTasks();
        frame.setVisible(true);
    }


    private void createTask() {
        JTextArea inputArea = (JTextArea) ((JViewport)((JScrollPane)frame.getContentPane().getComponent(1)).getComponent(0)).getComponent(0);
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



    private void addTaskFromInput(String text) {
        text = text.trim();
        if (!text.isEmpty()) {
            TaskItem task;
            if (selectedTask != null && !selectedTask.isSubtask()) {
                task = new TaskItem(taskCounter++, text, false, true, false, selectedTask.getId());
            } else {
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
                StringBuilder encryptedBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    encryptedBuilder.append(line).append("\n");
                }

                String decryptedData = decrypt(encryptedBuilder.toString());
                BufferedReader dataReader = new BufferedReader(new StringReader(decryptedData));
                String dataLine;
                allTasks.clear();  // Clear any existing (prevents duplicates if reloading)
                idToTaskMap.clear();  // Clear map too
                while ((dataLine = dataReader.readLine()) != null) {
                    String[] parts = dataLine.split("\\|", 6);
                    if (parts.length == 6) {
                        int id = Integer.parseInt(parts[0]);
                        int parentId = Integer.parseInt(parts[1]);

                        // Restore original pipes in text and line breaks
                        String text = parts[2].replace("%%PIPE_ESCAPE%%", "|") // Replace escape sequence with pipe
                                .replace("\\n", "\n"); // Restore line breaks

                        boolean done = parts[3].equals("1");
                        boolean isSubtask = parts[4].equals("1");
                        boolean isCollapsed = parts[5].equals("1");

                        taskCounter = Math.max(taskCounter, id + 1);
                        TaskItem task = new TaskItem(id, text, done, isSubtask, isCollapsed, parentId);
                        allTasks.add(task);  // Add in file order (preserves saved/moved order)
                        idToTaskMap.put(id, task);
                    }
                }

                // Separate main tasks and subtasks, preserving order from allTasks
                List<TaskItem> mainTasks = new ArrayList<>();
                for (TaskItem task : allTasks) {  // Loop preserves order
                    if (!task.isSubtask()) {
                        mainTasks.add(task);
                        taskPanel.add(task); // Add main tasks in saved order (skips subtasks)
                    }
                    // Subtasks are intentionally NOT added here -- they'll be inserted via showSubtasks()
                }

                // Now show subtasks after main tasks are added (uses fixed showSubtasks, no ID sort)
                for (TaskItem task : mainTasks) {  // mainTasks is in saved order
                    if (!task.isCollapsed()) {
                        showSubtasks(task);  // Now preserves subtask order too
                    }
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Error loading tasks (decryption failed)");
                e.printStackTrace(); // Print stack trace for debugging
            }
        }
    }




    private void saveTasks() {
        try {
            StringBuilder plainBuilder = new StringBuilder();
            for (TaskItem t : allTasks) {
                // Escape pipes in task text before saving
                String escapedText = t.getRawText().replace("|", "%%PIPE_ESCAPE%%")
                        .replace("\n", "\\n"); // Replace line breaks with a placeholder

                plainBuilder.append(t.getId()).append("|")
                        .append(t.getParentId()).append("|")
                        .append(escapedText) // Use the escaped text
                        .append("|")
                        .append(t.isDone() ? "1" : "0").append("|")
                        .append(t.isSubtask() ? "1" : "0").append("|")
                        .append(t.isCollapsed() ? "1" : "0").append("\n");
            }

            String encryptedText = encrypt(plainBuilder.toString());
            try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
                writer.print(encryptedText);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error saving tasks (encryption failed)");
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

    private class TaskItem extends JPanel {
        private int id;
        private int parentId;
        private boolean isSubtask;
        private boolean isCollapsed;
        private JCheckBox checkBox;
        private JTextArea textArea;
        private Timer flickerTimer;
        private boolean isSelected = false;

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
                outerBorder = BorderFactory.createMatteBorder(5, 30, 2, 30, UniversalThemes.BORDER_COLOR);
            } else {
                outerBorder = BorderFactory.createMatteBorder(1, 0, isCollapsed ? 10 : 0, 0, UniversalThemes.BORDER_COLOR);
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

            JButton editButton = new JButton(" ✎ ");
            editButton.setText("<html><div style='margin-top:3px;'>✎</div></html>");

            editButton.setFont(UniversalThemes.UI_FONT_EMOJI);
            editButton.setBackground(UniversalThemes.ACCENT_COLOR);
            editButton.setForeground(UniversalThemes.TXT_SELECTED);
            editButton.setBorder(new LineBorder(UniversalThemes.ACCENT_COLOR_DARK, 2));
            editButton.setPreferredSize(new Dimension(40, 29));
            UniversalThemes.ClickEffect(editButton);
            editButton.addActionListener(e -> editTask());

            JButton deleteButton = new JButton(" \uD83D\uDDD1\uFE0F ");
            deleteButton.setText("<html><div style='margin-top:3px;'>🗑️</div></html>");
            deleteButton.setFont(UniversalThemes.UI_FONT_EMOJI);
            deleteButton.setBackground(UniversalThemes.ACCENT_COLOR);
            deleteButton.setForeground(UniversalThemes.TXT_SELECTED);
            deleteButton.setBorder(new LineBorder(UniversalThemes.ACCENT_COLOR_DARK, 2));
            deleteButton.setPreferredSize(new Dimension(40, 29));

            UniversalThemes.ClickEffect(deleteButton);

            deleteButton.addActionListener(e -> confirmDeleteTask());

            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);

            if (!isSubtask) {
                JButton createSubtaskButton = new JButton(" ➕ ");
                createSubtaskButton.setText("<html><div style='margin-top:1px;'>➕</div></html>");
                createSubtaskButton.setFont(UniversalThemes.UI_FONT_EMOJI1);
                createSubtaskButton.setBackground(UniversalThemes.ACCENT_COLOR);
                createSubtaskButton.setForeground(UniversalThemes.TXT_SELECTED);
                createSubtaskButton.setBorder(new LineBorder(UniversalThemes.ACCENT_COLOR_DARK, 2));
                createSubtaskButton.setPreferredSize(new Dimension(40, 29));
                UniversalThemes.ClickEffect(createSubtaskButton);

                createSubtaskButton.addActionListener(e -> createSubtask());
                buttonPanel.add(createSubtaskButton);

                addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            TaskItem.this.isCollapsed = !TaskItem.this.isCollapsed;
                            Border newOuter = BorderFactory.createMatteBorder(1, 0, TaskItem.this.isCollapsed ? 10 : 0, 0, UniversalThemes.BORDER_COLOR);
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

            // New: Add key bindings for Ctrl+Shift+W (move up) and Ctrl+Shift+S (move down)  
            InputMap im = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap am = this.getActionMap();

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "moveUp");
            am.put("moveUp", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isSelected) {
                        moveTaskUp();
                    }
                }
            });

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "moveDown");
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


        private void editTask() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(UniversalThemes.BG_MAIN);

            JTextArea field = new JTextArea(getRawText(), 3, 30); // Start with current raw text
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
                            textArea.setForeground(Color.GRAY);
                        } else {
                            textArea.setForeground(UniversalThemes.TXT_PRIMARY);
                        }
                        saveTasks(); // Now saves the updated (raw or decorated) text
                    } else {
                        // Optional: Confirm deletion if text is empty
                        boolean confirmed = UniversalThemes.showConfirmPopup(
                                panel,
                                "Text is empty. Delete this task?",
                                "Confirm Delete"
                        );

                        if (confirmed) {
                            confirmDeleteTask(); // Reuse existing delete logic
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
            scrollPane.setBorder(BorderFactory.createLineBorder(UniversalThemes.BORDER_COLOR, 1));

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
            dialog.setVisible(true);
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
                    ? " Delete this task and its subtasks?"
                    : " Delete this task? ";

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

            JTextArea field = new JTextArea(3, 30); // Start with 3 visible rows
            field.setBackground(UniversalThemes.BG_PANEL);
            field.setForeground(UniversalThemes.TXT_PRIMARY);
            field.setCaretColor(UniversalThemes.ACCENT_COLOR);

            JScrollPane scrollPane = new JScrollPane(field);
            scrollPane.setBorder(BorderFactory.createLineBorder(UniversalThemes.BORDER_COLOR, 1));

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


