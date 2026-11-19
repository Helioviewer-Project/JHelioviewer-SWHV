package org.helioviewer.jhv.layers.viewpoint;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

public final class ViewpointObjectNames {

    private static final float[] DARK_TEXT = {.15f, .15f, .15f, 1};
    private static final float[] LIGHT_TEXT = {.92f, .92f, .92f, 1};
    private static final float[] LIGHT_SHADOW = {.7f, .7f, .7f, .7f};
    private static final byte[] DARK_LEADER = Colors.bytes(64, 64, 64, 128);
    private static final byte[] LIGHT_LEADER = Colors.bytes(192, 192, 192, 145);
    private static final int PLACEMENT_BANDS = 6;
    private static final int POSITIONS_PER_BAND = 6;
    private static final double[][] REFERENCE_RING_POINTS = referenceRingPoints(32);

    private record Anchor(String name, float x, float y, boolean reference) {}
    private record Label(Anchor anchor, float x, float y, float width, boolean displaced) {}

    private final GLSLLine leaders = new GLSLLine(true);
    private final BufVertex leaderBuf = new BufVertex();
    private final ArrayList<Anchor> anchors = new ArrayList<>();
    private Viewport drawnViewport;
    private float markerRadius;

    public void init() {
        leaders.init();
    }

    public void dispose() {
        clear();
        leaders.dispose();
    }

    public void clear() {
        drawnViewport = null;
        anchors.clear();
    }

