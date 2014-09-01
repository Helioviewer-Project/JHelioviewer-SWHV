package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.HashMap;
import java.util.LinkedHashMap;

<<<<<<< HEAD
=======
import org.clapper.util.misc.FileHashMap;
import org.helioviewer.base.logging.Log;
>>>>>>> remove multi catch
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
<<<<<<< HEAD
        years = new HashMap<Integer, PfssYear>();
=======
        Log.info("Set up Pfss cache in " + JHVDirectory.PLUGINS.getFile().toURI() + "pfsscache");
        try {
            this.pfssDatas = new FileHashMap<String, PfssFitsFile>(JHVDirectory.PLUGINS.getFile().toURI().getPath() + "/pfsscache",
                    FileHashMap.RECLAIM_FILE_GAPS | FileHashMap.TRANSIENT | FileHashMap.FORCE_OVERWRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.years = new HashMap<Integer, PfssYear>();
>>>>>>> remove multi catch
    }

    public void addData(int year, int month, int dayAndTime, String url) {
        if (!this.years.containsKey(year)) {
            this.years.put(year, new PfssYear(year));
        }
        PfssDayAndTime tmp = this.years.get(year).addMonth(year, month, dayAndTime, url);
        if (this.lastEntry != null) {
            this.lastEntry.addNext(tmp);
        }
        this.lastEntry = tmp;

    }

    public synchronized PfssDayAndTime findData(int year, int month, int dayAndTime) {
        if (this.years.containsKey(year)) {
            return this.years.get(year).findData(month, dayAndTime);
        }
        return null;
    }

    public PfssData getData() {
        if (this.pfssDatas.get(this.lastURL) != null) {
            return this.pfssDatas.get(this.lastURL).getData();
        }
        return null;
    }

    public void preloadData(int year, int month, int dayAndTime) {
<<<<<<< HEAD
        load = true;
        pfssDatas.clear();
=======
        this.load = true;
        if (this.pfssDatas != null) {
            this.pfssDatas.clear();
        }
>>>>>>> remove multi catch
        PfssDayAndTime tmp = findData(year, month, dayAndTime);

        if (tmp != null) {
            this.lastURL = tmp.getUrl();
            for (int i = 0; i < PfssSettings.PRELOAD; i++) {
                // if (tmp != null) {
                PfssFitsFile tmpFits = new PfssFitsFile();
<<<<<<< HEAD
                pfssDatas.put(tmp.getUrl(), tmpFits);
                Thread t = new Thread(new PfssDataLoader(tmp, tmpFits), "PFFSLoader");
=======
                Thread t = new Thread(new PfssDataLoader(tmp, tmpFits, this.pfssDatas), "PFFSLoader");
>>>>>>> remove multi catch

                t.start();
                tmp = tmp.getNext();
            }
        }

        this.load = false;

    }

    public PfssFitsFile getFitsToDelete() {
        PfssFitsFile toDelete = this.toDelete;
        this.toDelete = null;
        return toDelete;
    }

    public void updateData(int year, int month, int dayAndTime) {
        if (this.pfssDatas != null && !this.load) {
            PfssDayAndTime tmp = findData(year, month, dayAndTime);

            if (tmp != null) {

                PfssFitsFile fits = this.pfssDatas.get(tmp.getUrl());

                if (this.lastURL != tmp.getUrl()) {
                    this.toDelete = this.pfssDatas.get(this.lastURL);
                }

                this.lastURL = tmp.getUrl();

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
<<<<<<< HEAD
        Thread t = new Thread(new PfssDataLoader(dayAndTime, fits), "PFSSLOADER2");
=======
        Thread t = new Thread(new PfssDataLoader(dayAndTime, fits, this.pfssDatas), "PFSSLOADER2");
>>>>>>> remove multi catch
        t.start();
    }

    private void addFile(String url, PfssFitsFile fits) {
        this.pfssDatas.remove(url);
        this.pfssDatas.put(url, fits);
    }

    private void preload(PfssDayAndTime dayAndTime) {
        PfssDayAndTime tmp = dayAndTime;
        for (int i = 0; i < PfssSettings.PRELOAD; i++) {
            if (tmp.getNext() != null) {
                tmp = tmp.getNext();
                PfssFitsFile fits = this.pfssDatas.get(tmp.getUrl());
                if (fits == null) {
                    fits = new PfssFitsFile();
                    this.loadFile(tmp, fits);
                    checkList();
                }
                this.addFile(tmp.getUrl(), fits);

            } else {
                break;
            }
        }
    }

    private void checkList() {
<<<<<<< HEAD
        if (pfssDatas.size() > PfssSettings.CACHE_SIZE) {
            String url = "";
            for (String s : pfssDatas.keySet())
                url = s;
            pfssDatas.remove(url);
=======
        if (this.pfssDatas.size() > PfssSettings.CACHE_SIZE) {
            for (String s : this.pfssDatas.keySet()) {
            }
>>>>>>> remove multi catch
        }
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
