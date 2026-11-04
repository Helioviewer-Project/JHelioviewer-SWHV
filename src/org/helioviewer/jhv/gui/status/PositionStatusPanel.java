package org.helioviewer.jhv.gui.status;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.annotation.Annotations;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.TransferAccess;
import org.helioviewer.jhv.gui.component.StatusPanel;
import org.helioviewer.jhv.input.InputMouseListener;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.FastFormat;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLRenderer;

@SuppressWarnings("serial")
public final class PositionStatusPanel extends StatusPanel.StatusPlugin implements InputMouseListener {

    private static final int FIELD_WIDTH = 7;
    private static final String MISSING = String.format("%" + FIELD_WIDTH + "s", "--");
    private final int[] labels = new int[12];
    private int labelCount;

    private final StringBuilder textBuffer = new StringBuilder(128);

    public PositionStatusPanel() {
        setText(formatOrtho(Vec2.NAN, GridType.Viewpoint, 0, 0, 0, 0));
        setLabelRanges(labels);
    }

    private void update(int x, int y) {
        Viewport vp = Display.getActiveViewport();
        MapView mv = GLRenderer.getMapView();
        Vec2 coord = mv.mouseToMap(vp, x, y);

        if (mv.isHpc()) {
            setText(formatHpc(coord));
        } else if (mv.isLatitudinal()) {
            setText(formatLati(coord, mv.gridType()));
        } else if (mv.isRadialWarp() || mv.isRectWarp()) {
            setText(formatAngleRadius(coord));
        } else {
            Vec3 v = mv.mouseToSky(vp, x, y);
            if (v == null) {
                setText(formatOrtho(Vec2.NAN, mv.gridType(), 0, 0, 0, 0));
            } else {
                String annStr = "";
                double r = Math.sqrt(v.x * v.x + v.y * v.y);
                Position viewpoint = GLRenderer.getDisplayedViewpoint();

                String annData = Annotations.getAnnotationData();
                if (annData != null)
                    annStr = annData;

                double zeta = viewpoint.distance - v.z;
                double px = (180 / Math.PI) * Math.atan2(v.x, zeta);
                double py = (180 / Math.PI) * Math.atan2(v.y, Math.sqrt(v.x * v.x + zeta * zeta));
                double pa = MathUtils.mapTo0To360((180 / Math.PI) * Math.atan2(v.y, v.x) - (DisplayController.getViewpointUpdate().dragAxis() == Vec3.ZAxis ? 0 : 90)); // w.r.t. axis
                String ortho = formatOrtho(coord, mv.gridType(), r, pa, px, py);
                if (!annStr.isEmpty()) {
                    int offset = annStr.length() + 4;
                    for (int index = 0; index < labels.length; index++)
                        labels[index] += offset;
                }
                setText(annStr.isEmpty() ? ortho : annStr + "    " + ortho);
            }
        }
    }

    private static StringBuilder appendXY(StringBuilder sb, double p) {
        if (Math.abs(p) < 1) // accomodate SOLO
            FastFormat.appendInteger(sb, Math.round(3600 * p), FIELD_WIDTH, true).append('″');
        else
            appendDegrees(sb, p);
        return sb;
    }

    private static StringBuilder appendR(StringBuilder sb, double r) {
        if (r < 32 * Sun.Radius)
            FastFormat.appendFixed2(sb, r, FIELD_WIDTH, false).append(" R☉");
        else
            FastFormat.appendFixed2(sb, r * Sun.MeanEarthDistanceInv, FIELD_WIDTH, false).append(" au");
        return sb;
    }

    private static StringBuilder appendDegrees(StringBuilder sb, double value) {
        return FastFormat.appendFixed2(sb, value, FIELD_WIDTH, true).append('°');
    }

    private void appendLabel(String text) {
        labels[labelCount++] = textBuffer.length();
        textBuffer.append(text);
        labels[labelCount++] = textBuffer.length();
        textBuffer.append(' ');
    }