    public void prepare(List<ViewpointOrbitGeometry.Marker> markers, @Nullable PositionLoad reference, MapView mv, Viewport vp,
                        Quat orbitFrameRotation, double pointFactor) {
        clear();
        if (!mv.isOrthographic())
            return;

        Quat rotation = Quat.rotateWithConjugate(mv.viewRotation(), orbitFrameRotation);
        double width = mv.cameraWidth(vp);
        double scale = vp.height / width;
        // Match Transform.ortho's depth clip for point centers.
        double clip = (float) (width < 32 ? 32 * Sun.Radius : 50 * Sun.MeanEarthDistance);
        markerRadius = (float) (0.5 * ViewpointOrbitGeometry.SIZE_PLANET * pointFactor);
        // At the edge the point shader can round the outer antialiased fringe to zero.
        double visibleRadius = Math.max(0, markerRadius - 0.125);
        double radiusSquared = visibleRadius * visibleRadius;
        double[] point = new double[3];
        for (ViewpointOrbitGeometry.Marker marker : markers) {
            point[0] = marker.x();
            point[1] = marker.y();
            point[2] = marker.z();
            rotation.qxv(point, point);
            double x = vp.width / 2.0 + (point[0] + mv.cameraTranslationX()) * scale;
            double y = vp.height / 2.0 + (point[1] + mv.cameraTranslationY()) * scale;
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(point[2]) || Math.abs(point[2]) > clip)
                continue;
            if (x < 0 || x > vp.width || y < 0 || y > vp.height) {
                // A marker centered outside the viewport must still cover a pixel inside it.
                double pixelDx = x - Math.clamp(Math.floor(x) + 0.5, 0.5, vp.width - 0.5);
                double pixelDy = y - Math.clamp(Math.floor(y) + 0.5, 0.5, vp.height - 0.5);
                if (pixelDx * pixelDx + pixelDy * pixelDy >= radiusSquared)
                    continue;
            }
            anchors.add(new Anchor(marker.load().target().toString(), (float) x, (float) y, marker.load() == reference));
        }
        drawnViewport = vp;
    }

    public void render(Viewport vp, float pixelScale) {
        if (drawnViewport != vp || anchors.isEmpty()) {
            clear();
            return;
        }

        SdfTextRenderer text = GLText.renderer();
        float size = 12 * pixelScale;
        float textScale = size / text.getFontSize();
        float padding = pixelScale;
        float height = 1.1f * size + 2 * padding;
        ArrayList<Label> labels = new ArrayList<>(anchors.size());
        byte[] leaderColor = Display.whiteBackground ? DARK_LEADER : LIGHT_LEADER;
        for (Anchor anchor : anchors) {
            float width = text.measureWidth(anchor.name) * textScale + 2 * padding;
            labels.add(place(anchor, width, height, pixelScale, labels, vp));
            if (anchor.reference)
                putReferenceRing(anchor, markerRadius + 2 * pixelScale, leaderColor);
        }

        for (Label label : labels) {
            if (!label.displaced)
                continue;
            float endX = Math.clamp(label.anchor.x, label.x, label.x + label.width);
            float endY = Math.clamp(label.anchor.y, label.y, label.y + height);
            float dx = endX - label.anchor.x;
            float dy = endY - label.anchor.y;
            float distance = (float) Math.hypot(dx, dy);
            if (distance <= markerRadius + 4 * pixelScale)
                continue;
            float start = (markerRadius + (label.anchor.reference ? 3 : 1) * pixelScale) / distance;
            float startX = label.anchor.x + dx * start;
            float startY = label.anchor.y + dy * start;
            if (crossesOtherName(label, labels, height, pixelScale, startX, startY, endX, endY))
                continue;
            leaderBuf.startLine(startX, startY, 0, 1, leaderColor);
            leaderBuf.putVertex(endX, endY, 0, 1, leaderColor);
            leaderBuf.endLine();
        }
        leaders.uploadAndClear(leaderBuf);

        text.beginRendering(vp.width, vp.height);
        leaders.renderLine(vp, 2 * pixelScale / vp.height);
        text.setColor(Display.whiteBackground ? LIGHT_SHADOW : GLText.SHADOW_COLOR);
        for (Label label : labels)
            text.draw(label.anchor.name, label.x + padding + pixelScale,
                    label.y + padding + .25f * size - pixelScale, 0, textScale);
        text.setColor(Display.whiteBackground ? DARK_TEXT : LIGHT_TEXT);
        for (Label label : labels)
            text.draw(label.anchor.name, label.x + padding, label.y + padding + .25f * size, 0, textScale);
        text.endRendering();
        clear();
    }

    private static double[][] referenceRingPoints(int segments) {
        double[][] points = new double[segments - 1][2];
        for (int i = 0; i < points.length; i++) {
            double angle = 2 * Math.PI * (i + 1) / segments;
            points[i][0] = Math.cos(angle);
            points[i][1] = Math.sin(angle);
        }
        return points;
    }

    private void putReferenceRing(Anchor anchor, float radius, byte[] color) {
        leaderBuf.startLine(anchor.x + radius, anchor.y, 0, 1, color);
        for (double[] point : REFERENCE_RING_POINTS) {
            float x = anchor.x + (float) (radius * point[0]);
            float y = anchor.y + (float) (radius * point[1]);
            leaderBuf.putVertex(x, y, 0, 1, color);
        }
        leaderBuf.putVertex(anchor.x + radius, anchor.y, 0, 1, color);
        leaderBuf.endLine();
    }

    private static boolean crossesOtherName(Label owner, List<Label> labels, float height, float margin,
                                            float startX, float startY, float endX, float endY) {
        Rectangle2D.Float bounds = new Rectangle2D.Float();
        for (Label label : labels) {
            if (label == owner)
                continue;
            bounds.setRect(label.x - margin, label.y - margin, label.width + 2 * margin, height + 2 * margin);
            if (bounds.intersectsLine(startX, startY, endX, endY))
                return true;
        }
        return false;
    }

    private Label place(Anchor anchor, float width, float height, float pixelScale, List<Label> labels, Viewport vp) {
        float gap = markerRadius + 3 * pixelScale;
        float bestOverlap = Float.POSITIVE_INFINITY;
        Label best = null;
        for (int slot = 0; slot < 2 + PLACEMENT_BANDS * POSITIONS_PER_BAND; slot++) {
            float x;
            float y;
            if (slot < 2) {
                x = slot == 0 ? anchor.x + gap : anchor.x - gap - width;
                y = anchor.y - height / 2;
            } else {
                int band = (slot - 2) / POSITIONS_PER_BAND + 1;
                int side = (slot - 2) % POSITIONS_PER_BAND;
                if (side < 4) {
                    x = side % 2 == 0 ? anchor.x + gap : anchor.x - gap - width;
                    float offset = band * (height + 2 * pixelScale);
                    y = anchor.y - height / 2 + (side < 2 ? offset : -offset);
                } else {
                    x = anchor.x - width / 2;
                    float offset = gap + (band - 1) * (height + 2 * pixelScale);
                    y = side == 4 ? anchor.y + offset : anchor.y - offset - height;
                }
            }
            float clampedX = Math.clamp(x, pixelScale, Math.max(pixelScale, vp.width - width - pixelScale));
            float clampedY = Math.clamp(y, pixelScale, Math.max(pixelScale, vp.height - height - pixelScale));
            float overlap = 0;
            for (Label label : labels)
                overlap += overlap(clampedX, clampedY, width, height, label.x - pixelScale, label.y - pixelScale,
                        label.width + 2 * pixelScale, height + 2 * pixelScale);
            for (Anchor marker : anchors)
                overlap += overlap(clampedX, clampedY, width, height, marker.x - markerRadius, marker.y - markerRadius,
                        2 * markerRadius, 2 * markerRadius);
            if (overlap < bestOverlap) {
                bestOverlap = overlap;
                best = new Label(anchor, clampedX, clampedY, width,
                        slot > 1 || Math.abs(x - clampedX) > pixelScale || Math.abs(y - clampedY) > pixelScale);
            }
            if (overlap == 0)
                break;
        }
        return best;
    }

    private static float overlap(float x, float y, float width, float height,
                                 float otherX, float otherY, float otherWidth, float otherHeight) {
        return Math.max(0, Math.min(x + width, otherX + otherWidth) - Math.max(x, otherX))
                * Math.max(0, Math.min(y + height, otherY + otherHeight) - Math.max(y, otherY));
    }
}
