package org.helioviewer.jhv.gui;

import java.awt.Font;

public class UIGlobals {

    private static UIGlobals instance;

    private UIGlobals() {
    }

    // always use getSingletonInstance
    public static UIGlobals getSingletonInstance() {
        if (instance == null) {
            instance = new UIGlobals();

            if (System.getProperty("jhv.os").equals("mac")) {
                Font font;

                font = new Font("HelveticaNeue", Font.PLAIN, 12);
                if (font != null)
                    UIFont = font;

                font = new Font("HelveticaNeue", Font.PLAIN, 10);
                if (font != null)
                    UIFontSmall = font;

                font = new Font("HelveticaNeue", Font.BOLD, 10);
                if (font != null)
                    UIFontSmallBold = font;
            }
        }
        return instance;
    }

    public static Font UIFont = new Font("SansSerif", Font.PLAIN, 12);
    public static Font UIFontSmall = new Font("SansSerif", Font.PLAIN, 10);
    public static Font UIFontSmallBold = new Font("SansSerif", Font.BOLD, 10);

    public static Font UIFontMono = new Font("Courier", Font.PLAIN, 12);

}
