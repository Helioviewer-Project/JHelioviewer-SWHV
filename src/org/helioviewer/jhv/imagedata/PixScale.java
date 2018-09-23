package org.helioviewer.jhv.imagedata;

public abstract class PixScale {

    public static final double GAMMA = 1 / 2.2;

    protected short[] lut;

    public short get(long v) {
        if (v < 0)
            return lut[0];
        else if (v < lut.length)
            return lut[(int) v];
        else
            return lut[lut.length - 1];
    }

    public static class LinScale extends PixScale {
        public LinScale(long size) {
            double scale = 65535. / size;
            lut = new short[(int) (size + 1)];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * i + .5);
        }
    }

    public static class LogScale extends PixScale {
        public LogScale(long size) {
            double scale = 65535. / Math.log1p(size);
            lut = new short[(int) (size + 1)];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.log1p(i) + .5);
        }
    }

    public static class PowScale extends PixScale {
        public PowScale(long size) {
            double scale = 65535. / Math.pow(size, GAMMA);
            lut = new short[(int) (size + 1)];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.pow(i, GAMMA) + .5);
        }
    }

}
