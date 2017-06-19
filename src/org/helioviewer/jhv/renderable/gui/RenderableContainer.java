package org.helioviewer.jhv.renderable.gui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.plugin.PluginManager;
import org.helioviewer.jhv.camera.Camera;
// import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableGrid;
import org.helioviewer.jhv.renderable.components.RenderableMiniview;
import org.helioviewer.jhv.renderable.components.RenderableTimeStamp;
import org.helioviewer.jhv.renderable.components.RenderableViewpoint;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
public class RenderableContainer extends AbstractTableModel implements Reorderable {

    private ArrayList<Renderable> renderables = new ArrayList<>();
    private ArrayList<Renderable> newRenderables = new ArrayList<>();
    private final ArrayList<Renderable> removedRenderables = new ArrayList<>();

    private RenderableGrid renderableGrid;
    private RenderableViewpoint renderableViewpoint;
    private RenderableMiniview renderableMiniview;

    public RenderableContainer() {
        addRenderable(new RenderableGrid(null));
        addRenderable(new RenderableViewpoint(null));
        addRenderable(new RenderableTimeStamp(null));
        addRenderable(new RenderableMiniview(null));
    }

    public RenderableViewpoint getRenderableViewpoint() {
        return renderableViewpoint;
    }

    public RenderableGrid getRenderableGrid() {
        return renderableGrid;
    }

    public RenderableMiniview getRenderableMiniview() {
        return renderableMiniview;
    }

    public void addBeforeRenderable(Renderable renderable) {
        int lastImagelayerIndex = -1;
        int size = renderables.size();
        for (int i = 0; i < size; i++) {
            if (renderables.get(i) instanceof ImageLayer) {
                lastImagelayerIndex = i;
            }
        }
        renderables.add(lastImagelayerIndex + 1, renderable);
        newRenderables.add(renderable);

        int row = lastImagelayerIndex + 1;
        fireTableRowsInserted(row, row);
    }

    public void addRenderable(Renderable renderable) {
        renderables.add(renderable);
        newRenderables.add(renderable);

        if (renderable instanceof RenderableGrid)
            renderableGrid = (RenderableGrid) renderable;
        else if (renderable instanceof RenderableViewpoint)
            renderableViewpoint = (RenderableViewpoint) renderable;
        else if (renderable instanceof RenderableMiniview)
            renderableMiniview = (RenderableMiniview) renderable;

        int row = renderables.size() - 1;
        fireTableRowsInserted(row, row);
        Displayer.display(); // e.g., PFSS renderable
    }

    public void removeRenderable(Renderable renderable) {
        renderables.remove(renderable);
        removedRenderables.add(renderable);
        // refreshTable(); display() will take care
        Displayer.display();
    }

    public void prerender(GL2 gl) {
        int count = removeRenderables(gl);
        initRenderables(gl);
        if (count > 0)
            refreshTable();

        for (Renderable renderable : renderables) {
            renderable.prerender(gl);
        }
    }

