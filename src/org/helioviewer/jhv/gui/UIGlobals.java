package org.helioviewer.jhv.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.InputStream;
//import java.util.Enumeration;
import java.util.Map;
//import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
//import javax.swing.plaf.FontUIResource;

import org.helioviewer.jhv.app.DisplaySettings;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.movie.ExportMovie;

public final class UIGlobals {

    public static void setLaf() {
        if (Platform.isWindows())
            com.formdev.flatlaf.FlatLaf.setPreferredMonospacedFontFamily("Consolas");
        boolean dark = DisplaySettings.getUITheme() == DisplaySettings.UITheme.Dark;
        com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(Map.ofEntries(
                Map.entry("@background", dark ? "#252b34" : "#f3f5f8"),
                Map.entry("@foreground", dark ? "#dce3ec" : "#293544"),
                Map.entry("@componentBackground", dark ? "#1d232c" : "#ffffff"),
                Map.entry("@buttonBackground", dark ? "#303946" : "#ffffff"),
                Map.entry("@accentColor", dark ? "#78b8e6" : "#256da8"),
                Map.entry("@selectionBackground", dark ? "#354f68" : "#dcecf9"),
                Map.entry("@selectionForeground", dark ? "#f0f6ff" : "#183d5e"),
                Map.entry("@disabledForeground", dark ? "#7f8b9b" : "#7b8693"),
                Map.entry("Component.borderColor", dark ? "#424d5c" : "#c8d0db"),
                Map.entry("Separator.foreground", dark ? "#3a4553" : "#d0d7e0"),
                Map.entry("TabbedPane.selectedBackground", dark ? "#303b49" : "#e5edf5"),
                Map.entry("TabbedPane.underlineColor", "@accentColor"),
                Map.entry("TabbedPane.tabSelectionHeight", "2"),
                Map.entry("TabbedPane.tabHeight", "32"),
                Map.entry("ScrollBar.width", "10"),
                Map.entry("ScrollBar.track", "@background"),
                Map.entry("ScrollBar.thumb", dark ? "#536173" : "#aebbc9"),
                Map.entry("Tree.selectionInactiveBackground", dark ? "#354657" : "#e0eaf3"),
                Map.entry("Tree.selectionInactiveForeground", "@foreground"),
                Map.entry("List.selectionInactiveBackground", dark ? "#354657" : "#e0eaf3"),
                Map.entry("List.selectionInactiveForeground", "@foreground"),
                Map.entry("Table.selectionInactiveBackground", dark ? "#354657" : "#e0eaf3"),
                Map.entry("Table.selectionInactiveForeground", "@foreground"),
                Map.entry("Button.default.background", "@accentColor"),
                Map.entry("Button.default.foreground", dark ? "#152838" : "#ffffff"),
                Map.entry("Button.default.focusedBackground", "@accentColor"),
                Map.entry("Button.toolbar.selectedBackground", dark ? "#354f68" : "#dcecf9"),
                Map.entry("Button.toolbar.pressedBackground", dark ? "#42617e" : "#c6dff1"),
                Map.entry("Table.background", "@componentBackground"),
                Map.entry("Table.gridColor", dark ? "#303b49" : "#e5eaf0"),
                Map.entry("JHV.recordForeground", dark ? "#f28b91" : "#b52b40")
        ));
        try {
            switch (DisplaySettings.getUITheme()) {
                case Dark -> com.formdev.flatlaf.FlatDarkLaf.setup();
                case Light -> com.formdev.flatlaf.FlatLightLaf.setup();
            }
            com.jidesoft.plaf.LookAndFeelFactory.installJideExtension();
        } catch (Exception e) {
            Log.error(e);
        }
        // listFontKeys();
        // listColorKeys();

        if (!Platform.isMacOS()) {
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            UIManager.put("Popup.forceHeavyWeight", true);
        }

        foreColor = UIManager.getColor("Label.foreground");
        backColor = UIManager.getColor("Label.background");
        midColor = new Color((foreColor.getRed() + backColor.getRed()) / 2, (foreColor.getGreen() + backColor.getGreen()) / 2, (foreColor.getBlue() + backColor.getBlue()) / 2);

        if (Platform.isLinux())
            UIManager.getLookAndFeelDefaults().put("defaultFont", UIManager.getFont("small.font"));
        if (Platform.isMacOS()) {
            UIManager.getLookAndFeelDefaults().put("defaultFont", UIManager.getFont("medium.font")); // smaller, FlatLaf 2
            ImageIcon cursor = IconBank.getIcon(JHVIcon.CLOSED_HAND_MAC);
            cursor = cursor == null ? IconBank.getBlank() : cursor;
            closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursor.getImage(), new Point(5, 1), cursor.toString());
        } else {
            ImageIcon cursor = IconBank.getIcon(JHVIcon.CLOSED_HAND);
            cursor = cursor == null ? IconBank.getBlank() : cursor;
            closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursor.getImage(), new Point(16, 8), cursor.toString());
        }
        openHandCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

        uiFont = UIManager.getFont("defaultFont");
        uiFont = uiFont == null ? UIManager.getFont("Label.font") : uiFont; // when not FlatLaf
        float defaultSize = uiFont.getSize();

        uiFontBold = uiFont.deriveFont(Font.BOLD);
        uiFontSmall = uiFont.deriveFont(defaultSize - 1);
        uiFontSmallBold = uiFontSmall.deriveFont(Font.BOLD);

        uiFontMono = UIManager.getFont("monospaced.font");
        uiFontMono = uiFontMono == null ? new Font("Monospaced", Font.PLAIN, (int) defaultSize) : uiFontMono; // when not FlatLaf
        uiFontMonoSmall = uiFontMono.deriveFont(defaultSize - 1);

        int arc = 6;
        UIManager.put("Button.arc", arc);
        UIManager.put("ButtonPanel.order", "OCA");
        UIManager.put("CheckBox.arc", arc);
        UIManager.put("Component.arc", arc);
        UIManager.put("ProgressBar.arc", arc);
        UIManager.put("TextComponent.arc", arc);
        // UIManager.put("Component.arrowType", "triangle");

        sansFont = new Font("SansSerif", Font.PLAIN, (int) defaultSize);

        try (InputStream is = FileUtils.getResource("/fonts/materialdesignicons-webfont.ttf")) {
            uiFontMDI = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(uiFontMDI);
        } catch (Exception e) {
            Log.warn("Font not loaded correctly, fallback to default", e);
            uiFontMDI = sansFont;
        }

        // Set timeline panel colors
        switch (DisplaySettings.getUITheme()) {
            case Dark -> {
                TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR = backColor.darker();
                TL_SELECTED_INTERVAL_BACKGROUND_COLOR = TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR.darker();
                TL_INTERVAL_BORDER_COLOR = TL_SELECTED_INTERVAL_BACKGROUND_COLOR.darker();
                TL_BORDER_COLOR = TL_SELECTED_INTERVAL_BACKGROUND_COLOR.brighter();

                TL_TICK_LINE_COLOR = foreColor.darker().darker();
                TL_LABEL_TEXT_COLOR = foreColor;
                TL_TEXT_COLOR = foreColor;
                TL_TEXT_BACKGROUND_COLOR = backColor;

                TL_MOVIE_FRAME_COLOR = foreColor;
                ExportMovie.setMovieLineColor(foreColor);
                TL_MOVIE_INTERVAL_COLOR = backColor;
            }
            case Light -> {
                TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR = new Color(224, 224, 224);
                TL_SELECTED_INTERVAL_BACKGROUND_COLOR = new Color(247, 249, 252);
                TL_INTERVAL_BORDER_COLOR = TL_SELECTED_INTERVAL_BACKGROUND_COLOR.darker();
                TL_BORDER_COLOR = new Color(182, 190, 206);

                TL_TICK_LINE_COLOR = new Color(205, 214, 225);
                TL_LABEL_TEXT_COLOR = Color.BLACK;
                TL_TEXT_COLOR = Color.WHITE;
                TL_TEXT_BACKGROUND_COLOR = Color.GRAY;

                TL_MOVIE_FRAME_COLOR = Color.BLACK;
                ExportMovie.setMovieLineColor(Color.BLACK);
                TL_MOVIE_INTERVAL_COLOR = Color.LIGHT_GRAY;
            }
        }
    }
