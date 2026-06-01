package org.helioviewer.jhv.base;

import java.awt.Color;

import org.helioviewer.jhv.DisplaySettings;

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

    public enum NamedColor {
        Red("Red", Color.RED),
        Green("Green", Color.GREEN),
        ReducedGreen("Reduced Green", new Color(100, 175, 100)),
        Blue("Blue", Color.BLUE),
        Yellow("Yellow", Color.YELLOW),
        Cyan("Cyan", Color.CYAN),
        Magenta("Magenta", Color.MAGENTA),
        White("White", Color.WHITE),
        Black("Black", Color.BLACK),
        Gray("Gray", Color.GRAY),
        DarkGray("Dark Gray", Color.DARK_GRAY),
        LightGray("Light Gray", Color.LIGHT_GRAY);

        private final String label;
        private final Color awtColor;
        private final byte[] bytes;

        NamedColor(String _label, Color _awtColor) {
            label = _label;
            awtColor = _awtColor;
            bytes = Colors.bytes(_awtColor);
        }

        public Color awtColor() {
            return awtColor;
        }

        public byte[] bytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return label;
        }

        public static NamedColor parse(String name) {
            try {
                return valueOf(name);
            } catch (RuntimeException e) {
                return Blue;
            }
        }
    }

    public static final byte[] Red = NamedColor.Red.bytes();
    public static final byte[] Green = NamedColor.Green.bytes();
    public static final byte[] ReducedGreen = NamedColor.ReducedGreen.bytes();
    public static final byte[] Blue = NamedColor.Blue.bytes();
    public static final byte[] Yellow = NamedColor.Yellow.bytes();
    public static final byte[] Cyan = NamedColor.Cyan.bytes();
    public static final byte[] Magenta = NamedColor.Magenta.bytes();
    public static final byte[] White = NamedColor.White.bytes();
    public static final byte[] Black = NamedColor.Black.bytes();
    public static final byte[] Gray = NamedColor.Gray.bytes();
    public static final byte[] DarkGray = NamedColor.DarkGray.bytes();
    public static final byte[] LightGray = NamedColor.LightGray.bytes();

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

    private static final Color[] darkColors = { // former BandColors
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

    public static class Data {

        private final Color[] colors;
        private final int[] used;
        private int minValue = 0;

        public Data() {
            colors = switch (DisplaySettings.getUITheme()) {
                case Dark -> brightColors;
                case Light -> darkColors;
            };
            used = new int[colors.length];
        }

        public Color getNextColor() {
            while (true) {
                for (int i = 0; i < used.length; i++) {
                    if (used[i] == minValue) {
                        used[i]++;
                        return colors[i];
                    }
                }
                minValue++;
            }
        }
/*
        public void resetColor(Color c) {
            for (int i = 0; i < used.length; i++) {
                if (colors[i].equals(c)) {
                    used[i]--;
                    minValue = used[i];
                }
            }
        }

        public void setColorUsed(Color c) {
            for (int i = 0; i < used.length; i++) {
                if (colors[i].equals(c)) {
                    used[i]++;
                }
            }
        }
*/
    }

}
