package org.helioviewer.jhv.display;

public final class DisplayLayout {

    static Viewport fullViewport(int x, int y, int width, int height, int fullHeight) {
        return viewport(-1, x, y, width, height, fullHeight);
    }

    public static Viewport viewport(int idx, int x, int y, int width, int height, int fullHeight) {
        return new Viewport(idx, x, y, width, height, fullHeight);
    }

    private static final int[][] ROW_LAYOUTS = {
            {}, // 0
            {1}, // 1
            {2}, // 2
            {2, 1}, // 3
            {2, 2}, // 4
            {3, 2}, // 5
            {3, 3}  // 6
    };

    static Viewport[] viewports(int width, int height, int count) {
        if (count < 1 || count >= ROW_LAYOUTS.length) {
            return new Viewport[]{viewport(0, 0, 0, width, height, height)};
        }

        Viewport[] vps = new Viewport[count];
        int[] layout = ROW_LAYOUTS[count];
        int numRows = layout.length;
        int idx = 0;
        int y = 0;

        for (int r = 0; r < numRows; r++) {
            int numCols = layout[r];
            int nextY = ((r + 1) * height) / numRows;
            int rowHeight = nextY - y;
            int x = 0;

            for (int c = 0; c < numCols; c++) {
                int nextX = ((c + 1) * width) / numCols;
                vps[idx] = viewport(idx, x, y, nextX - x, rowHeight, height);
                x = nextX;
                idx++;
            }
            y = nextY;
        }
        return vps;
    }

    private DisplayLayout() {}
}
