package org.helioviewer.gl3d.plugin.pfss.data;

import org.helioviewer.gl3d.plugin.pfss.data.dataStructure.PfssDayAndTime;

/**
 * Runnable class to load the Pfss-data in a thread
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssDataLoader implements Runnable {
    private final PfssDayAndTime dayAndTime;
    private final PfssFitsFile fitsFile;

    public PfssDataLoader(PfssDayAndTime dayAndTime, PfssFitsFile fitsFile) {
        this.dayAndTime = dayAndTime;
        this.fitsFile = fitsFile;

    }

    @Override
    public void run() {
        String m = (dayAndTime.getMonth()) < 9 ? "0" + (dayAndTime.getMonth() + 1) : (dayAndTime.getMonth() + 1) + "";
        String url = "http://swhv.oma.be/magtest/fileraw_short.dat";//PfssSettings.INFOFILE_URL + dayAndTime.getYear() + "/" + m + "/" + dayAndTime.getUrl();
        fitsFile.loadFile(url);

    }

}
