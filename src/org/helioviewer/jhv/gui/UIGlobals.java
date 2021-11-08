package org.helioviewer.jhv.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.io.InputStream;
//import java.util.Enumeration;
import java.util.HashMap;
//import java.util.Map;
//import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
//import javax.swing.plaf.FontUIResource;

import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.log.Log;

import com.formdev.flatlaf.FlatLightLaf;
import com.jidesoft.plaf.LookAndFeelFactory;

public class UIGlobals {

    public static void setLaf() {
        try {
            FlatLightLaf.setup();
            LookAndFeelFactory.installJideExtension();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!System.getProperty("jhv.os").equals("mac")) {
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        }

        // listFontKeys();
        // listColorKeys();

        foreColor = UIManager.getColor("Label.foreground");
        backColor = UIManager.getColor("Label.background");
        midColor = new Color((foreColor.getRed() + backColor.getRed()) / 2, (foreColor.getGreen() + backColor.getGreen()) / 2, (foreColor.getBlue() + backColor.getBlue()) / 2);

        Font font = UIManager.getFont("defaultFont");
        String defaultFamily = font.getFamily();
        int defaultSize = font.getSize();

        if (System.getProperty("jhv.os").equals("mac")) {
            defaultSize -= 1;

            ImageIcon cursor = IconBank.getIcon(JHVIcon.CLOSED_HAND_MAC);
            cursor = cursor == null ? IconBank.getBlank() : cursor;
            closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursor.getImage(), new Point(5, 1), cursor.toString());

            System.setProperty("apple.laf.useScreenMenuBar", "true"); // proper menu bar
        } else {
            ImageIcon cursor = IconBank.getIcon(JHVIcon.CLOSED_HAND);
            cursor = cursor == null ? IconBank.getBlank() : cursor;
            closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursor.getImage(), new Point(16, 8), cursor.toString());
        }
        openHandCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

        HashMap<TextAttribute, Object> map = new HashMap<>();
        map.put(TextAttribute.FAMILY, defaultFamily);

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        map.put(TextAttribute.SIZE, defaultSize);

        font = new Font(map);
        uiFont = font;

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        map.put(TextAttribute.SIZE, defaultSize);
        uiFontBold = font.deriveFont(map);

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        map.put(TextAttribute.SIZE, defaultSize - 2);
        uiFontSmall = font.deriveFont(map);

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        map.put(TextAttribute.SIZE, defaultSize - 2);
        uiFontSmallBold = font.deriveFont(map);

        // uiFontMono = new Font("Monospaced", Font.PLAIN, defaultSize);
        uiFontMonoSmall = new Font("Monospaced", Font.PLAIN, defaultSize - 2);

        // setUIFont(uiFont);
        UIManager.getLookAndFeelDefaults().put("defaultFont", uiFont);

        try (InputStream is = FileUtils.getResource("/fonts/DroidSansFallback.ttf")) {
            canvasFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(canvasFont);
        } catch (Exception e) {
            Log.warn("Font not loaded correctly, fallback to default");
            canvasFont = new Font("SansSerif", Font.PLAIN, defaultSize);
        }

        try (InputStream is = FileUtils.getResource("/fonts/materialdesignicons-webfont.ttf")) {
            uiFontMDI = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(uiFontMDI);
        } catch (Exception e) {
            Log.warn("Font not loaded correctly, fallback to default");
            uiFontMDI = new Font("SansSerif", Font.PLAIN, defaultSize);
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
*/
/*
    private static void listFontKeys() {
        TreeSet<String> keys = new TreeSet<>();
        for (Map.Entry<Object, Object> entry : UIManager.getLookAndFeelDefaults().entrySet()) {
            if (entry.getValue() instanceof Font) {
                keys.add((String) entry.getKey());
            }
        }
        keys.forEach(System.out::println);
    }
*/
/*
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

    // public static Font uiFontMono;
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