    public void render(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.render(camera, vp, gl);
        }
    }

    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderScale(camera, vp, gl);
        }
    }

    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFloat(camera, vp, gl);
        }
    }

    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFullFloat(camera, vp, gl);
        }
    }

    public void renderMiniview(Camera camera, Viewport miniview, GL2 gl) {
        RenderableMiniview.renderBackground(camera, miniview, gl);
        for (Renderable renderable : renderables) {
            renderable.renderMiniview(camera, miniview, gl);
        }
    }

    private void initRenderables(GL2 gl) {
        for (Renderable renderable : newRenderables) {
            renderable.init(gl);
        }
        newRenderables.clear();
    }

    private int removeRenderables(GL2 gl) {
        int count = removedRenderables.size();
        for (Renderable renderable : removedRenderables) {
            renderable.remove(gl);
        }
        removedRenderables.clear();
        return count;
    }

    private void insertRow(int row, Renderable rowData) {
        if (row > renderables.size()) {
            renderables.add(rowData);
        } else {
            renderables.add(row, rowData);
        }
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {
        if (toIndex > renderables.size()) {
            return;
        }
        Renderable toMove = renderables.get(fromIndex);
        Renderable moveTo = renderables.get(Math.max(0, toIndex - 1));

        if (!(toMove instanceof ImageLayer) || !(moveTo instanceof ImageLayer)) {
            return;
        }
        renderables.remove(fromIndex);
        if (fromIndex < toIndex) {
            insertRow(toIndex - 1, toMove);
        } else {
            insertRow(toIndex, toMove);
        }
        refreshTable();

        if (Displayer.multiview) {
            arrangeMultiView(true);
        }
    }

    public void arrangeMultiView(boolean multiview) {
        int ct = 0;

        if (multiview) {
            for (Renderable r : renderables) {
                if (r instanceof ImageLayer) {
                    ImageLayer l = (ImageLayer) r;
                    if (l.isEnabled()) {
                        l.setVisible(ct);
                        l.setOpacity(1);
                        ct++;
                    }
                }
            }
        } else {
            for (Renderable r : renderables) {
                if (r instanceof ImageLayer) {
                    ImageLayer l = (ImageLayer) r;
                    if (l.isEnabled()) {
                        l.setVisible(0);
                        float opacity;
                        if (ImageLayer.isCor(l.getName()))
                            opacity = 1;
                        else {
                            opacity = (float) (1. / (1. + ct));
                            ct++;
                        }
                        l.setOpacity(opacity);
                    }
                }
            }
        }
        Displayer.reshapeAll();
        Displayer.render(1);
    }

    @Override
    public int getRowCount() {
        return renderables.size();
    }

    @Override
    public int getColumnCount() {
        return RenderableContainerPanel.NUMBER_COLUMNS;
    }

    @Override
    public Object getValueAt(int row, int col) {
        try {
            return renderables.get(row);
        } catch (Exception e) {
            return null;
        }
    }

    public void refreshTable() {
        fireTableDataChanged();
    }

    public void updateCell(int row, int col) {
        if (row >= 0) // negative row breaks model
            fireTableCellUpdated(row, col);
    }

    public void fireTimeUpdated(Renderable renderable) {
        updateCell(renderables.indexOf(renderable), RenderableContainerPanel.TIME_COL);
    }

    public void dispose(GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.dispose(gl);
        }
        newRenderables = renderables;
        renderables = new ArrayList<>();
    }

    private final String defaultState = JHVDirectory.HOME.getPath() + "test.json";

    public void saveCurrentScene() {
        JSONObject main = new JSONObject();
        main.put("time", Layers.getLastUpdatedTimestamp());
        main.put("play", Layers.isMoviePlaying());
        main.put("multiview", RenderableContainerPanel.multiview.isSelected());

        JSONArray ja = new JSONArray();
        for (Renderable renderable : renderables) {
            if (!(renderable instanceof ImageLayer))
                ja.put(renderable2json(renderable, false));
        }
        main.put("renderables", ja);

        JSONArray ji = new JSONArray();
        for (Renderable renderable : renderables) {
            if (renderable instanceof ImageLayer)
                ji.put(renderable2json(renderable, ((ImageLayer) renderable).isActiveImageLayer()));
        }
        main.put("imageLayers", ji);

        saveTimelineScene(main);
        JSONObject plugins = new JSONObject();
        PluginManager.getSingletonInstance().saveState(plugins);
        main.put("plugins", plugins);
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(defaultState), StandardCharsets.UTF_8)) {
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
        JSONArray va = new JSONArray();
        renderable.serializeVisibility(va);
        jo.put("visibility", va);
        jo.put("enabled", renderable.isEnabled());
        if (master)
            jo.put("master", true);
        return jo;
    }

    private static void saveTimelineScene(JSONObject main) {
        JSONArray ja = new JSONArray();
        List<TimelineRenderable> lds = Timelines.getModel().getAllLineDataSelectorElements();
        for (TimelineRenderable renderable : lds) {
            JSONObject jo = new JSONObject().put("className", renderable.getClass().getName()).put("name", renderable.getName());
            JSONObject dataObject = new JSONObject();
            renderable.serialize(dataObject);
            jo.put("data", dataObject);
            jo.put("visibility", renderable.isVisible());
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
                        renderable.setVisible(jo.optBoolean("visibility", true));
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

    private void loadRenderables(JSONObject data) {
        ArrayList<Renderable> newlist = new ArrayList<>();
        Renderable masterRenderable = null;

        removedRenderables.addAll(renderables);
        renderables = new ArrayList<>();

        JSONArray rja = data.getJSONArray("renderables");
        for (Object o : rja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                try {
                    Object obj = json2Object(jo);
                    if (obj instanceof Renderable) {
                        Renderable renderable = (Renderable) obj;
                        addRenderable(renderable);
                        JSONArray va = jo.optJSONArray("visibility");
                        if (va == null)
                            va = new JSONArray(new boolean[] { false, false, false, false });
                        renderable.deserializeVisibility(va);
                        renderable.setEnabled(jo.optBoolean("enabled", false));
                    }
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }

        rja = data.getJSONArray("imageLayers");
        for (Object o : rja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                try {
                    Object obj = json2Object(jo);
                    if (obj instanceof Renderable) {
                        Renderable renderable = (Renderable) obj;
                        newlist.add(renderable);
                        JSONArray va = jo.optJSONArray("visibility");
                        if (va == null)
                            va = new JSONArray(new boolean[] { false, false, false, false });
                        renderable.deserializeVisibility(va);
                        renderable.setEnabled(jo.optBoolean("enabled", false));
                        if (jo.optBoolean("master", false))
                            masterRenderable = renderable;
                    }
                } catch (Exception e) { // don't stop for a broken one
                    e.printStackTrace();
                }
            }
        }

        LoadState loadStateTask = new LoadState(newlist, masterRenderable, new JHVDate(TimeUtils.optParse(data.optString("time"), "now")), data.optBoolean("play", false));
        JHVGlobals.getExecutorService().execute(loadStateTask);
    }

    public void loadScene(String stateFile) {
        try {
            JSONObject data = JSONUtils.getJSONFile(stateFile);
            RenderableContainerPanel.multiview.setSelected(data.optBoolean("multiview", false));
            loadTimelines(data);
            loadRenderables(data);
            JSONObject plugins = data.optJSONObject("plugins");
            if (plugins != null)
                PluginManager.getSingletonInstance().loadState(plugins);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadScene() {
        loadScene(defaultState);
    }

    private class LoadState extends JHVWorker<Void, Void> {
        private final ArrayList<Renderable> newlist;
        private final Renderable master;
        private final JHVDate time;
        private final boolean play;

        public LoadState(ArrayList<Renderable> _newlist, Renderable _master, JHVDate _time, boolean _play) {
            newlist = _newlist;
            master = _master;
            time = _time;
            play = _play;
        }

        @Override
        protected Void backgroundWork() {
            for (Renderable renderable : newlist) {
                while (!renderable.isLoadedForState()) {
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
            if (!isCancelled()) {
                for (Renderable renderable : newlist) {
                    addBeforeRenderable(renderable);
                    if (renderable instanceof ImageLayer) {
                        ImageLayer layer = (ImageLayer) renderable;
                        layer.unload(); // prune failed layers
                        if (layer == master)
                            layer.setActiveImageLayer();
                    }
                }
                if (Displayer.multiview) {
                    arrangeMultiView(true);
                }
                Layers.setTime(time);
                if (play)
                    Layers.playMovie();
                // CameraHelper.zoomToFit(Displayer.getMiniCamera()); // funky
            }
        }
    }

}
