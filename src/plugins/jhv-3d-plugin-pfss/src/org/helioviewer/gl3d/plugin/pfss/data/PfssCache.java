package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.helioviewer.gl3d.plugin.pfss.data.dataStructure.PfssDayAndTime;
import org.helioviewer.gl3d.plugin.pfss.data.dataStructure.PfssYear;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * Datastructur to cache the Pfss-Data with preload function
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssCache {

    private final LinkedHashMap<String, PfssFitsFile> pfssDatas = new LinkedHashMap<String, PfssFitsFile>();
    private final HashMap<Integer, PfssYear> years;
    private PfssDayAndTime lastEntry = null;
    private boolean visible = false;
    private boolean load = false;
    private String lastURL = "";
    private PfssFitsFile toDelete = null;

    /**
     * The private constructor to support the singleton pattern.
     * */
    public PfssCache() {
        years = new HashMap<Integer, PfssYear>();
    }

    public void addData(int year, int month, int dayAndTime, String url) {
        if (!years.containsKey(year))
            years.put(year, new PfssYear(year));
        PfssDayAndTime tmp = years.get(year).addMonth(year, month, dayAndTime, url);
        if (lastEntry != null)
            lastEntry.addNext(tmp);
        lastEntry = tmp;

    }

    public synchronized PfssDayAndTime findData(int year, int month, int dayAndTime) {
        if (years.containsKey(year)) {
            return years.get(year).findData(month, dayAndTime);
        }
        return null;
    }

    public PfssData getData() {
        if (pfssDatas.get(lastURL) != null)
            return pfssDatas.get(lastURL).getData();
        return null;
    }

    public void preloadData(int year, int month, int dayAndTime) {
        load = true;
        pfssDatas.clear();
        PfssDayAndTime tmp = findData(year, month, dayAndTime);

        if (tmp != null) {
            lastURL = tmp.getUrl();
            for (int i = 0; i < PfssSettings.PRELOAD; i++) {
                //if (tmp != null) {
                PfssFitsFile tmpFits = new PfssFitsFile();
                pfssDatas.put(tmp.getUrl(), tmpFits);
                Thread t = new Thread(new PfssDataLoader(tmp, tmpFits), "PFFSLoader");

                t.start();
                tmp = tmp.getNext();
            }
        }

        load = false;

    }

    public PfssFitsFile getFitsToDelete() {
        PfssFitsFile toDelete = this.toDelete;
        this.toDelete = null;
        return toDelete;
    }

    public void updateData(int year, int month, int dayAndTime) {
        if (pfssDatas != null && !load) {
            PfssDayAndTime tmp = findData(year, month, dayAndTime);

            if (tmp != null) {

                PfssFitsFile fits = pfssDatas.get(tmp.getUrl());

                if (lastURL != tmp.getUrl()) {
                    toDelete = pfssDatas.get(lastURL);
                }

                lastURL = tmp.getUrl();

                if (fits == null) {
                    fits = new PfssFitsFile();
                    loadFile(tmp, fits);
                }
                this.addFile(tmp.getUrl(), fits);
                this.preload(tmp);
            }
        }
    }

    private void loadFile(PfssDayAndTime dayAndTime, PfssFitsFile fits) {
        Thread t = new Thread(new PfssDataLoader(dayAndTime, fits), "PFSSLOADER2");
        t.start();
    }

    private void addFile(String url, PfssFitsFile fits) {
        pfssDatas.remove(url);
        pfssDatas.put(url, fits);
    }

    private void preload(PfssDayAndTime dayAndTime) {
        PfssDayAndTime tmp = dayAndTime;
        for (int i = 0; i < PfssSettings.PRELOAD; i++) {
            if (tmp.getNext() != null) {
                tmp = tmp.getNext();
                PfssFitsFile fits = pfssDatas.get(tmp.getUrl());
                if (fits == null) {
                    fits = new PfssFitsFile();
                    this.loadFile(tmp, fits);
                    checkList();
                }
                this.addFile(tmp.getUrl(), fits);

            } else
                break;
        }
    }

    private void checkList() {
        if (pfssDatas.size() > PfssSettings.CACHE_SIZE) {
            String url = "";
            for (String s : pfssDatas.keySet())
                url = s;
            pfssDatas.remove(url);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
