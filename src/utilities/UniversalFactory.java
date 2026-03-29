package utilities;

import javax.swing.*;

public class UniversalFactory {

    public static JMenu createMenuBar(String label) {
        JMenu menu = new JMenu(label);
        menu.setFont(UniversalThemes.UI_FONT_SMALL3);
        menu.setForeground(UniversalThemes.TXT_PRIMARY);
        menu.setBackground(UniversalThemes.BG_SIDEBAR);
        menu.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return menu;
    }

    public static JMenuItem createMenuBarItem(String label) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(UniversalThemes.UI_FONT_SMALL3);
        item.setForeground(UniversalThemes.TXT_PRIMARY);
        item.setBackground(UniversalThemes.BG_COMPONENT);
        item.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return item;
    }

}
