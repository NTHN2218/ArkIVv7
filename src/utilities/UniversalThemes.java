package utilities;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class UniversalThemes {

    // ===== Theme Colors =====
    public static final Color BG_MAIN        = new Color(0x0F0F12);
    public static final Color BG_PANEL       = new Color(0x1A1A1E);
    public static final Color BG_COMPONENT   = new Color(0x222226);
    public static final Color BG_SIDEBAR     = new Color(0x1e1f22);
    public static final Color TXT_PRIMARY    = new Color(0xE5E5E5);
    public static final Color TXT_SECONDARY  = new Color(0xBEBEBE);
    public static final Color BORDER_COLOR1   = new Color(0x303036);
    public static final Color BORDER_COLOR2   = new Color(0x6A6A6A);


    public static final Color ACCENT_COLOR    = new Color(0x2fafbc);  //0xE67E22
    public static final Color ACCENT_COLOR_DARK = new Color(0x2b929d);  //0xC66A1A

//  public static final Color ACCENT_COLOR    = new Color(0xE67E22);
//  public static final Color ACCENT_COLOR_DARK = new Color(0xC66A1A);

    public static final Color TAB_SELECTED   = ACCENT_COLOR;
    public static final Color TAB_UNSELECTED = BG_PANEL;
    public static final Color DISABLED_TEXT  = new Color(0x6B6B6B);
    public static final Color TXT_SELECTED   = Color.BLACK;

    // ===== Fonts =====
    public static final Font UI_FONT_SMALL1      = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font UI_FONT_SMALL2     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font UI_FONT_SMALL3     = new Font("Segoe UI", Font.PLAIN, 16);

    public static final Font UI_FONT_BIG        = new Font("Segoe UI", Font.PLAIN, 18);
    public static final Font UI_FONT_BIG2       = new Font("Segoe UI", Font.PLAIN, 20);
    public static final Font UI_FONT_BIG4      = new Font("Segoe UI", Font.PLAIN, 21);
    public static final Font UI_FONT_BIG3       = new Font("Segoe UI", Font.PLAIN, 25);

    public static final Font UI_FONT_TITLE1     = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font UI_FONT_TITLE2     = new Font("Segoe UI", Font.PLAIN, 42);

    public static final Font UI_FONT_BOLD1       = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font UI_FONT_BOLD2      = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font UI_FONT_BOLD3      = new Font("Segoe UI", Font.BOLD, 22);

    public static final Font UI_FONT_EMOJI       = new Font("Segoe UI Emoji", Font.PLAIN, 18);
    public static final Font UI_FONT_EMOJI1       = new Font("Segoe UI Emoji", Font.PLAIN, 16);
    public static final Font UI_FONT_EMOJI2       = new Font("Segoe UI Emoji", Font.PLAIN, 20);
    public static final Font UI_FONT_EMOJI3       = new Font("Segoe UI Emoji", Font.PLAIN, 22);


    public static Font getCompositeFont(int size) {
        // Segoe UI Symbol covers ❌❎✅✔ AND renders normal Latin text fine
        // It is pre-installed on all Windows machines with Java
        // On other OS, Dialog falls back gracefully
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new Font("Segoe UI Symbol", Font.PLAIN, size);
        } else if (os.contains("mac")) {
            return new Font("Apple Color Emoji", Font.PLAIN, size);
        } else {
            return new Font("Noto Color Emoji", Font.PLAIN, size);
        }
    }


    // ===== UI Helpers =====
    public static class NoPressedButtonUI extends BasicButtonUI {
        @Override
        protected void paintButtonPressed(Graphics g, AbstractButton b) {
            // Disable default pressed effect
        }
    }

    public static void ClickEffect(JButton button) {

        Color normalBg = ACCENT_COLOR;
        Color hoverBg = ACCENT_COLOR_DARK;
        Color disabledBg = BG_COMPONENT;          // or darker shade if you want
        Color disabledFg = ACCENT_COLOR;

        // Initial paint
        button.setBackground(button.isEnabled() ? normalBg : disabledBg);
        button.setForeground(button.isEnabled() ? TXT_SELECTED : disabledFg);

        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(normalBg);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(normalBg);
            }
        });

        // Cursor should reflect disabled state
        button.addPropertyChangeListener("enabled", evt -> {
            if (button.isEnabled()) {
                button.setBackground(normalBg);
                button.setForeground(TXT_SELECTED);
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                button.setBackground(disabledBg);
                button.setForeground(disabledFg);
                button.setCursor(Cursor.getDefaultCursor());
            }
            button.repaint();
        });

        // Remove Swing focus glow (important for tab-like buttons)
        button.setFocusable(false);
    }


    public static void removeFocusFromAllButtons(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JButton btn) {
                btn.setFocusPainted(false);
                btn.setFocusable(false);
                btn.setUI(new NoPressedButtonUI());
            }
            if (comp instanceof Container cont) {
                removeFocusFromAllButtons(cont);
            }
        }
    }



    public static void applyScrollbarTheme(JScrollPane scrollPane) {

        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {

            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = UniversalThemes.ACCENT_COLOR; // Orange
                this.trackColor = UniversalThemes.BG_COMPONENT;     // Dark background
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fill thumb
                g2.setColor(thumbColor);
                g2.fillRect(thumbBounds.x+3, thumbBounds.y, thumbBounds.width-3, thumbBounds.height);

// Draw inner black rectangle with a 1px margin on all sides
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1f));
//                g2.drawRect(
//                        thumbBounds.x + 1,                // move 1px right
//                        thumbBounds.y + 1,                // little top margin (optional)
//                        thumbBounds.width - 3,            // shrink width so right side isn't clipped
//                        thumbBounds.height - 3            // shrink height to match style
//                );
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize(JComponent c) {
                return new Dimension(12, super.getPreferredSize(c).height);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });

        scrollPane.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {

            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = UniversalThemes.ACCENT_COLOR; // Orange
                this.trackColor = UniversalThemes.BG_SIDEBAR;     // Dark background
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fill thumb
                g2.setColor(thumbColor);
                g2.fillRect(thumbBounds.x, thumbBounds.y+3, thumbBounds.width, thumbBounds.height+3);

// Draw inner black rectangle with a 1px margin on all sides
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1f));
//                g2.drawRect(
//                        thumbBounds.x + 1,                // move 1px right
//                        thumbBounds.y + 1,                // little top margin (optional)
//                        thumbBounds.width - 3,            // shrink width so right side isn't clipped
//                        thumbBounds.height - 3            // shrink height to match style
//                );
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize(JComponent c) {
                return new Dimension( super.getPreferredSize(c).width,12);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });

        scrollPane.setCorner(
                JScrollPane.LOWER_RIGHT_CORNER,
                new JPanel() {{
                    setBackground(UniversalThemes.BG_PANEL); // match your UI
                }}
        );

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

    }

    public static void applyCheckBoxTheme(JCheckBox checkBox) {

        boolean[] isHovered = {false};

        checkBox.setUI(new javax.swing.plaf.basic.BasicCheckBoxUI() {
            @Override
            public synchronized void paint(Graphics g, JComponent c) {
                JCheckBox cb = (JCheckBox) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int boxSize = 12;
                int x = (c.getWidth() - boxSize) / 2;
                x=x-1;
                int y = (c.getHeight() - boxSize) / 2;

                // Draw background box
                g2.setColor(cb.isEnabled() ? BORDER_COLOR1 : new Color(0x1A1A1E));
                g2.fillRect(x, y, boxSize, boxSize);

                // Draw border
                g2.setColor(cb.isEnabled() ? BORDER_COLOR1 : new Color(0x3A3A40));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(x, y, boxSize, boxSize);

                // Draw checkmark if selected
                if (cb.isSelected()) {
                    // Fill with orange
                    g2.setColor(cb.isEnabled() ? ACCENT_COLOR : DISABLED_TEXT);
                    g2.fillRect(x, y, boxSize, boxSize);
                    g2.setColor(cb.isEnabled() ? BORDER_COLOR1 : new Color(0x3A3A40));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRect(x, y, boxSize, boxSize);


                    // Draw checkmark (white tick)
                    g2.setColor(cb.isEnabled() ? TXT_SELECTED : new Color(0x9A9A9A));
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 3, y + 6,  x + 4,  y + 9);
                    g2.drawLine(x + 4, y + 9, x + 10, y + 3);
                }

                // Hover highlight overlay
                if (isHovered[0]) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    g2.setColor(cb.isEnabled() ? Color.WHITE : ACCENT_COLOR);
                    g2.fillRect(x, y, boxSize, boxSize);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }

                // Draw label text
                FontMetrics fm = g2.getFontMetrics(cb.getFont());
                g2.setFont(cb.getFont());
                g2.setColor(cb.isEnabled() ? TXT_PRIMARY : DISABLED_TEXT);
                String text = cb.getText();
                if (text != null && !text.isEmpty()) {
                    int textX = x + boxSize + 6;
                    int textY = (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(text, textX, textY);
                }

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize(JComponent c) {
                JCheckBox cb = (JCheckBox) c;
                FontMetrics fm = cb.getFontMetrics(cb.getFont());
                int textWidth = (cb.getText() != null) ? fm.stringWidth(cb.getText()) : 0;
                return new Dimension(textWidth + 28, 20);
            }
        });

        // Remove default focus painting
        checkBox.setFocusPainted(false);
        checkBox.setFocusable(false);
        checkBox.setOpaque(true);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered[0] = true;
                checkBox.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered[0] = false;
                checkBox.repaint();
            }
        });

        // Repaint on enabled state change
        checkBox.addPropertyChangeListener("enabled", evt -> checkBox.repaint());

        checkBox.setForeground(TXT_PRIMARY);
        checkBox.setFont(UI_FONT_BIG);
    }



    public static void showPopup(Component parent, String message, String title) {
        // Create a custom modal dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        // Main panel with dark background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_MAIN);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        // Message label
        JLabel messageLabel = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>"); // Support multi-line
        messageLabel.setFont(UI_FONT_BIG);
        messageLabel.setForeground(TXT_PRIMARY);
        messageLabel.setBackground(BG_PANEL);
        messageLabel.setOpaque(false); // Ensure background is painted
        mainPanel.add(messageLabel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(BG_MAIN);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JButton okButton = new JButton("OK");
        okButton.setFont(UI_FONT_BIG);
        okButton.setBackground(ACCENT_COLOR);
        okButton.setForeground(TXT_SELECTED);
        okButton.setBorder(new LineBorder(ACCENT_COLOR_DARK, 2));
        okButton.setUI(new NoPressedButtonUI());
        okButton.setFocusPainted(false);
        okButton.setFocusable(false);
        UniversalThemes.ClickEffect(okButton);

        // Make the button a little wider (increase width by 20 pixels)
        Dimension currentSize = okButton.getPreferredSize();
        okButton.setPreferredSize(new Dimension(currentSize.width + 25, currentSize.height + 2));

        // Action to close dialog
        okButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent); // Center on parent
        dialog.setVisible(true); // Blocks until closed
    }

    public static boolean showConfirmPopup(Component parent, String message, String title) {

        final boolean[] result = { false };

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(parent),
                title,
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_MAIN);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel messageLabel = new JLabel(
                "<html>" + message.replace("\n", "<br>") + "</html>"
        );
        messageLabel.setFont(UI_FONT_BIG);
        messageLabel.setForeground(TXT_PRIMARY);
        mainPanel.add(messageLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        buttonPanel.setBackground(BG_MAIN);

        JButton yesButton = new JButton(" Yes ");
        JButton noButton  = new JButton(" No ");

        yesButton.setFont(UI_FONT_BIG);
        noButton.setFont(UI_FONT_BIG);

        yesButton.setBackground(ACCENT_COLOR);
        yesButton.setForeground(TXT_SELECTED);
        yesButton.setBorder(new LineBorder(ACCENT_COLOR_DARK, 2));

        noButton.setBackground(BG_COMPONENT);
        noButton.setForeground(TXT_PRIMARY);
        noButton.setBorder(new LineBorder(ACCENT_COLOR_DARK, 2));

        yesButton.setUI(new NoPressedButtonUI());
        noButton.setUI(new NoPressedButtonUI());

        UniversalThemes.ClickEffect(yesButton);
        UniversalThemes.ClickEffect(noButton);

        yesButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        noButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true); // blocks

        return result[0];
    }

    private static boolean menuUiManagerApplied = false;

    public static void applyMenuTheme(JMenu menu) {
        if (!menuUiManagerApplied) {
            UIManager.put("PopupMenu.border", BorderFactory.createEmptyBorder());
            UIManager.put("PopupMenu.background", BG_COMPONENT);
            UIManager.put("Menu.selectionBackground", BG_SIDEBAR);   // ← kills the dark box
            UIManager.put("Menu.selectionForeground", ACCENT_COLOR); // ← text color when open
            menuUiManagerApplied = true;
        }

        menu.setUI(new javax.swing.plaf.basic.BasicMenuUI() {
            @Override
            protected void paintBackground(Graphics g, JMenuItem item, Color bgColor) {
                // always keep sidebar color, no highlight box on the label
                g.setColor(BG_SIDEBAR);
                g.fillRect(0, 0, item.getWidth(), item.getHeight());
            }

            @Override
            protected void paintText(Graphics g, JMenuItem item,
                                     Rectangle textRect, String text) {
                ButtonModel model = item.getModel();
                g.setColor((model.isSelected() || model.isArmed())
                        ? ACCENT_COLOR
                        : TXT_PRIMARY);
                super.paintText(g, item, textRect, text);
            }
        });
    }

    public static void applyMenuItemTheme(JMenuItem item) {
        item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
            @Override
            protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
                ButtonModel model = menuItem.getModel();
                Color bg = model.isArmed() ? ACCENT_COLOR : BG_COMPONENT;
                g.setColor(bg);
                g.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
            }

            @Override
            protected void paintText(Graphics g, JMenuItem menuItem,
                                     Rectangle textRect, String text) {
                ButtonModel model = menuItem.getModel();
                g.setColor(model.isArmed() ? BG_MAIN : TXT_PRIMARY);
                super.paintText(g, menuItem, textRect, text);
            }
        });
    }

    public static void flashBorder(JComponent component, Color flashColor, Color normalColor) {
        final int[] count = {0};
        Timer timer = new Timer(100, null);
        timer.addActionListener(e -> {
            if (count[0] % 2 == 0) {
                component.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, flashColor));
            } else {
                component.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, normalColor));
            }
            count[0]++;
            if (count[0] >= 6) { // 3 flashes
                ((Timer) e.getSource()).stop();
                component.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, normalColor));
            }
        });
        timer.start();
    }

}