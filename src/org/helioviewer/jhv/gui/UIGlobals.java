package org.helioviewer.jhv.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.InputStream;
//import java.util.Enumeration;
//import java.util.Map;
//import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
//import javax.swing.plaf.FontUIResource;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.io.FileUtils;

public class UIGlobals {

    public static void setLaf() {
        try {
            com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme.setup();
            com.jidesoft.plaf.LookAndFeelFactory.installJideExtension();
        } catch (Exception e) {
            Log.error(e);
        }
        // listFontKeys();
        // listColorKeys();

        if (!System.getProperty("jhv.os").equals("mac")) {
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        }

        foreColor = UIManager.getColor("Label.foreground");
        backColor = UIManager.getColor("Label.background");
        midColor = new Color((foreColor.getRed() + backColor.getRed()) / 2, (foreColor.getGreen() + backColor.getGreen()) / 2, (foreColor.getBlue() + backColor.getBlue()) / 2);

        if (System.getProperty("jhv.os").equals("mac")) {
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
        float defaultSize = uiFont.getSize();

        uiFontBold = uiFont.deriveFont(Font.BOLD);
        uiFontSmall = uiFont.deriveFont(defaultSize - 2);
        uiFontSmallBold = uiFontSmall.deriveFont(Font.BOLD);

        Font monoFont = UIManager.getFont("monospaced.font");
        uiFontMonoSmall = monoFont.deriveFont(defaultSize - 2);

        int arc = 6;
        UIManager.put("Button.arc", arc);
        UIManager.put("CheckBox.arc", arc);
        UIManager.put("Component.arc", arc);
        UIManager.put("ProgressBar.arc", arc);
        UIManager.put("TextComponent.arc", arc);
        // UIManager.put("Component.arrowType", "triangle");

        try (InputStream is = FileUtils.getResource("/fonts/DejaVuSansCondensed.ttf")) {
            canvasFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(canvasFont);
        } catch (Exception e) {
            Log.warn("Font not loaded correctly, fallback to default", e);
            canvasFont = new Font("SansSerif", Font.PLAIN, (int) defaultSize);
        }

        try (InputStream is = FileUtils.getResource("/fonts/materialdesignicons-webfont.ttf")) {
            uiFontMDI = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(uiFontMDI);
        } catch (Exception e) {
            Log.warn("Font not loaded correctly, fallback to default", e);
            uiFontMDI = new Font("SansSerif", Font.PLAIN, (int) defaultSize);
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

    public static Font uiFont;
    public static Font uiFontBold;

    public static Font uiFontSmall;
    public static Font uiFontSmallBold;

    public static Font uiFontMonoSmall;

    public static Font uiFontMDI;
    public static Font canvasFont;

    public static Cursor openHandCursor;
    public static Cursor closedHandCursor;

    public static Color foreColor;
    public static Color backColor;
    public static Color midColor;

    public static final boolean canBrowse = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    public static final int menuShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

}
