package org.helioviewer.jhv.gui;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class UIGlobals {

    private static UIGlobals instance;

    private UIGlobals() {
    }

    // always use getSingletonInstance
    public static UIGlobals getSingletonInstance() {
        if (instance == null) {
            instance = new UIGlobals();


            Font font = UIManager.getDefaults().getFont("Label.font");
            int defaultSize = font.getSize();

            String defaultFont = "SansSerif";
            if (System.getProperty("jhv.os").equals("mac")) { // scrap enormous Lucida Sans
                defaultFont = "HelveticaNeue";
                defaultSize -= 1;
            }

            HashMap<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
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

            InputStream is = FileUtils.getResourceInputStream("/fonts/RobotoCondensed-Regular.ttf");
            try {
                UIFontRoboto = Font.createFont(Font.TRUETYPE_FONT, is);
            } catch (Exception e) {
                Log.warn("Font not loaded correctly, fallback to default");
                UIFontRoboto = new Font("SansSerif", Font.PLAIN, defaultSize);
            }
        }
        return instance;
    }

    public void setUIFont(Font font) {
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

    public static Font UIFontRoboto;

    public static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(16, 8), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());
    public static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(16, 8), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());

}
