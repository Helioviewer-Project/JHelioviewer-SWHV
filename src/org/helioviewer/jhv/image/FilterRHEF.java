package org.helioviewer.jhv.image;

import java.util.Arrays;

import org.helioviewer.jhv.thread.ParallelRange;
import org.helioviewer.jhv.wcs.ImageGeometry;

// Radial Histogram Equalizing Filter (Gilly & DeForest 2024): rank-equalizes pixel
// values within ~1-pixel-wide annuli centered on the Sun, flattening the radial
// brightness gradient while preserving the relative structure at each height.
class FilterRHEF implements ImageFilter.Algorithm {

    // Annuli with fewer valid pixels are passed through unfiltered
    private static final int MIN_BIN_COUNT = 5;

    private final ImageGeometry geometry;

    FilterRHEF(ImageGeometry _geometry) {
        geometry = _geometry;
    }

    @Override
    public float[] filter(float[] data, int width, int height) {
        if (width < 1 || height < 1)
            return data;

        // Without valid pixel sizes, assume the Sun at the image center with pixel units.
        boolean valid = geometry.pixelSizeX > 0 && geometry.pixelSizeY > 0;
        double pixX = valid ? geometry.pixelSizeX : 1;
        double pixY = valid ? geometry.pixelSizeY : 1;
        double sunX = valid ? geometry.sunPixel.x : .5 * width;
        double sunY = valid ? geometry.sunPixel.y : .5 * height;
        double llx = -sunX * pixX, lly = -sunY * pixY;
        double invBinWidth = 1 / Math.min(pixX, pixY); // ~1-pixel-wide annuli

        double dxMax = Math.max(Math.abs(llx), Math.abs(llx + width * pixX));
        double dyMax = Math.max(Math.abs(lly), Math.abs(lly + height * pixY));
        int numBins = (int) (Math.sqrt(dxMax * dxMax + dyMax * dyMax) * invBinWidth) + 1;

        int length = width * height;
        int[] binOf = new int[length];
        double[] dx2 = new double[width];
        for (int x = 0; x < width; x++) {
            double dx = llx + (x + .5) * pixX;
            dx2[x] = dx * dx;
        }
        ParallelRange.run(height, (from, to) -> {
            for (int y = from; y < to; y++) {
                double dy = lly + (y + .5) * pixY;
                double dy2 = dy * dy;
                int rowBase = y * width;
                for (int x = 0; x < width; x++) {
                    binOf[rowBase + x] = (int) (Math.sqrt(dx2[x] + dy2) * invBinWidth);
                }
            }
        });

        // Counting sort of pixel indices by annulus
        int[] offset = new int[numBins + 1];
        for (int i = 0; i < length; i++)
            offset[binOf[i] + 1]++;
        for (int b = 0; b < numBins; b++)
            offset[b + 1] += offset[b];

        int[] order = new int[length];
        int[] cursor = Arrays.copyOf(offset, numBins);
        for (int i = 0; i < length; i++)
            order[cursor[binOf[i]]++] = i;

        ParallelRange.run(numBins, (from, to) -> {
            long[] packed = new long[0];
            for (int b = from; b < to; b++) {
                int lo = offset[b];
                int hi = offset[b + 1];
                if (hi - lo < MIN_BIN_COUNT)
                    continue;

                // Pack value bits with the pixel index; non-negative float bits sort numerically.
                // Zero pixels (detector padding, occulters) are excluded and stay zero.
                int binSize = hi - lo;
                if (packed.length < binSize) {
                    packed = new long[binSize];
                }
                int n = 0;
                for (int j = lo; j < hi; j++) {
                    int idx = order[j];
                    float v = data[idx];
                    if (v > 0)
                        packed[n++] = (long) Float.floatToRawIntBits(v) << 32 | idx;
                }
                if (n < MIN_BIN_COUNT)
                    continue;

                Arrays.sort(packed, 0, n);

                // Equal values get their average rank, like scipy.stats.rankdata(method="average")
                float invRange = 1f / (n - 1);
                int i = 0;
                while (i < n) {
                    long bits = packed[i] >>> 32;
                    int j = i;
                    while (j + 1 < n && packed[j + 1] >>> 32 == bits)
                        j++;
                    float value = .5f * (i + j) * invRange;
                    for (int k = i; k <= j; k++)
                        data[(int) packed[k]] = value;
                    i = j + 1;
                }
            }
        });
        return data;
    }

}
