package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

import org.json.JSONObject;

public class TimestampLayer extends AbstractLayer {

    public static final int MIN_SCALE = 50;
    public static final int MAX_SCALE = 300;
    private static final float FONT_SIZE = 18;
    private static final float DESCENT = 0.25f;

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
    public void renderFloat(MapView mv, Viewport vp) {
        if (!enabled)
            return;

        String text = "";
        Position viewpoint = mv.viewpoint();
        if (Display.multiview) {
            ImageLayer im = ImageLayers.getImageLayerInViewport(vp.idx);
            if (im != null) {
                text = ' ' + im.getName();
                viewpoint = im.getMetaData().getViewpoint();
            }
        }
        text = viewpoint.time.toString() + text;

        if (extra) {
            text += String.format(" | D☉: %7.4fau", viewpoint.distance * Sun.MeanEarthDistanceInv);
            if (!Display.multiview) {
                text += " | FOV: " + formatFOV(mv, vp);
            }
        }

        int deltaX = (int) (vp.height * 0.01);
        int availableWidth = vp.width - 2 * deltaX;
        int availableHeight = vp.height - 2 * deltaX;
        if (availableWidth <= 0)
            return;

        float shadowScale = vp.height / 1024f;

        SdfTextRenderer renderer = GLText.renderer();
        SdfTextRenderer.LineMetrics metrics = renderer.measureLine(text);
        float fontSize = renderer.getFontSize();
        float requestedSize = FONT_SIZE * (scale / 100f) * Display.getRenderPixelsPerPoint();
        float size = Math.min(requestedSize, availableWidth * fontSize / metrics.width());
        size = Math.min(size, availableHeight / (1 + DESCENT));
        float textScaleFactor = size / fontSize;
        float shadowY = GLText.SHADOW_OFFSET_Y * shadowScale;
        float deltaY = top ? vp.height - deltaX - size
                : Math.max(deltaX, metrics.descent() * textScaleFactor - shadowY);
        renderer.beginRendering(vp.width, vp.height);
        renderer.setColor(GLText.SHADOW_COLOR);
        renderer.draw(text, deltaX + GLText.SHADOW_OFFSET_X * shadowScale,
                deltaY + shadowY, 0, textScaleFactor);
        renderer.setColor(Colors.LightGrayFloat);
        renderer.draw(text, deltaX, deltaY, 0, textScaleFactor);
        renderer.endRendering();
    }

    private static String formatFOV(MapView mv, Viewport vp) {
        if (mv.isHpc())
            return formatHpcFOV(mv, vp);
        return formatOrthoFOV(mv.cameraWidth(vp));
    }

    private static String formatOrthoFOV(double r) {
        if (r < 2 * 32 * Sun.Radius)
            return String.format("%6.4fR☉", r);
        else
            return String.format("%6.4fau", r * Sun.MeanEarthDistanceInv);
    }

    private static String formatHpcFOV(MapView mv, Viewport vp) {
        int centerX = vp.x + vp.width / 2;
        int centerY = vp.yAWT + vp.height / 2;

        Vec2 left = mv.mouseToMap(vp, vp.x, centerY);
        Vec2 right = mv.mouseToMap(vp, vp.x + vp.width - 1, centerY);
        Vec2 bottom = mv.mouseToMap(vp, centerX, vp.yAWT + vp.height - 1);
        Vec2 top = mv.mouseToMap(vp, centerX, vp.yAWT);

        MapScale scale = mv.scale(vp);
        double minX = scale.toMapX(0);
        double maxX = scale.toMapX(1);
        double minY = scale.toMapY(0);
        double maxY = scale.toMapY(1);

        double width = Math.abs(Math.clamp(right.x, minX, maxX) - Math.clamp(left.x, minX, maxX));
        double height = Math.abs(Math.clamp(top.y, minY, maxY) - Math.clamp(bottom.y, minY, maxY));
        return String.format("%6.2f°×%6.2f°", width, height);
    }

    @Override
    public void init() {}

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
        DisplayController.display();
    }

    public boolean isExtra() {
        return extra;
    }

    public void setExtra(boolean _extra) {
        extra = _extra;
        DisplayController.display();
    }

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean _top) {
        top = _top;
        DisplayController.display();
    }

}
