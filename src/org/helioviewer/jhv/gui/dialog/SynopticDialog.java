package org.helioviewer.jhv.gui.dialog;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.JComboBox;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
public final class SynopticDialog extends CatalogDialog<SynopticDialog.SynopticMap> {

    private static final String server = "https://idoc-ssa-prod.ias.u-psud.fr";
    private static final String hmiQuery = "/api/s005a/map/hmi";
    private static final String aiaQuery = "/api/s050a/map/";

    private record Channel(String label, String query, String name) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final Channel[] Channels = {
            new Channel("HMI magnetogram", hmiQuery, "hmi.m_720s"),
            new Channel("HMI continuum", hmiQuery, "hmi.ic_720s"),
            aia("94"), aia("131"), aia("171"), aia("193"), aia("211"), aia("304"), aia("335"), aia("1600"), aia("1700")};

    private static Channel aia(String wavelength) {
        return new Channel("AIA " + wavelength, aiaQuery, wavelength);
    }

    record SynopticMap(URI uri) {
        @Override
        public String toString() {
            String path = uri.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

    private final JComboBox<Channel> channelCombo = new JComboBox<>(Channels);
    private final JComboBox<String> mapTypeCombo = new JComboBox<>(new String[]{"forecast", "nowcast", "temporary", "archived"});

    private static SynopticDialog instance;

    public static SynopticDialog getInstance() {
        return instance == null ? instance = new SynopticDialog() : instance;
    }

    private SynopticDialog() {
        super("New Synoptic Layer");
    }

    @Override
    protected void addQueryRows() {
        channelCombo.setSelectedIndex(0);
        channelCombo.addActionListener(e -> scheduleSearch());
        mapTypeCombo.addActionListener(e -> scheduleSearch());
        addRow("Channel", channelCombo);
        addRow("Map type", mapTypeCombo);
    }

    @Override
    protected boolean startSearch(Consumer<List<SynopticMap>> onResult, Consumer<Throwable> onFailure) {
        if (!(channelCombo.getSelectedItem() instanceof Channel channel && mapTypeCombo.getSelectedItem() instanceof String mapType))
            return false;
        JSONObject jo = new JSONObject();
        jo.put("start_date", TimeUtils.formatDate(timeSelectorPanel.getStartTime()));
        jo.put("end_date", TimeUtils.formatDate(timeSelectorPanel.getEndTime()));
        jo.put("channels", new JSONArray().put(channel.name));
        jo.put("map_type", new JSONArray().put(mapType));
        jo.put("latest", true);
        Task.submitBackground(server, new SearchSynoptic(channel.query, jo), onResult, (logContext, t) -> {
            Log.error(logContext, t);
            onFailure.accept(t);
        });
        return true;
    }

    @Override
    protected boolean load(List<SynopticMap> items) {
        Load.image(items.stream().map(SynopticMap::uri).toList());
        return true;
    }

    @Override
    public void showDialog() {
        scheduleSearch();
        super.showDialog();
    }

    private record SearchSynoptic(String queryPath, JSONObject jo) implements Callable<List<SynopticMap>> {
        @Override
        public List<SynopticMap> call() throws Exception {
            URI base = new URI(server);
            JSONArray result = JSONUtils.post(base.resolve(queryPath), jo);
            List<SynopticMap> maps = new ArrayList<>(result.length());
            for (int i = 0; i < result.length(); i++) {
                JSONObject item = result.optJSONObject(i);
                JSONObject resources = item == null ? null : item.optJSONObject("_resources");
                String fitsFile = resources == null ? null : resources.optString("fits_file", null);
                if (fitsFile != null)
                    maps.add(new SynopticMap(base.resolve(fitsFile)));
            }
            return maps;
        }
    }

}
