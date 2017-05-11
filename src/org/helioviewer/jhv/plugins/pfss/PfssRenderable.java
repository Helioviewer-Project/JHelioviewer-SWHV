package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.opengl.VBO;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;
import org.helioviewer.jhv.plugins.pfss.data.PfssNewDataLoader;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.threads.CancelTask;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class PfssRenderable extends AbstractRenderable implements TimespanListener {

    private final PfssPluginPanel optionsPanel = new PfssPluginPanel();
    private PfssData previousPfssData = null;
    private VBO vertexVBO;

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        PfssData pfssData = PfssPlugin.getPfsscache().getData(Layers.getLastUpdatedTimestamp().milli);
        if (pfssData != null) {
            renderData(gl, pfssData);
            previousPfssData = pfssData;
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "PFSS Model";
    }

    private String timeString = null;

    @Override
    public String getTimeString() {
        return timeString;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible)
            timeString = null;
    }

    @Override
    public void timespanChanged(long start, long end) {
        PfssPlugin.getPfsscache().clear();

        FutureTask<Void> dataLoaderTask = new FutureTask<>(new PfssNewDataLoader(start, end), null);
        PfssPlugin.pfssNewLoadPool.execute(dataLoaderTask);
        PfssPlugin.pfssReaperPool.schedule(new CancelTask(dataLoaderTask), 60 * 5, TimeUnit.SECONDS);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        vertexVBO = new VBO(GL2.GL_ARRAY_BUFFER, -1, 3);
        vertexVBO.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        vertexVBO.dispose(gl);
        previousPfssData = null;
    }

    private void renderData(GL2 gl, PfssData data) {
        if (previousPfssData == null || data != previousPfssData ||
            PfssSettings.qualityReduction != data.lastQuality || PfssSettings.fixedColor != data.lastFixedColor) {
            data.calculatePositions(PfssSettings.qualityReduction, PfssSettings.fixedColor);
            vertexVBO.bindBufferData(gl, data.vertices, Buffers.SIZEOF_FLOAT);

            timeString = data.getDateObs().toString();
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
        }
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);

        gl.glDepthMask(false);

        vertexVBO.bindArray(gl);
        gl.glColorPointer(4, GL2.GL_FLOAT, 7 * 4, 3 * 4);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 7 * 4, 0);

        gl.glLineWidth(PfssSettings.LINE_WIDTH);
        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, vertexVBO.bufferSize);
        vertexVBO.unbindArray(gl);

        gl.glDepthMask(true);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
    }

}
