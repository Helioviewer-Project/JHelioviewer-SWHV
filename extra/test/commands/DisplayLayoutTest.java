package org.helioviewer.jhv.display;

public final class DisplayLayoutTest {

    public static void main(String[] args) {
        check(DisplayLayout.MAX_VIEWPORTS == 6, "The viewport limit must remain six");
        for (int[] size : new int[][]{{1200, 800}, {101, 103}}) {
            for (int count = 1; count <= DisplayLayout.MAX_VIEWPORTS; count++) {
                Viewport[] viewports = DisplayLayout.viewports(size[0], size[1], count);
                check(viewports.length == count, "Layout has the wrong viewport count");
                long area = 0;
                for (int i = 0; i < count; i++) {
                    Viewport viewport = viewports[i];
                    check(viewport != null && viewport.idx == i, "Layout has an unassigned or incorrectly indexed viewport");
                    check(viewport.x >= 0 && viewport.yAWT >= 0 && viewport.x + viewport.width <= size[0]
                            && viewport.yAWT + viewport.height <= size[1], "Viewport exceeds the canvas");
                    check(viewport.yGL == size[1] - viewport.yAWT - viewport.height, "Viewport GL and AWT bounds disagree");
                    area += (long) viewport.width * viewport.height;
                    for (int j = 0; j < i; j++) {
                        Viewport other = viewports[j];
                        check(viewport.x >= other.x + other.width || other.x >= viewport.x + viewport.width
                                || viewport.yAWT >= other.yAWT + other.height || other.yAWT >= viewport.yAWT + viewport.height,
                                "Viewports overlap");
                    }
                }
                check(area == (long) size[0] * size[1], "Layout does not cover the canvas");
            }
            for (int count : new int[]{-1, 0, DisplayLayout.MAX_VIEWPORTS + 1}) {
                Viewport[] viewports = DisplayLayout.viewports(size[0], size[1], count);
                check(viewports.length == 1 && viewports[0].idx == 0 && viewports[0].x == 0 && viewports[0].yAWT == 0
                        && viewports[0].width == size[0] && viewports[0].height == size[1], "Invalid layout count lost the full-canvas fallback");
            }
        }
        System.out.println("PASS: six-viewport limit, supported layouts tile the canvas, unsupported counts retain the fallback");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
