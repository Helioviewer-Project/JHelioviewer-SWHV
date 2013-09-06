package ch.fhnw.jhv.plugins.vectors.data.importer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector2f;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;
import ch.fhnw.jhv.plugins.vectors.data.importer.parser.FitsVectorParser;
import ch.fhnw.jhv.plugins.vectors.data.importer.parser.IDLVectorParser;

/**
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorImporter {

    /**
     * Loads Vectors of a Textfile that was exported in IDL
     * 
     * @param path
     *            filepath
     * @param width
     *            width of the vectorfield
     * @param height
     *            height of the vectorfield
     * @param time
     *            time dimension
     * @return
     */
    public static VectorField loadIDLExport(String path, int width, int height, int time) {

        IDLVectorParser parser = new IDLVectorParser();
        VectorField vf = new VectorField();
        vf.vectors = parser.getVectorData(path, width, height, time);
        vf.sizePixel = new Vector2f(width, height);
        return vf;
    }

    /**
     * Loads Vectors of a Fits file
     * 
     * @param paths
     *            list of fits file paths
     * 
     * @return VectorField
     * 
     * @throws ObservationDateMissingException
     * @throws InconsistentVectorfieldSizeException
     */
    public static VectorField loadFITSExport(List<String> paths) throws FitsException, ObservationDateMissingException, InconsistentVectorfieldSizeException {

        FitsVectorParser parser = new FitsVectorParser();
        VectorField field = new VectorField();
        Map<Date, VectorData[]> map = new HashMap<Date, VectorData[]>();
        Date dates[] = new Date[paths.size()];
        int index = 0;
        int size = 0;
        int height = 0, width = 0;

        for (String path : paths) {
            Fits fits = null;
            BasicHDU image = null;

            try {
                fits = new Fits(path);
                image = fits.getHDU(0);
            } catch (IOException e) {
                // the FITS library throws an IO Exception for incorrect format
                // of fits file
                // we pack it into a FitsException because it's more appropriate
                throw new FitsException("The File is not a valid FITS Format", e);
            }

            Header header = image.getHeader();
            if (!((image).getKernel() instanceof float[][][]) || image.getBitPix() != BasicHDU.BITPIX_FLOAT) {
                System.out.println("only 3-dimensional FITS-Files of type float are currently supported - sorry for that");
                throw new FitsException("Only 3-dimensional FITS-Files with floating point type supported");
            }

            float[][][] images = (float[][][]) ((image).getKernel());

            // get the size from the vectorfield
            int h = images[0].length;
            int w = images[0][0].length;

            if (height == 0 && width == 0) {
                height = h;
                width = w;
            } else if (h != height || w != width) {
                throw new InconsistentVectorfieldSizeException(path);
            }

            size = height * width;

            String date = null;
            date = header.findKey("DATE-OBS");

            if (date == null) {
                date = header.getStringValue("DATE_OBS");
            }

            if (date == null) {
                throw new ObservationDateMissingException(path);
            }

            /*
             * The date in fits file is in ISO 8601 Format. (according to
             * http://www.lmsal.com/solarsoft/ssw_standards.html)
             * 
             * We just use the JAXB parser because "...since JAXB must be able
             * to parse ISO8601 date string according to the XML Schema
             * specification. ..." (found on
             * http://stackoverflow.com/questions/2201925
             * /converting-iso8601-compliant-string-to-java-util-date)
             */

            Calendar c = javax.xml.bind.DatatypeConverter.parseDateTime(date);
            dates[index] = c.getTime();
            map.put(dates[index], parser.parseFitsFile(image));
            index++;
        }

        Arrays.sort(dates);

        field.vectors = new VectorData[dates.length][size];
        field.sizePixel = new Vector2f(width, height);

        index = 0;
        for (Date date : dates) {
            field.vectors[index] = map.get(date);
            index++;
        }

        return field;
    }

}
