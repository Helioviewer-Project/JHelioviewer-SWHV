package org.helioviewer.jhv.base;

import java.awt.Color;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.DisplaySettings;

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

    public static float[] fade(float[] color, double alpha) {
        if (alpha == 1)
            return color;
        return new float[]{(float) (color[0] * alpha), (float) (color[1] * alpha), (float) (color[2] * alpha), (float) (color[3] * alpha)};
    }

    @Nullable
    public static Color parseColor(String name) {
        if (name.startsWith("#")) {
            try {
                return Color.decode(name);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        NamedColor color = NamedColor.parse(name, null);
        return color == null ? null : color.awtColor;
    }

    public static final byte[] Null = {0, 0, 0, 0};

    public enum NamedColor {
        Red("Red", Color.RED),
        Green("Green", Color.GREEN),
        ReducedGreen("Reduced Green", new Color(100, 175, 100)),
        Blue("Blue", Color.BLUE),
        Yellow("Yellow", Color.YELLOW),
        Orange("Orange", Color.ORANGE),
        Cyan("Cyan", Color.CYAN),
        Magenta("Magenta", Color.MAGENTA),
        Pink("Pink", Color.PINK),
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
            return parse(name, Blue);
        }

        @Nullable
        public static NamedColor parse(String name, @Nullable NamedColor fallback) {
            String normalized = "grey".equalsIgnoreCase(name) ? "gray" : name;
            for (NamedColor color : values()) {
                if (color.name().equalsIgnoreCase(normalized))
                    return color;
            }
            return fallback;
        }
    }

    public static final byte[] Red = NamedColor.Red.bytes();
    public static final byte[] Green = NamedColor.Green.bytes();
    public static final byte[] ReducedGreen = NamedColor.ReducedGreen.bytes();
    public static final byte[] Blue = NamedColor.Blue.bytes();
    public static final byte[] Yellow = NamedColor.Yellow.bytes();
    public static final byte[] Orange = NamedColor.Orange.bytes();
    public static final byte[] Cyan = NamedColor.Cyan.bytes();
    public static final byte[] Magenta = NamedColor.Magenta.bytes();
    public static final byte[] Pink = NamedColor.Pink.bytes();
    public static final byte[] White = NamedColor.White.bytes();
    public static final byte[] Black = NamedColor.Black.bytes();
    public static final byte[] Gray = NamedColor.Gray.bytes();
    public static final byte[] DarkGray = NamedColor.DarkGray.bytes();
    public static final byte[] LightGray = NamedColor.LightGray.bytes();

    public static final float[] WhiteFloat = {1, 1, 1, 1};
    public static final float[] LightGrayFloat = {.75f, .75f, .75f, 1};
    public static final float[] MiddleGrayFloat = {.5f, .5f, .5f, 1};

    private static final Color[] brightColors = {
            new Color(78, 121, 167),   // steel blue
            new Color(242, 142, 43),   // orange
            new Color(89, 161, 79),    // green
            new Color(196, 58, 250),   // purple
            new Color(237, 77, 78),    // red
            new Color(118, 183, 178),  // teal
            new Color(255, 209, 102),  // gold
            new Color(175, 122, 161),  // mauve
            new Color(190, 190, 190),  // gray
            new Color(142, 186, 229),  // light blue
            new Color(196, 156, 148),  // tan
            new Color(247, 182, 210),  // pink
            new Color(158, 218, 160),  // light green
            new Color(255, 187, 120),  // peach
            new Color(219, 219, 141),  // olive
            new Color(140, 158, 162),  // slate
            new Color(255, 124, 168),  // salmon
            new Color(100, 181, 246),  // sky blue
            new Color(192, 137, 54),   // brown
            new Color(200, 200, 100),  // lime
            new Color(232, 135, 192),  // orchid
            new Color(148, 216, 232),  // cyan
    };

    private static final Color[] darkColors = {
            new Color(55, 90, 127),    // darker steel blue
            new Color(192, 104, 21),   // darker orange
            new Color(57, 130, 49),    // darker green
            new Color(152, 36, 200),   // darker purple
            new Color(192, 48, 49),    // darker red
            new Color(76, 143, 138),   // darker teal
            new Color(200, 160, 52),   // darker gold
            new Color(131, 82, 121),   // darker mauve
            new Color(140, 140, 140),  // darker gray
            new Color(98, 146, 199),   // darker light blue
            new Color(156, 116, 108),  // darker tan
            new Color(197, 132, 160),  // darker pink
            new Color(108, 168, 110),  // darker light green
            new Color(205, 137, 70),   // darker peach
            new Color(169, 169, 91),   // darker olive
            new Color(98, 118, 122),   // darker slate
            new Color(195, 74, 118),   // darker salmon
            new Color(50, 131, 206),   // darker sky blue
            new Color(142, 97, 24),    // darker brown
            new Color(150, 150, 50),   // darker lime
            new Color(182, 85, 142),   // darker orchid
            new Color(98, 168, 182),   // darker cyan
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
