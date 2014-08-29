package org.helioviewer.gl3d.plugin.pfss.data;

import org.clapper.util.misc.FileHashMap;
import org.helioviewer.gl3d.plugin.pfss.data.dataStructure.PfssDayAndTime;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * Runnable class to load the Pfss-data in a thread
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssDataLoader implements Runnable {
    private final PfssDayAndTime dayAndTime;
    private final PfssFitsFile fitsFile;
    private final FileHashMap<String, PfssFitsFile> pfssDatas;

    public PfssDataLoader(PfssDayAndTime dayAndTime, PfssFitsFile fitsFile, FileHashMap<String, PfssFitsFile> pfssDatas) {
        this.dayAndTime = dayAndTime;
        this.fitsFile = fitsFile;
        this.pfssDatas = pfssDatas;
    }

    @Override
    public void run() {
        String baseUrl = PfssSettings.baseUrl + "webGL/streamdata.php?skip=0&filename=";
        String m = (dayAndTime.getMonth()) < 9 ? "0" + (dayAndTime.getMonth() + 1) : (dayAndTime.getMonth() + 1) + "";
        String url = baseUrl + dayAndTime.getUrl();
        fitsFile.loadFile(url);
        pfssDatas.put(dayAndTime.getUrl(), fitsFile);

    }

}
