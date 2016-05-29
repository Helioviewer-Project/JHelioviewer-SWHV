package org.helioviewer.jhv.export.jcodec;

import java.awt.image.BufferedImage;

import org.jcodec.common.model.Picture;
import static org.jcodec.common.tools.MathUtil.clip;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Transforms Picture in RGB colorspace ( one plane, 3 integers per pixel ) to
 * Yuv420 colorspace output picture ( 3 planes, luma - 0th plane, cb - 1th
 * plane, cr - 2nd plane; cb and cr planes are half width and half haight )
 * 
 * TODO: implement jpeg colorspace instead of NTSC
 * 
 * @author The JCodec project
 * 
 */
public class JHVRgbToYuv420j8Bit {

    public static void transform(BufferedImage img, Picture dst) {
        int[][] dstData = dst.getData(); // byte for 0.2
        int[][] out = new int[4][3];

        int r, g, b, rgb;
        int x, y = 0, srcHeight = img.getHeight(), srcWidth = img.getWidth();

        int offChr = 0, offLuma = 0, strideDst = dst.getWidth();
        for (int i = 0; i < srcHeight >> 1; i++) {
            x = 0;
            for (int j = 0; j < srcWidth >> 1; j++) {
                // dstData[1][offChr] = 0;
                // dstData[2][offChr] = 0;

                rgb = img.getRGB(x, y);
                r = (rgb >> 16) & 0xff;
                g = (rgb >> 8) & 0xff;
                b = rgb & 0xff;
                rgb2yuv(r, g, b, out[0]);
                dstData[0][offLuma] = /*(byte)*/ out[0][0];

                rgb = img.getRGB(x, y + 1);
                r = (rgb >> 16) & 0xff;
                g = (rgb >> 8) & 0xff;
                b = rgb & 0xff;
                rgb2yuv(r, g, b, out[1]);
                dstData[0][offLuma + strideDst] = /*(byte)*/ out[1][0];

                ++offLuma;

                rgb = img.getRGB(x + 1, y);
                r = (rgb >> 16) & 0xff;
                g = (rgb >> 8) & 0xff;
                b = rgb & 0xff;
                rgb2yuv(r, g, b, out[2]);
                dstData[0][offLuma] = /*(byte)*/ out[2][0];

                rgb = img.getRGB(x + 1, y + 1);
                r = (rgb >> 16) & 0xff;
                g = (rgb >> 8) & 0xff;
                b = rgb & 0xff;
                rgb2yuv(r, g, b, out[3]);
                dstData[0][offLuma + strideDst] = /*(byte)*/ out[3][0];

                ++offLuma;

                dstData[1][offChr] = /*(byte)*/ ((out[0][1] + out[1][1] + out[2][1] + out[3][1] + 2) >> 2);
                dstData[2][offChr] = /*(byte)*/ ((out[0][2] + out[1][2] + out[2][2] + out[3][2] + 2) >> 2);

                ++offChr;
                x += 2;
            }
            offLuma += strideDst;
            y += 2;
        }
    }

    private static void rgb2yuv(int r, int g, int b, int[] out) {
        int y = 66 * r + 129 * g + 25 * b;
        int u = -38 * r - 74 * g + 112 * b;
        int v = 112 * r - 94 * g - 18 * b;
        y = (y + 128) >> 8;
        u = (u + 128) >> 8;
        v = (v + 128) >> 8;

        out[0] = clip(y + 16, 0, 255);
        out[1] = clip(u + 128, 0, 255);
        out[2] = clip(v + 128, 0, 255);
    }

/*
    private static void rgb2yuv_new(int r, int g, int b, int[] out) { // for 0.2
        int y = 77 * r + 150 * g + 15 * b;
        int u = -43 * r - 85 * g + 128 * b;
        int v = 128 * r - 107 * g - 21 * b;
        y = (y + 128) >> 8;
        u = (u + 128) >> 8;
        v = (v + 128) >> 8;

        out[0] = clip(y - 128, -128, 127);
        out[1] = clip(u, -128, 127);
        out[2] = clip(v, -128, 127);
    }
*/

}
