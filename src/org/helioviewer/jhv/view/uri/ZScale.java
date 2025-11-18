package org.helioviewer.jhv.view.uri;

// Copyright (C) 1999-2018
// Smithsonian Astrophysical Observatory, Cambridge, MA, USA
// transcribed from SAOImageDS9/tksao/frame/fitsdata.C

class ZScale {

    private static final short GOOD_PIXEL = 0;
    private static final short BAD_PIXEL = 1;
    private static final short REJECT_PIXEL = 2;

    private static final float ZSINDEF = 0;
    // smallest permissible sample
    private static final int ZSMIN_NPIXELS = 5;
    // max frac. of pixels to be rejected
    private static final double ZSMAX_REJECT = 0.5;
    // k-sigma pixel rejection factor
    private static final float ZSKREJ = 2.5f;
    // maximum number of fitline iterations
    private static final int ZSMAX_ITERATIONS = 5;

    // private static final float zContrast = 0.25f;

    private static int ZSNINT(double a) {
        return (int) (a + 0.5);
    }

    // flattenData -- Compute and subtract the fitted line from the data array,
    // returned the flattened data in FLAT.
    private static void zFlattenData(float[] sampleData, float[] flat, float[] x, int npix, float z0, float dz) {
        for (int i = 0; i < npix; i++)
            flat[i] = sampleData[i] - (x[i] * dz + z0);
    }

    // computeSigma -- Compute the root mean square deviation from the
    // mean of a flattened array.  Ignore rejected pixels.
    private static void zComputeSigma(float[] a, short[] badpix, int npix, float[] mean, float[] sigma) {
        int ngoodpix = 0;
        double sum = 0.0;
        double sumsq = 0.0;

        // Accumulate sum and sum of squares
        for (int i = 0; i < npix; i++)
            if (badpix[i] == GOOD_PIXEL) {
                float pixval = a[i];
                ngoodpix = ngoodpix + 1;
                sum = sum + pixval;
                sumsq = sumsq + pixval * pixval;
            }

        // Compute mean and sigma
        switch (ngoodpix) {
            case 0:
                mean[0] = ZSINDEF;
                sigma[0] = ZSINDEF;
                break;
            case 1:
                mean[0] = (float) sum;
                sigma[0] = ZSINDEF;
                break;
            default:
                mean[0] = (float) (sum / ngoodpix);
                double temp = sumsq / (ngoodpix - 1) - (sum * sum) / (ngoodpix * (ngoodpix - 1));
                if (temp < 0)       // possible with roundoff error
                    sigma[0] = 0;
                else
                    sigma[0] = (float) Math.sqrt(temp);
        }
    }

    // rejectPixels -- Detect and reject pixels more than "threshold" greyscale
    // units from the fitted line.  The residuals about the fitted line are given
    // by the "flat" array, while the raw data is in "data".  Each time a pixel
    // is rejected subtract its contributions from the matrix sums and flag the
    // pixel as rejected.  When a pixel is rejected reject its neighbors out to
    // a specified radius as well.  This speeds up convergence considerably and
    // produces a more stringent rejection criteria which takes advantage of the
    // fact that bad pixels tend to be clumped.  The number of pixels left in the
    // fit is returned as the function value.
    private static int zRejectPixels(float[] sampleData, float[] flat, float[] normx, short[] badpix, int npix,
                                     double[] sumxsqr, double[] sumxz, double[] sumx, double[] sumz, float threshold, int ngrow) {
        int ngoodpix = npix;
        float lcut = -threshold;
        float hcut = threshold;

        for (int i = 0; i < npix; i++) {
            if (badpix[i] == BAD_PIXEL)
                ngoodpix = ngoodpix - 1;
            else {
                float residual = flat[i];
                if (residual < lcut || residual > hcut) {
                    // Reject the pixel and its neighbors out to the growing
                    // radius.  We must be careful how we do this to avoid
                    // directional effects.  Do not turn off thresholding on
                    // pixels in the forward direction; mark them for rejection
                    // but do not reject until they have been thresholded.
                    // If this is not done growing will not be symmetric.
                    for (int j = Math.max(0, i - ngrow); j < Math.min(npix, i + ngrow); j++) {
                        if (badpix[j] != BAD_PIXEL) {
                            if (j <= i) {
                                double x = normx[j];
                                double z = sampleData[j];
                                sumxsqr[0] = sumxsqr[0] - (x * x);
                                sumxz[0] = sumxz[0] - z * x;
                                sumx[0] = sumx[0] - x;
                                sumz[0] = sumz[0] - z;
                                badpix[j] = BAD_PIXEL;
                                ngoodpix = ngoodpix - 1;
                            } else
                                badpix[j] = REJECT_PIXEL;
                        }
                    }
                }
            }
        }
        return ngoodpix;
    }

