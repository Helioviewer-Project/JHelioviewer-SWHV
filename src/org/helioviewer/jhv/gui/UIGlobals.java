package org.helioviewer.jhv.gui;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.log.Log;

import com.jidesoft.plaf.LookAndFeelFactory;

public class UIGlobals {

    private static final UIGlobals instance = new UIGlobals();

    private UIGlobals() {
        try {
            String laf = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(laf);
            if (laf.contains("gtk") || laf.contains("nimbus") || laf.contains("synth")) { // crash due to uninitialized defaults
                UIManager.getDefaults().put("RangeSliderUI", "com.jidesoft.plaf.basic.BasicRangeSliderUI");
                if (laf.contains("synth"))
                    UIManager.getDefaults().put("textHighlight", new ColorUIResource(255, 255, 255));
            }
            LookAndFeelFactory.installJideExtension();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!System.getProperty("jhv.os").equals("mac")) {
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        }

        Font font = new JLabel().getFont();
        int defaultSize = font.getSize();

        String defaultFont = "SansSerif";
        if (System.getProperty("jhv.os").equals("mac")) { // scrap enormous Lucida Sans
            defaultFont = "HelveticaNeue";
            defaultSize -= 1;

            closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND_MAC).getImage(), new Point(5, 1), IconBank.getIcon(JHVIcon.CLOSED_HAND_MAC).toString());
        } else {
            closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(16, 8), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());
        }
        openHandCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

        HashMap<TextAttribute, Object> map = new HashMap<>();
        map.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        map.put(TextAttribute.FAMILY, defaultFont);

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        map.put(TextAttribute.SIZE, defaultSize);

        font = new Font(map);
        UIFont = font;

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        map.put(TextAttribute.SIZE, defaultSize);
        UIFontBold = font.deriveFont(map);

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        map.put(TextAttribute.SIZE, defaultSize - 2);
        UIFontSmall = font.deriveFont(map);

        map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        map.put(TextAttribute.SIZE, defaultSize - 2);
        UIFontSmallBold = font.deriveFont(map);

        UIFontMono = new Font("Monospaced", Font.PLAIN, defaultSize);

        try (InputStream is = FileUtils.getResource("/fonts/RobotoCondensed-Regular.ttf")) {
            UIFontRoboto = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(UIFontRoboto);
        } catch (Exception e) {
            Log.warn("Font not loaded correctly, fallback to default");
            UIFontRoboto = new Font("SansSerif", Font.PLAIN, defaultSize);
        }
        try (InputStream is = FileUtils.getResource("/fonts/materialdesignicons-webfont.ttf")) {
            UIFontMDI = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(UIFontMDI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setUIFont(Font font) {
        FontUIResource f = new FontUIResource(font);
        Enumeration<?> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, f);
        }
    }

    public static Font UIFont;
    public static Font UIFontBold;

    public static Font UIFontSmall;
    public static Font UIFontSmallBold;

    public static Font UIFontMono;

    public static Font UIFontMDI;
    public static Font UIFontRoboto;

    public static Cursor openHandCursor;
    public static Cursor closedHandCursor;

}
