package org.helioviewer.jhv.renderable.gui;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableMiniview;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
public class RenderableContainer extends AbstractTableModel implements Reorderable {

    private ArrayList<Renderable> renderables = new ArrayList<>();
    private ArrayList<Renderable> newRenderables = new ArrayList<>();
    private final ArrayList<Renderable> removedRenderables = new ArrayList<>();

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
            Layers.arrangeMultiView(true);
        }
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

    public void saveCurrentScene() {
        JSONArray ja = new JSONArray();
        for (Renderable renderable : renderables) {
            JSONObject jo = new JSONObject();
            JSONObject dataObject = new JSONObject();
            jo.put("data", dataObject);
            jo.put("className", renderable.getClass().getName());
            renderable.serialize(dataObject);
            ja.put(jo);
        }
        System.out.println(ja);
    }

    public void loadScene(JSONArray ja) {
        for (Renderable renderable : renderables) {
            removeRenderable(renderable);
        }
        for (Object o : ja) {
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                try {
                    Class<?> c = Class.forName(jo.getString("className"));
                    Constructor<?> cons = c.getConstructor(JSONObject.class);
                    Object object = cons.newInstance(jo);
                    if(object instanceof Renderable )
                        addRenderable((Renderable) object);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | JSONException
                        | ClassNotFoundException
                        | NoSuchMethodException
                        | SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
