package org.helioviewer.jhv.base;

import java.awt.Color;

public class Colors {

    public static final Color darkGray = Color.DARK_GRAY;
    public static final Color lightGray = Color.LIGHT_GRAY;

    public static byte[] bytes(Color c) {
        return new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(), (byte) 255};
    }

    public static byte[] bytes(Color c, double alpha) {
        return new byte[]{(byte) (c.getRed() * alpha), (byte) (c.getGreen() * alpha), (byte) (c.getBlue() * alpha), (byte) (255 * alpha)};
    }

    public static byte[] bytes(int r, int g, int b) {
        return new byte[]{(byte) r, (byte) g, (byte) b, (byte) 255};
    }

    public static float[] floats(Color c, double alpha) {
        return new float[]{(float) (c.getRed() * alpha / 255), (float) (c.getGreen() * alpha / 255), (float) (c.getBlue() * alpha / 255), (float) alpha};
    }

    public static final byte[] Null = {0, 0, 0, 0};
    public static final byte[] Black = bytes(Color.BLACK);
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

}