    private void appendDegreePair(Vec2 coord, GridType gridType) {
        appendLabel("φ");
        if (coord == Vec2.NAN)
            textBuffer.append(MISSING).append('°');
        else
            appendDegrees(textBuffer, gridType.displayLongitude(coord.x));
        textBuffer.append("  ");
        appendLabel("θ");
        if (coord == Vec2.NAN)
            textBuffer.append(MISSING).append('°');
        else
            appendDegrees(textBuffer, coord.y);
    }

    private String finishBuffer() {
        // Keep each label next to its value; reserve stable field width before the label.
        for (int index = 0; index < labelCount; index += 2) {
            int start = labels[index];
            int end = labels[index + 1];
            int value = end + 1;
            while (value < textBuffer.length() && textBuffer.charAt(value) == ' ')
                value++;
            int padding = value - end - 1;
            for (int character = end - 1; character >= start; character--)
                textBuffer.setCharAt(character + padding, textBuffer.charAt(character));
            for (int character = start; character < start + padding; character++)
                textBuffer.setCharAt(character, ' ');
            textBuffer.setCharAt(end + padding, ' ');
            labels[index] += padding;
            labels[index + 1] += padding;
        }
        // Modes expose two or six measurements. Unused label ranges end at the text boundary.
        Arrays.fill(labels, labelCount, labels.length, textBuffer.length());
        return textBuffer.toString();
    }

    private String formatLati(@Nonnull Vec2 coord, GridType gridType) {
        resetBuffer();
        appendDegreePair(coord, gridType);
        return finishBuffer();
    }

    private String formatHpc(@Nonnull Vec2 coord) {
        resetBuffer();
        appendLabel("Tx");
        if (coord == Vec2.NAN)
            textBuffer.append(MISSING);
        else
            appendXY(textBuffer, coord.x);
        textBuffer.append("  ");
        appendLabel("Ty");
        if (coord == Vec2.NAN)
            textBuffer.append(MISSING);
        else
            appendXY(textBuffer, coord.y);
        return finishBuffer();
    }

    private String formatAngleRadius(@Nonnull Vec2 coord) {
        resetBuffer();
        appendLabel("θ");
        if (coord == Vec2.NAN)
            textBuffer.append(MISSING).append('°');
        else
            appendDegrees(textBuffer, coord.x);
        textBuffer.append("  ");
        appendLabel("ρ");
        if (coord == Vec2.NAN)
            textBuffer.append(MISSING).append(" R☉");
        else
            appendR(textBuffer, coord.y);
        return finishBuffer();
    }

    private String formatOrtho(@Nonnull Vec2 coord, GridType gridType, double r, double pa, double px, double py) {
        resetBuffer();
        appendLabel("ρ");
        appendR(textBuffer, r).append("  ");
        appendLabel("ψ");
        appendDegrees(textBuffer, pa).append("    ");
        appendDegreePair(coord, gridType);
        textBuffer.append("    ");
        appendLabel("x");
        appendXY(textBuffer, px).append("  ");
        appendLabel("y");
        appendXY(textBuffer, py);
        return finishBuffer();
    }

    private StringBuilder resetBuffer() {
        labelCount = 0;
        textBuffer.setLength(0);
        return textBuffer;
    }

    @Override
    public void mouseDragged(PointerEvent e) {
        update(e.x(), e.y());
    }

    @Override
    public void mouseMoved(PointerEvent e) {
        update(e.x(), e.y());
    }

    @Override
    public void mouseClicked(PointerEvent e) {
        maybeCopyToClipboard(e);
    }

    @Override
    public void mousePressed(PointerEvent e) {
        maybeCopyToClipboard(e);
    }

    @Override
    public void mouseReleased(PointerEvent e) {
        maybeCopyToClipboard(e);
    }

    private void maybeCopyToClipboard(PointerEvent e) {
        if (e.popupTrigger() || e.button() == 3)
            TransferAccess.writeClipboard(GLRenderer.getDisplayedViewpoint().time.toString() + " " + getText());
    }

}