    // fitLine -- Fit a straight line to a data array of type real.  This is
    // an iterative fitting algorithm, wherein points further than ksigma from the
    // current fit are excluded from the next fit.  Convergence occurs when the
    // next iteration does not decrease the number of pixels in the fit, or when
    // there are no pixels left.  The number of pixels left after pixel rejection
    // is returned as the function value.
    private static int zFitLine(float[] sampleData, int npix, float[] zstart, float[] zslope, float krej, int ngrow, int maxiter) {
        float xscale;
        if (npix <= 0)
            return 0;
        else if (npix == 1) {
            zstart[0] = sampleData[0];
            zslope[0] = 0;

            return 1;
        } else
            xscale = (float) (2.0 / (npix - 1));

        // Allocate a buffer for data minus fitted curve, another for the
        // normalized X values, and another to flag rejected pixels.
        float[] flat = new float[npix];
        float[] normx = new float[npix];
        short[] badpix = new short[npix];

        // Compute normalized X vector.  The data X values [1:npix] are
        // normalized to the range [-1:1].  This diagonalizes the lsq matrix
        // and reduces its condition number.
        for (int i = 0; i < npix; i++)
            normx[i] = i * xscale - 1;

        // Fit a line with no pixel rejection.  Accumulate the elements of the
        // matrix and data vector.  The matrix M is diagonal with
        // M[1,1] = sum x**2 and M[2,2] = ngoodpix.  The data vector is
        // DV[1] = sum (data[i] * x[i]) and DV[2] = sum (data[i]).
        double[] sumxsqr = {0};
        double[] sumxz = {0};
        double[] sumx = {0};
        double[] sumz = {0};

        for (int j = 0; j < npix; j++) {
            float x = normx[j];
            float z = sampleData[j];
            sumxsqr[0] = sumxsqr[0] + (x * x);
            sumxz[0] = sumxz[0] + z * x;
            sumz[0] = sumz[0] + z;
        }

        // Solve for the coefficients of the fitted line
        float z0 = (float) (sumz[0] / npix);
        float dz = (float) (sumxz[0] / sumxsqr[0]);

        // Iterate, fitting a new line in each iteration. Compute the flattened
        // data vector and the sigma of the flat vector.  Compute the lower and
        // upper k-sigma pixel rejection thresholds.  Run down the flat array
        // and detect pixels to be rejected from the fit.  Reject pixels from
        // the fit by subtracting their contributions from the matrix sums and
        // marking the pixel as rejected.

        int ngoodpix = npix;
        int last_ngoodpix;
        int minpix = Math.max(ZSMIN_NPIXELS, (int) (npix * ZSMAX_REJECT));

        for (int niter = 0; niter < maxiter; niter++) {
            last_ngoodpix = ngoodpix;

            // Subtract the fitted line from the data array
            zFlattenData(sampleData, flat, normx, npix, z0, dz);

            // Compute the k-sigma rejection threshold.  In principle this
            // could be more efficiently computed using the matrix sums
            // accumulated when the line was fitted, but there are problems with
            // numerical stability with that approach.
            float[] mean = {0};
            float[] sigma = {0};
            zComputeSigma(flat, badpix, npix, mean, sigma);
            float threshold = sigma[0] * krej;

            // Detect and reject pixels further than ksigma from the fitted line.
            ngoodpix = zRejectPixels(sampleData, flat, normx, badpix, npix, sumxsqr, sumxz, sumx, sumz, threshold, ngrow);

            // Solve for the coefficients of the fitted line.  Note that after
            // pixel rejection the sum of the X values need no longer be zero.
            if (ngoodpix > 0) {
                double rowrat = sumx[0] / sumxsqr[0];
                z0 = (float) ((sumz[0] - rowrat * sumxz[0]) / (ngoodpix - rowrat * sumx[0]));
                dz = (float) ((sumxz[0] - z0 * sumx[0]) / sumxsqr[0]);
            }

            if (ngoodpix >= last_ngoodpix || ngoodpix < minpix)
                break;
        }

        // Transform the line coefficients back to the X range [1:npix]
        zstart[0] = z0 - dz;
        zslope[0] = dz * xscale;

        return ngoodpix;
    }

