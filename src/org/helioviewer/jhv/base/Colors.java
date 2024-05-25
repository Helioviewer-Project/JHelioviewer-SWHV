package org.helioviewer.jhv.base;

import java.awt.Color;

public class Colors {

    public static byte[] bytes(Color c) {
        return new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(), (byte) 255};
    }

    public static byte[] bytes(Color c, double alpha) {
        return new byte[]{(byte) (c.getRed() * alpha), (byte) (c.getGreen() * alpha), (byte) (c.getBlue() * alpha), (byte) (255 * alpha)};
    }

    public static byte[] bytes(int r, int g, int b) {
        return new byte[]{(byte) r, (byte) g, (byte) b, (byte) 255};
    }

    public static byte[] bytes(int r, int g, int b, int a) {
        return new byte[]{(byte) (r * a / 255f), (byte) (g * a / 255f), (byte) (b * a / 255f), (byte) a};
    }

    public static float[] floats(Color c, double alpha) {
        return new float[]{(float) (c.getRed() * alpha / 255), (float) (c.getGreen() * alpha / 255), (float) (c.getBlue() * alpha / 255), (float) alpha};
    }

    public static final byte[] Null = {0, 0, 0, 0};
    public static final byte[] Red = bytes(Color.RED);
    public static final byte[] Green = bytes(Color.GREEN);
    public static final byte[] ReducedGreen = {(byte) 100, (byte) 175, (byte) 100, (byte) 255};
    public static final byte[] Blue = bytes(Color.BLUE);
    public static final byte[] White = bytes(Color.WHITE);
    public static final byte[] Yellow = bytes(Color.YELLOW);
    public static final byte[] Gray = bytes(Color.GRAY);
    public static final byte[] DarkGray = bytes(Color.DARK_GRAY);
    public static final byte[] LightGray = bytes(Color.LIGHT_GRAY);

    public static final float[] WhiteFloat = {1, 1, 1, 1};
    public static final float[] LightGrayFloat = {.75f, .75f, .75f, 1};
    public static final float[] MiddleGrayFloat = {.5f, .5f, .5f, 1};

    private static final Color[] brightColors = {
            new Color(144, 238, 144),
            new Color(255, 140, 0),
            new Color(255, 0, 255),
            new Color(99, 184, 255),
            new Color(143, 188, 143),
            new Color(219, 112, 147),
            new Color(255, 222, 173),
            new Color(0, 255, 255),
            new Color(255, 0, 0),
            new Color(255, 105, 180),
            new Color(160, 32, 240),
            new Color(0, 255, 0),
            new Color(205, 92, 92),
            new Color(139, 0, 139),
            new Color(238, 201, 0),
            new Color(95, 158, 160),
            new Color(189, 183, 107),
            new Color(107, 142, 35),
            new Color(127, 255, 212),
            new Color(100, 149, 237),
            new Color(190, 190, 190),
            new Color(106, 90, 205)
    };

/*
    private static final Color[] darkerColors = { -- former BandColors
            new Color(80, 80, 80),
            new Color(204, 51, 0),
            new Color(255, 0, 255),
            new Color(0, 120, 28),
            new Color(99, 184, 255),
            new Color(143, 188, 143),
            new Color(219, 112, 147),
            new Color(255, 222, 173),
            new Color(0, 255, 255),
            new Color(255, 0, 0),
            new Color(255, 105, 180),
            new Color(160, 32, 240),
            new Color(0, 255, 0),
            new Color(205, 92, 92),
            new Color(139, 0, 139),
            new Color(238, 201, 0),
            new Color(95, 158, 160),
            new Color(189, 183, 107),
            new Color(107, 142, 35),
            new Color(127, 255, 212),
            new Color(100, 149, 237),
            new Color(190, 190, 190),
            new Color(106, 90, 205)
    };
*/

    public static class BrightColors {

        private final int[] used = new int[brightColors.length];
        private int minValue = 0;

        public Color getNextColor() {
            while (true) {
                for (int i = 0; i < used.length; i++) {
                    if (used[i] == minValue) {
                        used[i]++;
                        return brightColors[i];
                    }
                }
                minValue++;
            }
        }
/*
        public void resetColor(Color c) {
            for (int i = 0; i < used.length; i++) {
                if (brightColors[i].equals(c)) {
                    used[i]--;
                    minValue = used[i];
                }
            }
        }

        public void setColorUsed(Color c) {
            for (int i = 0; i < used.length; i++) {
                if (brightColors[i].equals(c)) {
                    used[i]++;
                }
            }
        }
*/
    }

}
