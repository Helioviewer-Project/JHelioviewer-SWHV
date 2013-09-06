package ch.fhnw.jhv.plugins.vectors.data.importer.parser;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import ch.fhnw.jhv.plugins.vectors.data.VectorData;

/**
 * Parser for FITS File that can load singel BasicHDU of FITS File
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 24.06.2011
 * 
 */
public class FitsVectorParser {

    /**
     * Read all the parameter of the VectorField inside the the specified Header
     * Data Unit
     * 
     * @param hdu
     *            FITS HDU to be parsed
     * @return Array of VectorData objects containg the vectors read from the
     *         HDU
     */
    public VectorData[] parseFitsFile(BasicHDU hdu) throws FitsException {

        int bitsPerPixel = 0;

        try {
            bitsPerPixel = hdu.getBitPix();
        } catch (FitsException e) {
            e.printStackTrace();
        }

        if (bitsPerPixel == BasicHDU.BITPIX_FLOAT && hdu.getKernel() instanceof float[][][]) {
            float[][][] images = (float[][][]) ((hdu).getKernel());
            if (images.length < 3) {
                System.out.println("At least three images necessary");
                throw new FitsException("At least three images are required");
            }
            int height = images[0].length;
            int width = images[0][0].length;

            VectorData[] ret = new VectorData[width * height];

            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    VectorData v = ret[h * width + w] = new VectorData();
                    v.length = images[0][h][w];
                    v.inclination = images[1][h][w];
                    v.azimuth = images[2][h][w];
                    v.x = w + 0.5f;
                    v.y = h + 0.5f;
                }
            }
            return ret;
        }
        return null;
    }
}