    // ZSCALE -- Compute the optimal Z1, Z2 (range of greyscale values to be
    // displayed) of an image.  For efficiency a statistical subsample of an image
    // is used.  The pixel sample evenly subsamples the image in x and y.  The
    // entire image is used if the number of pixels in the image is smaller than
    // the desired sample.
    //
    // The sample is accumulated in a buffer and sorted by greyscale value.
    // The median value is the central value of the sorted array.  The slope of a
    // straight line fitted to the sorted sample is a measure of the standard
    // deviation of the sample about the median value.  Our algorithm is to sort
    // the sample and perform an iterative fit of a straight line to the sample,
    // using pixel rejection to omit gross deviants near the endpoints.  The fitted
    // straight line is the transfer function used to map image Z into display Z.
    // If more than half the pixels are rejected the full range is used.  The slope
    // of the fitted line is divided by the user-supplied contrast factor and the
    // final Z1 and Z2 are computed, taking the origin of the fitted line at the
    // median value.
    static void zscale(float[] sample, int npix, float[] zLow, float[] zHigh, float[] zMax, int zContrast) {
        int center_pixel = Math.max(1, (npix + 1) / 2);

        // Sort the sample, compute the minimum, maximum, and median pixel values
        // Arrays.sort(sample, 0, npix); -- already sorted
        float zmin = sample[0];
        float zmax = sample[Math.max(npix, 1) - 1];
        zMax[0] = zmax;

        // The median value is the average of the two central values if there
        // are an even number of pixels in the sample.
        float left = sample[center_pixel - 1];
        float median;
        if ((npix & 1) == 1 || center_pixel >= npix)
            median = left;
        else
            median = (left + sample[center_pixel]) / 2;

        // Fit a line to the sorted sample vector.  If more than half of the
        // pixels in the sample are rejected give up and return the full range.
        // If the user-supplied contrast factor is not 1.0 adjust the scale
        // accordingly and compute zLow and zHigh, the y intercepts at indices 1 and
        // npix.
        int minpix = Math.max(ZSMIN_NPIXELS, (int) (npix * ZSMAX_REJECT));
        int ngrow = Math.max(1, ZSNINT(npix * .01));
        float[] zstart = {0};
        float[] zslope = {0};

        int ngoodpix = zFitLine(sample, npix, zstart, zslope, ZSKREJ, ngrow, ZSMAX_ITERATIONS);
        if (ngoodpix < minpix) {
            zLow[0] = zmin;
            zHigh[0] = zmax;
        } else {
            /* if (zContrast > 0) {
                zslope[0] = zslope[0] / zContrast;
            } */
            zslope[0] = zslope[0] * zContrast;

            zLow[0] = Math.max(zmin, median - (center_pixel - 1) * zslope[0]);
            zHigh[0] = Math.min(zmax, median + (npix - center_pixel) * zslope[0]);
        }
    }

}