/*
    private static void setUIFont(Font font) {
        FontUIResource f = new FontUIResource(font);
        Enumeration<?> keys = UIManager.getLookAndFeelDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, f);
        }
    }

    private static void listFontKeys() {
        TreeSet<String> keys = new TreeSet<>();
        for (Map.Entry<Object, Object> entry : UIManager.getLookAndFeelDefaults().entrySet()) {
            if (entry.getValue() instanceof Font) {
                keys.add((String) entry.getKey());
            }
        }
        keys.forEach(System.out::println);
    }

    private static void listColorKeys() {
        TreeSet<String> keys = new TreeSet<>();
        for (Map.Entry<Object, Object> entry : UIManager.getLookAndFeelDefaults().entrySet()) {
            if (entry.getValue() instanceof Color) {
                keys.add((String) entry.getKey());
            }
        }
        keys.forEach(System.out::println);
    }
*/

    public static Font sansFont;

    public static Font uiFont;
    public static Font uiFontBold;

    public static Font uiFontSmall;
    public static Font uiFontSmallBold;

    public static Font uiFontMono;
    public static Font uiFontMonoSmall;

    public static Font uiFontMDI;

    public static Cursor openHandCursor;
    public static Cursor closedHandCursor;

    public static Color foreColor;
    public static Color backColor;
    public static Color midColor;

    // Timelines panel colors
    public static Color TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR;
    public static Color TL_SELECTED_INTERVAL_BACKGROUND_COLOR;
    public static Color TL_INTERVAL_BORDER_COLOR;
    public static Color TL_BORDER_COLOR;

    public static Color TL_TICK_LINE_COLOR;
    public static Color TL_LABEL_TEXT_COLOR;
    public static Color TL_TEXT_COLOR;
    public static Color TL_TEXT_BACKGROUND_COLOR;

    public static Color TL_MOVIE_FRAME_COLOR;
    public static Color TL_MOVIE_INTERVAL_COLOR;

    private UIGlobals() {}
}
