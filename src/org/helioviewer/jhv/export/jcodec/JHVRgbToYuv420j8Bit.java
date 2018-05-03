package org.helioviewer.jhv.export.jcodec;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.image.MappedImageFactory;

import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

public class JHVRgbToYuv420j8Bit {

    private static void get(ByteBuffer data, int x, int y, int w, byte[] bgr) {
        //data.position(3 * (w * y + x)); -- slower
        //data.get(bgr);
        int i = 3 * (w * y + x);
        bgr[0] = data.get(i);
        bgr[1] = data.get(i + 1);
        bgr[2] = data.get(i + 2);
    }

    public static void transform(BufferedImage img, Picture dst) {
        int[][] dstData = dst.getData();
        int[][] out = new int[4][3];

        byte[] bgr = new byte[3];
        ByteBuffer data = MappedImageFactory.getByteBuffer(img);

        int x, y = 0, h = img.getHeight(), w = img.getWidth();
        int offChr = 0, offLuma = 0, strideDst = dst.getWidth();
        for (int i = 0; i < h >> 1; i++) {
            x = 0;
            for (int j = 0; j < w >> 1; j++) {
                get(data, x, y, w, bgr);
                rgb2yuv(bgr, out[0]);
                dstData[0][offLuma] = out[0][0];

                get(data, x, y + 1, w, bgr);
                rgb2yuv(bgr, out[1]);
                dstData[0][offLuma + strideDst] = out[1][0];

                ++offLuma;

                get(data, x + 1, y, w, bgr);
                rgb2yuv(bgr, out[2]);
                dstData[0][offLuma] = out[2][0];

                get(data, x + 1, y + 1, w, bgr);
                rgb2yuv(bgr, out[3]);
                dstData[0][offLuma + strideDst] = out[3][0];

                ++offLuma;

                dstData[1][offChr] = ((out[0][1] + out[1][1] + out[2][1] + out[3][1] + 2) >> 2);
                dstData[2][offChr] = ((out[0][2] + out[1][2] + out[2][2] + out[3][2] + 2) >> 2);

                ++offChr;
                x += 2;
            }
            offLuma += strideDst;
            y += 2;
        }
    }

    private static void rgb2yuv(byte[] bgr, int[] out) {
        int b = getUnsigned(bgr[0]);
        int g = getUnsigned(bgr[1]);
        int r = getUnsigned(bgr[2]);

        int y = 66 * r + 129 * g + 25 * b;
        int u = -38 * r - 74 * g + 112 * b;
        int v = 112 * r - 94 * g - 18 * b;
        y = (y + 128) >> 8;
        u = (u + 128) >> 8;
        v = (v + 128) >> 8;

        out[0] = MathUtil.clip(y + 16, 0, 255);
        out[1] = MathUtil.clip(u + 128, 0, 255);
        out[2] = MathUtil.clip(v + 128, 0, 255);
    }

    private static int getUnsigned(byte b) {
        return (b + 256) & 0xFF;
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
