package org.helioviewer.jhv.renderable.gui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.plugin.PluginManager;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;
import org.json.JSONArray;
import org.json.JSONObject;

public class State {

    public static void save() {
        JSONObject main = new JSONObject();
        main.put("time", Layers.getLastUpdatedTimestamp());
        main.put("play", Layers.isMoviePlaying());
        main.put("multiview", RenderableContainerPanel.multiview.isSelected());
        main.put("projection", Displayer.mode);
        main.put("tracking", ImageViewerGui.getToolBar().getTrackingButton().isSelected());
        main.put("showCorona", ImageViewerGui.getToolBar().getShowCoronaButton().isSelected());

        JSONArray ja = new JSONArray();
        for (Renderable renderable : RenderableContainer.getRenderables()) {
            if (!(renderable instanceof ImageLayer))
                ja.put(renderable2json(renderable, false));
        }
        main.put("renderables", ja);

        JSONArray ji = new JSONArray();
        for (Renderable renderable : RenderableContainer.getRenderables()) {
            if (renderable instanceof ImageLayer)
                ji.put(renderable2json(renderable, ((ImageLayer) renderable).isActiveImageLayer()));
        }
        main.put("imageLayers", ji);

        saveTimelineState(main);
        JSONObject plugins = new JSONObject();
        PluginManager.getSingletonInstance().saveState(plugins);
        main.put("plugins", plugins);

        String fileName = JHVDirectory.STATES.getPath() + "jhv_state__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".json";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
            main.write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject renderable2json(Renderable renderable, boolean master) {
        JSONObject jo = new JSONObject().put("className", renderable.getClass().getName()).put("name", renderable.getName());
        JSONObject dataObject = new JSONObject();
        renderable.serialize(dataObject);
        jo.put("data", dataObject);
        jo.put("enabled", renderable.isEnabled());
        if (master)
            jo.put("master", true);
        return jo;
    }

    private static void saveTimelineState(JSONObject main) {
        JSONArray ja = new JSONArray();
        List<TimelineRenderable> lds = Timelines.getModel().getAllLineDataSelectorElements();
        for (TimelineRenderable renderable : lds) {
            JSONObject jo = new JSONObject().put("className", renderable.getClass().getName()).put("name", renderable.getName());
            JSONObject dataObject = new JSONObject();
            renderable.serialize(dataObject);
            jo.put("data", dataObject);
            jo.put("enabled", renderable.isEnabled());
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
        ArrayList<TimelineRenderable> newlist = new ArrayList<>();

        JSONArray rja = data.getJSONArray("timelines");
        for (Object o : rja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                try {
                    Object obj = json2Object(jo);
                    if (obj instanceof TimelineRenderable) {
                        TimelineRenderable renderable = (TimelineRenderable) obj;
                        newlist.add(renderable);
                        renderable.setEnabled(jo.optBoolean("enabled", true));
                    }
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }
        Timelines.getModel().clear();
        for (TimelineRenderable tr : newlist) {
            Timelines.getModel().addLineData(tr);
        }
    }

    private static void loadRenderables(JSONObject data) {
        RenderableContainer.removeAll();

        JSONArray rja = data.getJSONArray("renderables");
        for (Object o : rja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                try {
                    Object obj = json2Object(jo);
                    if (obj instanceof Renderable) {
                        Renderable renderable = (Renderable) obj;
                        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
                        renderable.setEnabled(jo.optBoolean("enabled", false));
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

        if (masterLayer != null)
            masterLayer.setActiveImageLayer();
        RenderableContainerPanel.multiview.setSelected(data.optBoolean("multiview", RenderableContainerPanel.multiview.isSelected()));
        ImageViewerGui.getToolBar().getShowCoronaButton().setSelected(data.optBoolean("showCorona", ImageViewerGui.getToolBar().getShowCoronaButton().isSelected()));

        JHVDate time = new JHVDate(TimeUtils.optParse(data.optString("time"), Layers.getLastUpdatedTimestamp().milli));
        boolean tracking = data.optBoolean("tracking", ImageViewerGui.getToolBar().getTrackingButton().isSelected());
        boolean play = data.optBoolean("play", false);
        LoadState loadStateTask = new LoadState(newlist, time, tracking, play);
        JHVGlobals.getExecutorService().execute(loadStateTask);
    }

    public static void load(String stateFile) {
        try {
            JSONObject data = JSONUtils.getJSONFile(stateFile);
            // to be loaded before viewpoint
            try {
                Displayer.DisplayMode.valueOf(data.optString("projection")).radio.doClick();
            } catch (Exception ignore) {
            }
            loadTimelines(data);
            loadRenderables(data);
            JSONObject plugins = data.optJSONObject("plugins");
            if (plugins != null)
                PluginManager.getSingletonInstance().loadState(plugins);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class LoadState extends JHVWorker<Void, Void> {
        private final ArrayList<ImageLayer> newlist;
        private final JHVDate time;
        private final boolean tracking;
        private final boolean play;

        public LoadState(ArrayList<ImageLayer> _newlist, JHVDate _time, boolean _tracking, boolean _play) {
            newlist = _newlist;
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
            Layers.setTime(time);
            ImageViewerGui.getToolBar().getTrackingButton().setSelected(tracking);
            if (play)
                Layers.playMovie();
        }
    }

}
