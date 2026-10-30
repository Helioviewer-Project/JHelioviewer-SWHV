package org.helioviewer.jhv.app.state;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.helioviewer.jhv.annotation.Annotations;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.layers.ConnectionLayer;
import org.helioviewer.jhv.layers.FOVLayer;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.layers.ModelLayer;
import org.helioviewer.jhv.layers.TimestampLayer;
import org.helioviewer.jhv.layers.ViewpointLayer;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssLayer;
import org.helioviewer.jhv.plugins.swek.EventTimelineLayer;
import org.helioviewer.jhv.plugins.swek.SWEKLayer;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.radio.RadioData;

import org.json.JSONArray;
import org.json.JSONObject;

public final class State {

    private static int loadSerial;

    public static void save(String dir, String file) {
        JSONObject json = toJson();

        AppThread.create(() -> {
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
        main.put("time", Player.getTime());
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
        String type = switch (layer) {
            case ImageLayer _ -> "image";
            case ViewpointLayer _ -> "viewpoint";
            case ConnectionLayer _ -> "connection";
            case GridLayer _ -> "grid";
            case FOVLayer _ -> "fov";
            case TimestampLayer _ -> "timestamp";
            case MiniviewLayer _ -> "miniview";
            case ModelLayer _ -> "model";
            case SWEKLayer _ -> "events";
            case PfssLayer _ -> "pfss";
            default -> throw new IllegalArgumentException("Unsupported saved layer: " + layer.getClass().getName());
        };
        JSONObject jo = new JSONObject().put("type", type).put("name", layer.getName());
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
            String type = switch (tl) {
                case Band _ -> "band";
                case RadioData _ -> "radio";
                case EventTimelineLayer _ -> "events";
                default -> throw new IllegalArgumentException("Unsupported saved timeline: " + tl.getClass().getName());
            };
            JSONObject jo = new JSONObject().put("type", type).put("name", tl.getName());
            JSONObject dataObject = new JSONObject();
            tl.serialize(dataObject);
            jo.put("data", dataObject);
            jo.put("enabled", tl.isEnabled());
            ja.put(jo);
        }
        main.put("timelines", ja);
    }

    @Nullable
    private static Layer readLayer(JSONObject json) {
        JSONObject data = json.optJSONObject("data");
        if (data == null)
            return null;

        try {
            String type = json.optString("type", json.optString("className"));
            return switch (type) {
                case "viewpoint", "org.helioviewer.jhv.layers.ViewpointLayer" -> new ViewpointLayer(data);
                case "connection", "org.helioviewer.jhv.layers.ConnectionLayer" -> new ConnectionLayer(data);
                case "grid", "org.helioviewer.jhv.layers.GridLayer" -> new GridLayer(data);
                case "fov", "org.helioviewer.jhv.layers.FOVLayer" -> new FOVLayer(data);
                case "timestamp", "org.helioviewer.jhv.layers.TimestampLayer" -> new TimestampLayer(data);
                case "miniview", "org.helioviewer.jhv.layers.MiniviewLayer" -> new MiniviewLayer(data);
                case "model", "org.helioviewer.jhv.layers.ModelLayer" -> new ModelLayer(data);
                case "events", "org.helioviewer.jhv.plugins.swek.SWEKLayer" -> new SWEKLayer(data);
                case "pfss", "org.helioviewer.jhv.plugins.pfss.PfssLayer" -> new PfssLayer(data);
                default -> {
                    Log.warn("Unknown saved layer type: " + type);
                    yield null;
                }
            };
        } catch (Exception | Error e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    private static TimelineLayer readTimeline(JSONObject json) {
        JSONObject data = json.optJSONObject("data");
        if (data == null)
            return null;

        try {
            String type = json.optString("type", json.optString("className"));
            return switch (type) {
                case "band", "org.helioviewer.jhv.timelines.band.Band" -> Band.deserialize(data);
                case "radio", "org.helioviewer.jhv.timelines.radio.RadioData" -> RadioData.deserialize(data);
                case "events", "org.helioviewer.jhv.plugins.swek.EventTimelineLayer" -> EventTimelineLayer.deserialize(data);
                default -> {
                    Log.warn("Unknown saved timeline type: " + type);
                    yield null;
                }
            };
        } catch (Exception | Error e) {
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
                        TimelineLayer layer = readTimeline(jo);
                        if (layer != null) {
                            newList.add(layer);
                            layer.setEnabled(jo.optBoolean("enabled", true));
                        }
                    } catch (Exception e) { // don't stop for a broken one
                        Log.error(e);
                    }
                }
            }
        }
        TimelineLayers.restore(newList);
    }

