package org.helioviewer.jhv.layers.selector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public class State {

    public static void save(String dir, String file) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(dir, file), StandardCharsets.UTF_8)) {
            toJson().write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject toJson() {
        JSONObject main = new JSONObject();
        main.put("time", Movie.getTime());
        main.put("play", Movie.isPlaying());
        main.put("multiview", JHVFrame.getToolBar().getMultiviewButton().isSelected());
        main.put("projection", Display.mode);
        main.put("tracking", JHVFrame.getToolBar().getTrackingButton().isSelected());
        main.put("showCorona", JHVFrame.getToolBar().getShowCoronaButton().isSelected());
        main.put("differentialRotation", JHVFrame.getToolBar().getDiffRotationButton().isSelected());
        main.put("annotations", JHVFrame.getInteraction().saveAnnotations());

        JSONArray ja = new JSONArray();
        for (Layer layer : Layers.getLayers()) {
            if (!(layer instanceof ImageLayer))
                ja.put(layer2json(layer, false));
        }
        main.put("layers", ja);

        JSONArray ji = new JSONArray();
        ImageLayer active = Layers.getActiveImageLayer();
        Layers.forEachImageLayer(layer -> ji.put(layer2json(layer, layer == active)));
        main.put("imageLayers", ji);

        saveTimelineState(main);
        JSONObject plugins = new JSONObject();
        PluginManager.saveState(plugins);
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

    @Nullable
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

        for (Object o : data.getJSONArray("timelines")) {
            if (o instanceof JSONObject jo) {
                try {
                    if (json2Object(jo) instanceof TimelineLayer layer) {
                        newlist.add(layer);
                        layer.setEnabled(jo.optBoolean("enabled", true));
                    }
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }
        Timelines.getLayers().clear();
        newlist.forEach(layer -> Timelines.getLayers().add(layer));
    }

    private static void loadLayers(JSONObject data) {
        Layers.clear();

        for (Object o : data.getJSONArray("layers")) {
            if (o instanceof JSONObject jo) {
                try {
                    if (json2Object(jo) instanceof Layer layer) {
                        JHVFrame.getLayers().add(layer);
                        layer.setEnabled(jo.optBoolean("enabled", false));
                    }
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }

        HashMap<ImageLayer, Boolean> newLayers = new HashMap<>();
        ImageLayer masterLayer = null;
        for (Object o : data.getJSONArray("imageLayers")) {
            if (o instanceof JSONObject jo) {
                JSONObject jd = jo.optJSONObject("data");
                if (jd == null)
                    continue;

                try {
                    ImageLayer layer = ImageLayer.create(jd);
                    newLayers.put(layer, jo.optBoolean("enabled", false));
                    if (jo.optBoolean("master", false))
                        masterLayer = layer;
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }

        JHVFrame.getInteraction().loadAnnotations(data.optJSONObject("annotations"));
        JHVFrame.getToolBar().getMultiviewButton().setSelected(data.optBoolean("multiview", JHVFrame.getToolBar().getMultiviewButton().isSelected()));
        JHVFrame.getToolBar().getShowCoronaButton().setSelected(data.optBoolean("showCorona", JHVFrame.getToolBar().getShowCoronaButton().isSelected()));
        JHVFrame.getToolBar().getDiffRotationButton().setSelected(data.optBoolean("differentialRotation", JHVFrame.getToolBar().getDiffRotationButton().isSelected()));

        JHVTime time = new JHVTime(TimeUtils.optParse(data.optString("time"), Movie.getTime().milli));
        boolean tracking = data.optBoolean("tracking", JHVFrame.getToolBar().getTrackingButton().isSelected());
        boolean play = data.optBoolean("play", false);

        EventQueueCallbackExecutor.pool.submit(new WaitLoad(newLayers.keySet()), new Callback(newLayers, masterLayer, time, tracking, play));
    }

    private static class WaitLoad implements Callable<Void> {

        private final Set<ImageLayer> newLayers;

        WaitLoad(Set<ImageLayer> _newLayers) {
            newLayers = _newLayers;
        }

        @Override
        public Void call() throws Exception {
            for (ImageLayer layer : newLayers) {
                while (!layer.isLoadedForState()) {
                    Thread.sleep(1000);
                }
            }
            return null;
        }

    }

    private static class Callback implements FutureCallback<Void> {

        private final Map<ImageLayer, Boolean> newLayers;
        private final ImageLayer masterLayer;
        private final JHVTime time;
        private final boolean tracking;
        private final boolean play;

        Callback(Map<ImageLayer, Boolean> _newLayers, ImageLayer _masterLayer, JHVTime _time, boolean _tracking, boolean _play) {
            newLayers = _newLayers;
            masterLayer = _masterLayer;
            time = _time;
            tracking = _tracking;
            play = _play;
        }

        @Override
        public void onSuccess(Void result) {
            newLayers.keySet().forEach(ImageLayer::unload); // prune failed layers
            for (ImageLayer layer : Layers.getImageLayers()) {
                Boolean enabled = newLayers.get(layer);
                if (enabled != null) // user may have loaded a new layer in the meanwhile
                    layer.setEnabled(enabled);
            }
            if (masterLayer != null)
                Layers.setActiveImageLayer(masterLayer);
            Movie.setTime(time);
            JHVFrame.getToolBar().getTrackingButton().setSelected(tracking);
            if (play)
                Movie.play();
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("StateLoad", t);
        }

    }

    public static void load(JSONObject jo) {
        try {
            // to be loaded before viewpoint
            try {
                Display.ProjectionMode.valueOf(jo.optString("projection")).radio.doClick();
            } catch (Exception ignore) {
            }
            loadTimelines(jo);
            loadLayers(jo);
            JSONObject plugins = jo.optJSONObject("plugins");
            if (plugins != null)
                PluginManager.loadState(plugins);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
