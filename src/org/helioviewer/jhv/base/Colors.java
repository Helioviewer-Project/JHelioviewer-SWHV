package org.helioviewer.jhv.base;

import java.awt.Color;

import org.helioviewer.jhv.app.DisplaySettings;

public enum Colors {

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

    private final String displayName;
    private final Color awtColor;
    private final byte[] bytes;

    Colors(String _displayName, Color _awtColor) {
        displayName = _displayName;
        awtColor = _awtColor;
        bytes = bytes(_awtColor);
    }

    public Color awtColor() {
        return awtColor;
    }

    public byte[] bytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Colors parse(String serializedName) {
        return parse(serializedName, Blue);
    }

    public static Colors parse(String serializedName, Colors fallback) {
        String normalized = "grey".equalsIgnoreCase(serializedName) ? "gray" : serializedName;
        for (Colors color : values()) {
            if (color.name().equalsIgnoreCase(normalized))
                return color;
        }
        return fallback;
    }

    public static byte[] bytes(Color c) {
        return bytes(c.getRed(), c.getGreen(), c.getBlue());
    }

    public static byte[] bytes(Color c, double alpha) {
        return new byte[]{(byte) (c.getRed() * alpha), (byte) (c.getGreen() * alpha), (byte) (c.getBlue() * alpha), (byte) (255 * alpha)};
    }

    public static byte[] bytes(int r, int g, int b) {
        return bytes(r, g, b, 255);
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

    public static Color parseColor(String value) {
        return value.startsWith("#") ? Color.decode(value) : parse(value).awtColor;
    }

    public static final byte[] Null = {0, 0, 0, 0};

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
        private int next;

        public Data() {
            colors = switch (DisplaySettings.getUITheme()) {
                case Dark -> brightColors;
                case Light -> darkColors;
            };
        }

        public Color getNextColor() {
            Color color = colors[next++];
            if (next == colors.length)
                next = 0;
            return color;
        }
    }

}
