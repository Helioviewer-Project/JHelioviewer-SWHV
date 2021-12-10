package org.helioviewer.jhv.events;

import java.awt.Color;

import static org.helioviewer.jhv.base.Colors.brightColors;

class EventColors {

    private static final int[] usedArray = new int[brightColors.length];
    private static int minValue = 0;

    static Color getNextColor() {
        while (true) {
            for (int i = 0; i < usedArray.length; i++) {
                if (usedArray[i] == minValue) {
                    usedArray[i]++;
                    return brightColors[i];
                }
            }
            minValue++;
        }
    }

    static void resetColor(Color c) {
        for (int i = 0; i < usedArray.length; i++) {
            if (brightColors[i].equals(c)) {
                usedArray[i]--;
                minValue = usedArray[i];
            }
        }
    }

    static void setColorUsed(Color c) {
        for (int i = 0; i < usedArray.length; i++) {
            if (brightColors[i].equals(c)) {
                usedArray[i]++;
            }
        }
    }

}
