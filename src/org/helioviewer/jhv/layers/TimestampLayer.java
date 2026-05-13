package org.helioviewer.jhv.layers;

import java.awt.Component;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

import org.json.JSONObject;

public class TimestampLayer extends AbstractLayer {

    public static final int MIN_SCALE = 50;
    public static final int MAX_SCALE = 300;

    private int scale = 100;
    private boolean extra = false;
    private boolean top = false;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
        jo.put("extra", extra);
        jo.put("top", top);
    }

    private void deserialize(JSONObject jo) {
        scale = Math.clamp(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        extra = jo.optBoolean("extra", extra);
        top = jo.optBoolean("top", top);
    }

    public TimestampLayer(JSONObject jo) {
        if (jo != null)
            deserialize(jo);
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;

        String text = "";
        Position viewpoint = camera.getViewpoint();
        if (Display.multiview) {
            ImageLayer im = ImageLayers.getImageLayerInViewport(vp.idx);
            if (im != null) {
                text = ' ' + im.getName();
                viewpoint = im.getMetaData().getViewpoint();
            }
        }
        text = viewpoint.time.toString() + text;

        if (extra) {
            text += String.format(" | D\u2609: %7.4fau", viewpoint.distance * Sun.MeanEarthDistanceInv);
            if (!Display.multiview) {
                text += " | FOV: " + formatFOV(camera, vp);
            }
        }

        int size = (int) (vp.height * (scale * 0.01 * 0.024));

        int deltaX = (int) (vp.height * 0.01);
        int deltaY = top ? (int) (vp.height - Display.pixelScale[1] * deltaX - size) : deltaX; //!

        SdfTextRenderer renderer = GLText.renderer();
        float textScaleFactor = size / renderer.getFontSize();
        renderer.beginRendering(vp.width, vp.height);
        renderer.setColor(GLText.SHADOW_COLOR);
        renderer.draw(text, deltaX + GLText.SHADOW_OFFSET_X, deltaY + GLText.SHADOW_OFFSET_Y, 0, textScaleFactor);
        renderer.setColor(Colors.LightGrayFloat);
        renderer.draw(text, deltaX, deltaY, 0, textScaleFactor);
        renderer.endRendering();
    }

    private static String formatFOV(Camera camera, Viewport vp) {
        if (Display.mode == ProjectionMode.HPC)
            return formatHpcFOV(camera, vp);
        return formatOrthoFOV(camera.getCameraWidth(vp));
    }

    private static String formatOrthoFOV(double r) {
        if (r < 2 * 32 * Sun.Radius)
            return String.format("%6.4fR\u2609", r);
        else
            return String.format("%6.4fau", r * Sun.MeanEarthDistanceInv);
    }

    private static String formatHpcFOV(Camera camera, Viewport vp) {
        int centerX = vp.x + vp.width / 2;
        int centerY = vp.yAWT + vp.height / 2;

        Vec2 left = Display.mode.mouseToGrid(camera, vp, Display.gridType, vp.x, centerY);
        Vec2 right = Display.mode.mouseToGrid(camera, vp, Display.gridType, vp.x + vp.width - 1, centerY);
        Vec2 bottom = Display.mode.mouseToGrid(camera, vp, Display.gridType, centerX, vp.yAWT + vp.height - 1);
        Vec2 top = Display.mode.mouseToGrid(camera, vp, Display.gridType, centerX, vp.yAWT);

        double minX = Display.mode.scale.getInterpolatedXValue(0);
        double maxX = Display.mode.scale.getInterpolatedXValue(1);
        double minY = Display.mode.scale.getInterpolatedYValue(0);
        double maxY = Display.mode.scale.getInterpolatedYValue(1);

        double width = Math.abs(Math.clamp(right.x, minX, maxX) - Math.clamp(left.x, minX, maxX));
        double height = Math.abs(Math.clamp(top.y, minY, maxY) - Math.clamp(bottom.y, minY, maxY));
        return String.format("%6.2f\u00B0\u00D7%6.2f\u00B0", width, height);
    }

    @Override
    public void init() {}

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "Timestamp";
    }

    @Override
    public void dispose() {}

    public int getScale() {
        return scale;
    }

    public void setScale(int _scale) {
        scale = _scale;
        MovieDisplay.display();
    }

    public boolean isExtra() {
        return extra;
    }

    public void setExtra(boolean _extra) {
        extra = _extra;
        MovieDisplay.display();
    }

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean _top) {
        top = _top;
        MovieDisplay.display();
    }

}
