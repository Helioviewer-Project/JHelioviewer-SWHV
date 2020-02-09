package org.helioviewer.jhv.events;

import java.awt.Color;

class JHVCacheColors {

    private static final Color[] hekColorArray = {new Color(144, 238, 144), new Color(255, 140, 0), new Color(255, 0, 255), new Color(99, 184, 255), new Color(143, 188, 143), new Color(219, 112, 147), new Color(255, 222, 173), new Color(0, 255, 255), new Color(255, 0, 0), new Color(255, 105, 180), new Color(160, 32, 240), new Color(0, 255, 0), new Color(205, 92, 92), new Color(139, 0, 139), new Color(238, 201, 0), new Color(95, 158, 160), new Color(189, 183, 107), new Color(107, 142, 35), new Color(127, 255, 212), new Color(100, 149, 237), new Color(190, 190, 190), new Color(106, 90, 205),};
    private static final int[] usedArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int minValue = 0;

    static Color getNextColor() {
        while (true) {
            for (int i = 0; i < hekColorArray.length; i++) {
                if (usedArray[i] == minValue) {
                    usedArray[i]++;
                    return hekColorArray[i];
                }
            }
            minValue++;
        }
    }

    static void resetColor(Color c) {
        for (int i = 0; i < hekColorArray.length; i++) {
            if (hekColorArray[i].equals(c)) {
                usedArray[i]--;
                minValue = usedArray[i];
            }
        }
    }

    static void setColorUsed(Color c) {
        for (int i = 0; i < hekColorArray.length; i++) {
            if (hekColorArray[i].equals(c)) {
                usedArray[i]++;
            }
        }
    }

}
