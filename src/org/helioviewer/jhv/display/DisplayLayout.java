package org.helioviewer.jhv.display;

public class DisplayLayout {

    private DisplayLayout() {
    }

    static Viewport fullViewport(int x, int y, int width, int height, int fullHeight) {
        return viewport(-1, x, y, width, height, fullHeight);
    }

    public static Viewport viewport(int idx, int x, int y, int width, int height, int fullHeight) {
        return new Viewport(idx, x, y, width, height, fullHeight);
    }

    static Viewport[] viewports(int width, int height, int count) {
        return switch (count) {
            case 2 -> new Viewport[]{
                    viewport(0, 0, 0, width / 2, height, height),
                    viewport(1, width / 2, 0, width / 2, height, height)};
            case 3 -> new Viewport[]{
                    viewport(0, 0, 0, width / 2, height / 2, height),
                    viewport(1, width / 2, 0, width / 2, height / 2, height),
                    viewport(2, 0, height / 2, width, height / 2, height)};
            case 4 -> new Viewport[]{
                    viewport(0, 0, 0, width / 2, height / 2, height),
                    viewport(1, width / 2, 0, width / 2, height / 2, height),
                    viewport(2, 0, height / 2, width / 2, height / 2, height),
                    viewport(3, width / 2, height / 2, width / 2, height / 2, height)};
            case 5 -> new Viewport[]{
                    viewport(0, 0, 0, width / 3, height / 2, height),
                    viewport(1, width / 3, 0, width / 3, height / 2, height),
                    viewport(2, 2 * width / 3, 0, width / 3, height / 2, height),
                    viewport(3, 0, height / 2, width / 2, height / 2, height),
                    viewport(4, width / 2, height / 2, width / 2, height / 2, height)};
            case 6 -> new Viewport[]{
                    viewport(0, 0, 0, width / 3, height / 2, height),
                    viewport(1, width / 3, 0, width / 3, height / 2, height),
                    viewport(2, 2 * width / 3, 0, width / 3, height / 2, height),
                    viewport(3, 0, height / 2, width / 3, height / 2, height),
                    viewport(4, width / 3, height / 2, width / 3, height / 2, height),
                    viewport(5, 2 * width / 3, height / 2, width / 3, height / 2, height)};
            default -> new Viewport[]{viewport(0, 0, 0, width, height, height)};
        };
    }

}
