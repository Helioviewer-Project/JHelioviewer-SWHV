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
        String baseUrl = "http://127.0.0.1/web/webGL/streamdata.php?skip=0&filename=";
        String m = (dayAndTime.getMonth()) < 9 ? "0" + (dayAndTime.getMonth() + 1) : (dayAndTime.getMonth() + 1) + "";
        String url = baseUrl + dayAndTime.getUrl();
        fitsFile.loadFile(url);
    }

}