    private static void loadLayers(JSONObject data, Commands.Completion completion, ViewState.ModeData modeData, int serial) {
        ArrayList<Layer> restoredLayers = new ArrayList<>();

        JSONArray layers = data.optJSONArray("layers");
        if (layers != null) {
            for (Object o : layers) {
                if (o instanceof JSONObject jo) {
                    try {
                        Layer layer = readLayer(jo);
                        if (layer != null) {
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
        long now = System.currentTimeMillis();

        JSONArray imageLayers = data.optJSONArray("imageLayers");
        if (imageLayers != null) {
            for (Object o : imageLayers) {
                if (o instanceof JSONObject jo) {
                    JSONObject jd = jo.optJSONObject("data");
                    if (jd == null || jd.optJSONObject("APIRequest") == null)
                        continue;

                    try {
                        APIRequest request = APIRequest.fromJson(jd.getJSONObject("APIRequest"));
                        if (modeData.refresh())
                            request = request.endingAt(now);
                        ImageLayer layer = ImageLayer.createDetached(jd, request);
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

        JHVTime time = new JHVTime(TimeUtils.optParse(data.optString("time"), Player.getTime().milli));
        Callback callback = new Callback(completion, newLayers, masterLayer, time, modeData, serial);
        CompletableFuture.allOf(newLayers.keySet().stream().map(ImageLayer::whenLoadFinished).toArray(CompletableFuture[]::new))
                .thenRun(() -> UiThread.post(callback::onSuccess));
    }

    private record Callback(Commands.Completion completion, Map<ImageLayer, Boolean> newLayers,
                            @Nullable ImageLayer masterLayer, JHVTime time, ViewState.ModeData modeData, int serial) {

        private void applyRestoredPlaybackState() {
            ViewState.applyMode(modeData); // this applies projection again
            Commands.seekTime(time);
            DisplayController.refreshCamera();
        }

        void onSuccess() {
            if (serial != loadSerial) {
                completion.finished(false, "Superseded by a newer state load.", null);
                return;
            }
            for (ImageLayer layer : Layers.getImageLayers()) {
                Boolean enabled = newLayers.get(layer);
                if (enabled != null) // user may have loaded a new layer in the meanwhile
                    layer.setEnabled(enabled);
            }
            if (masterLayer != null && Layers.getImageLayers().contains(masterLayer))
                Layers.setActiveImageLayer(masterLayer);
            applyRestoredPlaybackState();
            completion.finished(true, "State loaded.", null);
        }

    }

    public static void load(Commands.Completion completion, JSONObject jo) {
        int serial = ++loadSerial;
        try {
            ViewState.ModeData modeData = ViewState.readModeJson(jo);
            ViewState.setRefresh(false); // no periodic reload may replace a layer while restoration waits for it
            ViewState.setProjection(modeData.projection()); // to be set before viewpoint

            if (PluginManager.isActive(EVEPlugin.class))
                loadTimelines(jo);
            else
                Log.info("Skipping timeline state because EVEPlugin is inactive");

            JSONObject plugins = jo.optJSONObject("plugins");
            if (plugins != null)
                PluginManager.loadState(plugins);
            loadLayers(jo, completion, modeData, serial);
        } catch (Exception e) {
            Log.error(e);
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "State load failed." : e.getMessage();
            completion.finished(false, message, null);
        }
    }

    private State() {}
}
