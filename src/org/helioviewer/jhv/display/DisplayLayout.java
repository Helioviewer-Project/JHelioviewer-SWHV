package org.helioviewer.jhv.display;

final class DisplayLayout {

    private DisplayLayout() {
    }

    static Viewport fullViewport(int x, int y, int width, int height) {
        return new Viewport(-1, x, y, width, height);
    }

    static Viewport[] viewports(int width, int height, int count) {
        return switch (count) {
            case 2 -> new Viewport[]{
                    new Viewport(0, 0, 0, width / 2, height),
                    new Viewport(1, width / 2, 0, width / 2, height)};
            case 3 -> new Viewport[]{
                    new Viewport(0, 0, 0, width / 2, height / 2),
                    new Viewport(1, width / 2, 0, width / 2, height / 2),
                    new Viewport(2, 0, height / 2, width, height / 2)};
            case 4 -> new Viewport[]{
                    new Viewport(0, 0, 0, width / 2, height / 2),
                    new Viewport(1, width / 2, 0, width / 2, height / 2),
                    new Viewport(2, 0, height / 2, width / 2, height / 2),
                    new Viewport(3, width / 2, height / 2, width / 2, height / 2)};
            case 5 -> new Viewport[]{
                    new Viewport(0, 0, 0, width / 3, height / 2),
                    new Viewport(1, width / 3, 0, width / 3, height / 2),
                    new Viewport(2, 2 * width / 3, 0, width / 3, height / 2),
                    new Viewport(3, 0, height / 2, width / 2, height / 2),
                    new Viewport(4, width / 2, height / 2, width / 2, height / 2)};
            case 6 -> new Viewport[]{
                    new Viewport(0, 0, 0, width / 3, height / 2),
                    new Viewport(1, width / 3, 0, width / 3, height / 2),
                    new Viewport(2, 2 * width / 3, 0, width / 3, height / 2),
                    new Viewport(3, 0, height / 2, width / 3, height / 2),
                    new Viewport(4, width / 3, height / 2, width / 3, height / 2),
                    new Viewport(5, 2 * width / 3, height / 2, width / 3, height / 2)};
            default -> new Viewport[]{new Viewport(0, 0, 0, width, height)};
        };
    }

}
