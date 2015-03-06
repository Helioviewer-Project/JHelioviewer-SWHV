package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

public class TestOpenFits {

    public static void main(String[] args) {
        FileInputStream is;
        try {
            is = new FileInputStream(new File("/Users/freekv/swhv/build/data/pfss/testfile.fits"));

            Fits fits = new Fits(is, true);
            BasicHDU hdus[] = fits.read();
            BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];

            short[] fieldlinex = ((short[]) bhdu.getColumn("FIELDLINEx"));
            for (int i = 0; i < fieldlinex.length; i++) {
                System.out.println(fieldlinex[i]);
            }

            Header header = bhdu.getHeader();
            String date = header.findKey("DATE-OBS");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date ddate = null;
            //ddate.UTC(year, month, date, hrs, min, sec)
            try {
                ddate = dateFormat.parse(date.substring(11, 30));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            int year = Integer.parseInt(date.substring(11, 15));
            int month = Integer.parseInt(date.substring(16, 18));
            int day = Integer.parseInt(date.substring(19, 21));
            int hours = Integer.parseInt(date.substring(22, 24));
            int minutes = Integer.parseInt(date.substring(25, 27));
            int seconds = Integer.parseInt(date.substring(28, 30));
            System.out.println(ddate);
            System.out.println(year + " " + month + " " + day + "T" + hours + " " + minutes + " " + seconds);

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FitsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
