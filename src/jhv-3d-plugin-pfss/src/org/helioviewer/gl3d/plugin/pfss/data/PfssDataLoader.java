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
        String url = "http://swhv.oma.be/magtest/webGL/streamdata.php?skip=2";
        fitsFile.loadFile(url);
    }

}
