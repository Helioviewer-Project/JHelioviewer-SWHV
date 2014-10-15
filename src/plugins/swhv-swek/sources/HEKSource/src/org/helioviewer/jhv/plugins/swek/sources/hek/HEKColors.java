package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.awt.Color;

public class HEKColors {
    private final static Color[] hekColorArray = { new Color(144, 238, 144), new Color(255, 140, 000), new Color(255, 255, 000),
            new Color(255, 000, 255), new Color(255, 255, 255), new Color(99, 184, 255), new Color(143, 188, 143),
            new Color(220, 220, 220), new Color(219, 112, 147), new Color(255, 222, 173), new Color(000, 255, 255),
            new Color(255, 000, 000), new Color(255, 250, 205), new Color(255, 105, 180), new Color(160, 032, 240),
            new Color(255, 225, 255), new Color(000, 139, 139), new Color(000, 255, 000), new Color(205, 92, 92), new Color(238, 224, 229),
            new Color(139, 000, 139), new Color(238, 201, 000), new Color(211, 211, 211), new Color(153, 153, 153),
            new Color(95, 158, 160), new Color(189, 183, 107), new Color(107, 142, 035), new Color(127, 255, 212),
            new Color(100, 149, 237), new Color(190, 190, 190), new Color(106, 90, 205), };
    private final static int[] usedArray = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static int minValue = 0;
    private static int printerColorCounter = 0;

    public synchronized static Color getNextColor() {
        for (int i = 0; i < hekColorArray.length; i++) {
            if (usedArray[i] == minValue) {
                usedArray[i]++;
                return hekColorArray[i];
            }
        }
        minValue++;
        return HEKColors.getNextColor();
    }

    public synchronized static void resetColor(Color c) {
        for (int i = 0; i < hekColorArray.length; i++) {
            if (hekColorArray[i].equals(c)) {
                usedArray[i]--;
                minValue = usedArray[i];
            }
        }
    }

    public synchronized static Color getPrinterColor() {
        if (printerColorCounter >= hekColorArray.length) {
            printerColorCounter = 0;
        }
        Color c = hekColorArray[printerColorCounter];
        printerColorCounter++;
        return c;
    }
}
