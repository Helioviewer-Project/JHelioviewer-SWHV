package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;

public class RasterLine {

    private RasterLine() {}

    // Example:
    // BufVertex vexBuf = new BufVertex(RasterLine.vertexCount(2) * GLSLShape.stride);
    // RasterLine.putVertical(camera, vp, targetX, y0, y1, 1.5, Colors.Green, vexBuf);
    // RasterLine.putHorizontal(camera, vp, x0, x1, targetY, 1.5, Colors.Green, vexBuf);

    public static int vertexCount(int lineCount) {
        return 6 * lineCount;
    }

    public static double snapVertical(Camera camera, Viewport vp, double x) {
        return snapVerticalCenter(camera, vp, vp.aspect * x) / vp.aspect;
    }

    public static double snapHorizontal(Camera camera, Viewport vp, double y) {
        return snapHorizontalCenter(camera, vp, y);
    }

    public static void putVertical(Camera camera, Viewport vp, double x, double startY, double stopY, double thicknessPixels, byte[] color, BufVertex vexBuf) {
        double centerX = snapVerticalCenter(camera, vp, x);
        double halfThickness = halfThickness(camera, vp, thicknessPixels);
        vexBuf.putQuad2D((float) (centerX - halfThickness), (float) startY, (float) (centerX + halfThickness), (float) stopY, color);
    }

    public static void putHorizontal(Camera camera, Viewport vp, double startX, double stopX, double y, double thicknessPixels, byte[] color, BufVertex vexBuf) {
        double halfThickness = halfThickness(camera, vp, thicknessPixels);
        double centerY = snapHorizontalCenter(camera, vp, y);
        vexBuf.putQuad2D((float) startX, (float) (centerY - halfThickness), (float) stopX, (float) (centerY + halfThickness), color);
    }

    private static double snapVerticalCenter(Camera camera, Viewport vp, double centerX) {
        double widthAspect = camera.getCameraWidth(vp) * vp.aspect;
        double screenX = vp.x + vp.width * ((centerX + camera.getTranslationX()) / widthAspect + 0.5);
        double snappedScreenX = Math.rint(screenX - 0.5) + 0.5;
        return CameraHelper.computeUpX(camera, vp, snappedScreenX);
    }

    private static double snapHorizontalCenter(Camera camera, Viewport vp, double centerY) {
        double width = camera.getCameraWidth(vp);
        double screenY = vp.yAWT + vp.height * (0.5 - (centerY + camera.getTranslationY()) / width);
        double snappedScreenY = Math.rint(screenY - 0.5) + 0.5;
        return CameraHelper.computeUpY(camera, vp, snappedScreenY);
    }

    private static double halfThickness(Camera camera, Viewport vp, double thicknessPixels) {
        return 0.5 * thicknessPixels * camera.getCameraWidth(vp) / vp.height;
    }
}
