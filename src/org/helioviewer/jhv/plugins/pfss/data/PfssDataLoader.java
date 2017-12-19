package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.EventQueue;

import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.time.JHVDate;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;

class PfssDataLoader implements Runnable {

    private final long time;
    private final String url;

    PfssDataLoader(long _time, String _url) {
        time = _time;
        url = _url;
    }

    @Override
    public void run() {
        try (NetClient nc = NetClient.of(url); Fits fits = new Fits(nc.getStream())) {
            BasicHDU<?> hdus[] = fits.read();
            if (hdus == null || hdus.length < 2 || !(hdus[1] instanceof BinaryTableHDU))
                throw new Exception("Could not read FITS");

            BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
            Header header = bhdu.getHeader();

            String dateFits = header.getStringValue("DATE-OBS");
            if (dateFits == null)
                throw new Exception("DATE-OBS not found");
            JHVDate date = new JHVDate(dateFits);
            if (time != date.milli)
                throw new Exception("Inconsistent DATE-OBS. Expected " + new JHVDate(time) + ", got " + date);

            int points = header.getIntValue("HIERARCH.POINTS_PER_LINE");
            if (points == 0)
                throw new Exception("POINTS_PER_LINE not found");

            short[] flinex = (short[]) bhdu.getColumn("FIELDLINEx");
            short[] fliney = (short[]) bhdu.getColumn("FIELDLINEy");
            short[] flinez = (short[]) bhdu.getColumn("FIELDLINEz");
            short[] flines = (short[]) bhdu.getColumn("FIELDLINEs");
            if (flinex.length != fliney.length || flinex.length != flinez.length || flinex.length != flines.length)
                throw new Exception("Fieldline arrays not equal " + flinex.length + " " + fliney.length + " " + flinez.length + " " + flinex.length);

            PfssData pfssData = new PfssData(date, flinex, fliney, flinez, flines, points);
            EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().addData(time, pfssData));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
