package org.helioviewer.jhv.plugin.renderable;

import java.util.ArrayList;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.renderable.RenderableGrid;
import org.helioviewer.jhv.renderable.RenderableSolarAxes;
import org.helioviewer.jhv.renderable.RenderableSolarAxesType;

public class RenderableContainer {
    private final ArrayList<Renderable> renderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> newRenderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> removedRenderables = new ArrayList<Renderable>();

    public RenderableContainer() {
        RenderableSolarAxesType solarAxesType = new RenderableSolarAxesType("Solar Axes");
        this.addRenderable(new RenderableSolarAxes(solarAxesType));
        RenderableSolarAxesType gridType = new RenderableSolarAxesType("Grids");
        this.addRenderable(new RenderableGrid(gridType, 20, 20, false));
    }

    public void addRenderable(Renderable renderable) {
        newRenderables.add(renderable);
    }

    public void removeRenderable(Renderable renderable) {
        renderables.remove(renderable);
        removedRenderables.add(renderable);
    }

    public void render(GL3DState state) {
        initRenderables(state);
        removeRenderables(state);
        for (Renderable renderable : renderables) {
            renderable.render(state);
        }
    }

    private void initRenderables(GL3DState state) {
        for (Renderable renderable : newRenderables) {
            renderable.init(state);
            renderables.add(renderable);
        }
        newRenderables.clear();
    }

    private void removeRenderables(GL3DState state) {
        for (Renderable renderable : removedRenderables) {
            renderable.remove(state);
        }
    }
}
