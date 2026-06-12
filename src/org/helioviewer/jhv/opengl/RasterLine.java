package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.display.Viewport;

public class RasterLine {

    // Example:
    // BufVertex vexBuf = new BufVertex(RasterLine.vertexCount(2) * GLSLShape.stride);
    // RasterLine.putVertical(vp, width, tx, targetX, y0, y1, 1.5, Colors.Green, vexBuf);
    // RasterLine.putHorizontal(vp, width, ty, x0, x1, targetY, 1.5, Colors.Green, vexBuf);

    public static int vertexCount(int lineCount) {
        return 6 * lineCount;
    }

    public static double snapVertical(Viewport vp, double width, double tx, double x) {
        return snapVerticalCenter(vp, width, tx, vp.aspect * x) / vp.aspect;
    }

    public static double snapHorizontal(Viewport vp, double width, double ty, double y) {
        return snapHorizontalCenter(vp, width, ty, y);
    }

    public static void putVertical(Viewport vp, double width, double tx, double x, double startY, double stopY, double thicknessPixels, byte[] color, BufVertex vexBuf) {
        double centerX = snapVerticalCenter(vp, width, tx, x);
        double halfThickness = halfThickness(vp, width, thicknessPixels);
        vexBuf.putQuad2D((float) (centerX - halfThickness), (float) startY, (float) (centerX + halfThickness), (float) stopY, color);
    }

    public static void putHorizontal(Viewport vp, double width, double ty, double startX, double stopX, double y, double thicknessPixels, byte[] color, BufVertex vexBuf) {
        double halfThickness = halfThickness(vp, width, thicknessPixels);
        double centerY = snapHorizontalCenter(vp, width, ty, y);
        vexBuf.putQuad2D((float) startX, (float) (centerY - halfThickness), (float) stopX, (float) (centerY + halfThickness), color);
    }

    private static double snapVerticalCenter(Viewport vp, double width, double tx, double centerX) {
        double widthAspect = width * vp.aspect;
        double screenX = vp.x + vp.width * ((centerX + tx) / widthAspect + 0.5);
        double snappedScreenX = Math.rint(screenX - 0.5) + 0.5;
        return ((snappedScreenX - vp.x) / vp.width - 0.5) * widthAspect - tx;
    }

    private static double snapHorizontalCenter(Viewport vp, double width, double ty, double centerY) {
        double screenY = vp.yAWT + vp.height * (0.5 - (centerY + ty) / width);
        double snappedScreenY = Math.rint(screenY - 0.5) + 0.5;
        return (0.5 - (snappedScreenY - vp.yAWT) / vp.height) * width - ty;
    }

    private static double halfThickness(Viewport vp, double width, double thicknessPixels) {
        return 0.5 * thicknessPixels * width / vp.height;
    }

    private RasterLine() {}
}
