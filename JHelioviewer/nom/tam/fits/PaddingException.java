package nom.tam.fits;

/**
 * This exception is thrown if an error is found 
 * reading the padding following a valid FITS HDU.
 * This padding is required by the FITS standard, but
 * some FITS writes forego writing it.  To access such data
 * users can use something like:
 * 
 * <code>
 *     Fits f = new Fits("somefile");
 *     try {
 *          f.read();
 *     } catch (PaddingException e) {
 *          f.addHDU(e.getHDU());
 *     }
 * </code>
 * to ensure that a truncated HDU is included in the FITS object.
 * Generally the FITS file have already added any HDUs prior
 * to the truncatd one.
 */
public class PaddingException extends FitsException {

    /** The HDU where the error happened.
     */
    private BasicHDU truncatedHDU;

    /** 
     * When the error is thrown, the data object being
     * read must be supplied.  We initially create a dummy
     * header for this.  If someone is reading the entire
     * HDU, then they can trap the exception and set the header
     * to the appropriate value.
     */
    public PaddingException(Data datum) throws FitsException {
        truncatedHDU = FitsFactory.HDUFactory(datum.getKernel());
        // We want to use the original Data object... so
        truncatedHDU = FitsFactory.HDUFactory(truncatedHDU.getHeader(), datum);
    }

    public PaddingException(String msg, Data datum) throws FitsException {
        super(msg);
        truncatedHDU = FitsFactory.HDUFactory(datum.getKernel());
        truncatedHDU = FitsFactory.HDUFactory(truncatedHDU.getHeader(), datum);
    }

    void updateHeader(Header hdr) throws FitsException {
        truncatedHDU = FitsFactory.HDUFactory(hdr, truncatedHDU.getData());
    }

    public BasicHDU getTruncatedHDU() {
        return truncatedHDU;
    }
}
