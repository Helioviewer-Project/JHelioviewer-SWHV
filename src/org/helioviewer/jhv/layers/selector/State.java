package org.helioviewer.jhv.layers.selector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.plugin.PluginManager;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayersContainer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONArray;
import org.json.JSONObject;

public class State {

    public static void save() {
        String fileName = JHVDirectory.STATES.getPath() + "state__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".jhv";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
            toJson().write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject toJson() {
        JSONObject main = new JSONObject();
        main.put("time", Movie.getTime());
        main.put("play", Movie.isPlaying());
        main.put("multiview", LayersPanel.multiview.isSelected());
        main.put("projection", Displayer.mode);
        main.put("tracking", ImageViewerGui.getToolBar().getTrackingButton().isSelected());
        main.put("showCorona", ImageViewerGui.getToolBar().getShowCoronaButton().isSelected());

        JSONArray ja = new JSONArray();
        for (Layer layer : LayersContainer.getLayers()) {
            if (!(layer instanceof ImageLayer))
                ja.put(layer2json(layer, false));
        }
        main.put("layers", ja);

        JSONArray ji = new JSONArray();
        for (ImageLayer imageLayer : LayersContainer.getImageLayers()) {
            ji.put(layer2json(imageLayer, imageLayer == LayersContainer.getActiveImageLayer()));
        }
        main.put("imageLayers", ji);

        saveTimelineState(main);
        JSONObject plugins = new JSONObject();
        PluginManager.getSingletonInstance().saveState(plugins);
        main.put("plugins", plugins);

        return new JSONObject().put("org.helioviewer.jhv.state", main);
    }

    private static JSONObject layer2json(Layer layer, boolean master) {
        JSONObject jo = new JSONObject().put("className", layer.getClass().getName()).put("name", layer.getName());
        JSONObject dataObject = new JSONObject();
        layer.serialize(dataObject);
        jo.put("data", dataObject);
        jo.put("enabled", layer.isEnabled());
        if (master)
            jo.put("master", true);
        return jo;
    }

    private static void saveTimelineState(JSONObject main) {
        JSONArray ja = new JSONArray();
        for (TimelineLayer tl : TimelineLayers.get()) {
            JSONObject jo = new JSONObject().put("className", tl.getClass().getName()).put("name", tl.getName());
            JSONObject dataObject = new JSONObject();
            tl.serialize(dataObject);
            jo.put("data", dataObject);
            jo.put("enabled", tl.isEnabled());
            ja.put(jo);
        }
        main.put("timelines", ja);
    }

    private static Object json2Object(JSONObject json) {
        JSONObject jdata = json.optJSONObject("data");
        if (jdata == null)
            return null;

        try {
            Class<?> c = Class.forName(json.optString("className"));
            Constructor<?> cons = c.getConstructor(JSONObject.class);
            return cons.newInstance(jdata);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void loadTimelines(JSONObject data) {
        ArrayList<TimelineLayer> newlist = new ArrayList<>();

        JSONArray rja = data.getJSONArray("timelines");
        for (Object o : rja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                try {
                    Object obj = json2Object(jo);
                    if (obj instanceof TimelineLayer) {
                        TimelineLayer tl = (TimelineLayer) obj;
                        newlist.add(tl);
                        tl.setEnabled(jo.optBoolean("enabled", true));
                    }
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }
        Timelines.getModel().clear();
        for (TimelineLayer tl : newlist) {
            Timelines.getModel().addLayer(tl);
        }
    }

    private static void loadLayers(JSONObject data) {
        LayersContainer.removeAll();

        JSONArray rja = data.getJSONArray("layers");
        for (Object o : rja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                try {
                    Object obj = json2Object(jo);
                    if (obj instanceof Layer) {
                        Layer layer = (Layer) obj;
                        ImageViewerGui.getLayersContainer().addLayer(layer);
                        layer.setEnabled(jo.optBoolean("enabled", false));
                    }
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }

        ArrayList<ImageLayer> newlist = new ArrayList<>();
        ImageLayer masterLayer = null;
        rja = data.getJSONArray("imageLayers");
        for (Object o : rja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                JSONObject jd = jo.optJSONObject("data");
                if (jd == null)
                    continue;

                try {
                    ImageLayer layer = ImageLayer.create(jd);
                    newlist.add(layer);
                    layer.setEnabled(jo.optBoolean("enabled", false)); // pointless
                    if (jo.optBoolean("master", false))
                        masterLayer = layer;
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }

        LayersPanel.multiview.setSelected(data.optBoolean("multiview", LayersPanel.multiview.isSelected()));
        ImageViewerGui.getToolBar().getShowCoronaButton().setSelected(data.optBoolean("showCorona", ImageViewerGui.getToolBar().getShowCoronaButton().isSelected()));

        JHVDate time = new JHVDate(TimeUtils.optParse(data.optString("time"), Movie.getTime().milli));
        boolean tracking = data.optBoolean("tracking", ImageViewerGui.getToolBar().getTrackingButton().isSelected());
        boolean play = data.optBoolean("play", false);
        LoadState loadStateTask = new LoadState(newlist, masterLayer, time, tracking, play);
        JHVGlobals.getExecutorService().execute(loadStateTask);
    }

    public static void load(JSONObject obj) {
        try {
            JSONObject jo = obj.getJSONObject("org.helioviewer.jhv.state");
            // to be loaded before viewpoint
            try {
                Displayer.DisplayMode.valueOf(jo.optString("projection")).radio.doClick();
            } catch (Exception ignore) {
            }
            loadTimelines(jo);
            loadLayers(jo);
            JSONObject plugins = jo.optJSONObject("plugins");
            if (plugins != null)
                PluginManager.getSingletonInstance().loadState(plugins);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class LoadState extends JHVWorker<Void, Void> {
        private final ArrayList<ImageLayer> newlist;
        private final ImageLayer masterLayer;
        private final JHVDate time;
        private final boolean tracking;
        private final boolean play;

        LoadState(ArrayList<ImageLayer> _newlist, ImageLayer _masterLayer, JHVDate _time, boolean _tracking, boolean _play) {
            newlist = _newlist;
            masterLayer = _masterLayer;
            time = _time;
            tracking = _tracking;
            play = _play;
        }

        @Override
        protected Void backgroundWork() {
            for (ImageLayer layer : newlist) {
                while (!layer.isLoadedForState()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void done() {
            if (isCancelled())
                return;

            for (ImageLayer layer : newlist)
                layer.unload(); // prune failed layers
            if (masterLayer != null)
                LayersContainer.setActiveImageLayer(masterLayer);
            Movie.setTime(time);
            ImageViewerGui.getToolBar().getTrackingButton().setSelected(tracking);
            if (play)
                Movie.play();
        }
    }

}
