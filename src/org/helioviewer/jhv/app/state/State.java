package org.helioviewer.jhv.app.state;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.annotation.Annotations;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.movie.Movie;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.thread.JHVThread;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.Timelines;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public final class State {

    private State() {}

    public static void save(String dir, String file) {
        JSONObject json = toJson();

        JHVThread.create(() -> {
            Path path = Path.of(dir, file);
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                json.write(writer);
            } catch (IOException e) {
                Log.error(e);
            }
        }, "JHV-SaveState").start();
    }

    private static JSONObject toJson() {
        JSONObject main = new JSONObject();
        main.put("time", Movie.getTime());
        ViewState.writeModeJson(main);
        main.put("annotations", Annotations.toJson());

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
        JSONObject data = json.optJSONObject("data");
        if (data == null)
            return null;

        try {
            Class<?> c = Class.forName(json.optString("className"));
            Constructor<?> cons = c.getConstructor(JSONObject.class);
            return cons.newInstance(data);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    private static Object deserialize2Object(JSONObject json) {
        JSONObject data = json.optJSONObject("data");
        if (data == null)
            return null;

        try {
            Class<?> c = Class.forName(json.optString("className"));
            Method m = c.getDeclaredMethod("deserialize", JSONObject.class);
            return m.invoke(null, data);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static void loadTimelines(JSONObject data) {
        ArrayList<TimelineLayer> newList = new ArrayList<>();

        JSONArray timelines = data.optJSONArray("timelines");
        if (timelines != null) {
            for (Object o : timelines) {
                if (o instanceof JSONObject jo) {
                    try {
                        if (deserialize2Object(jo) instanceof TimelineLayer layer) {
                            newList.add(layer);
                            layer.setEnabled(jo.optBoolean("enabled", true));
                        }
                    } catch (Exception e) { // don't stop for a broken one
                        Log.error(e);
                    }
                }
            }
        }
        Timelines.getLayers().restore(newList);
    }

    private static void loadLayers(JSONObject data, @Nullable Commands.OperationContext context, ViewState.ModeData modeData) {
        ArrayList<Layer> restoredLayers = new ArrayList<>();

        JSONArray layers = data.optJSONArray("layers");
        if (layers != null) {
            for (Object o : layers) {
                if (o instanceof JSONObject jo) {
                    try {
                        if (json2Object(jo) instanceof Layer layer) {
                            restoredLayers.add(layer);
                            layer.setEnabled(jo.optBoolean("enabled", false));
                        }
                    } catch (Exception e) { // don't stop for a broken one
                        Log.error("layers", e);
                    }
                }
            }
        }

        HashMap<ImageLayer, Boolean> newLayers = new HashMap<>();
        ImageLayer masterLayer = null;

        JSONArray imageLayers = data.optJSONArray("imageLayers");
        if (imageLayers != null) {
            for (Object o : imageLayers) {
                if (o instanceof JSONObject jo) {
                    JSONObject jd = jo.optJSONObject("data");
                    if (jd == null)
                        continue;

                    try {
                        ImageLayer layer = ImageLayer.createDetached(jd);
                        restoredLayers.add(layer);
                        newLayers.put(layer, jo.optBoolean("enabled", false));
                        if (jo.optBoolean("master", false))
                            masterLayer = layer;
                    } catch (Exception e) { // don't stop for a broken one
                        Log.error("imageLayers", e);
                    }
                }
            }
        }

        Layers.restore(restoredLayers);
        Annotations.fromJson(data.optJSONObject("annotations"));

        JHVTime time = new JHVTime(TimeUtils.optParse(data.optString("time"), Movie.getTime().milli));
        Task.submit(
                new ImageLayers.WaitUntilLoaded(newLayers.keySet()),
                new Callback(context, newLayers, masterLayer, time, modeData));
    }

    private record Callback(@Nullable Commands.OperationContext context, Map<ImageLayer, Boolean> newLayers,
                            @Nullable ImageLayer masterLayer, JHVTime time,
                            ViewState.ModeData modeData) implements FutureCallback<Void> {

        private void applyRestoredPlaybackState() {
            ViewState.applyMode(modeData); // this applies projection again
            Commands.seekTime(time);
            DisplayController.refreshCamera();
        }

        @Override
        public void onSuccess(Void result) {
            newLayers.keySet().forEach(ImageLayer::unload); // prune failed layers
            for (ImageLayer layer : Layers.getImageLayers()) {
                Boolean enabled = newLayers.get(layer);
                if (enabled != null) // user may have loaded a new layer in the meanwhile
                    layer.setEnabled(enabled);
            }
            if (masterLayer != null && Layers.getImageLayers().contains(masterLayer))
                Layers.setActiveImageLayer(masterLayer);
            applyRestoredPlaybackState();
            Commands.notifyLoadStateFinished(context, true, "State loaded.");
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            String message = t.getMessage() == null || t.getMessage().isBlank() ? "State load failed." : t.getMessage();
            Commands.notifyLoadStateFinished(context, false, message);
        }

    }

    public static void load(@Nullable Commands.OperationContext context, JSONObject jo) {
        try {
            ViewState.ModeData modeData = ViewState.readModeJson(jo);
            ViewState.setProjection(modeData.projection()); // to be set before viewpoint

            if (PluginManager.isActive(EVEPlugin.class))
                loadTimelines(jo);
            else
                Log.info("Skipping timeline state because EVEPlugin is inactive");

            JSONObject plugins = jo.optJSONObject("plugins");
            if (plugins != null)
                PluginManager.loadState(plugins);
            loadLayers(jo, context, modeData);
        } catch (Exception e) {
            Log.error(e);
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "State load failed." : e.getMessage();
            Commands.notifyLoadStateFinished(context, false, message);
        }
    }

}
