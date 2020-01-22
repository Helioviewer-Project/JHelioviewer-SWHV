package org.helioviewer.jhv.plugins.pfss;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.pfss.data.PfssCache;
import org.json.JSONObject;

public class PfssPlugin extends Plugin {

    private static final PfssCache pfssCache = new PfssCache();
    private static final PfssLayer layer = new PfssLayer(null);

    public static int downloads;

    public PfssPlugin() {
        super("PFSS", "Visualize PFSS model data");
    }

    public static PfssCache getPfsscache() {
        return pfssCache;
    }

    @Override
    public void install() {
        downloads = 0;
        JHVFrame.getLayers().add(layer);
    }

    @Override
    public void uninstall() {
        JHVFrame.getLayers().remove(layer);
        pfssCache.clear();
    }

    @Override
    public void saveState(JSONObject jo) {
    }

    @Override
    public void loadState(JSONObject jo) {
    }

}
