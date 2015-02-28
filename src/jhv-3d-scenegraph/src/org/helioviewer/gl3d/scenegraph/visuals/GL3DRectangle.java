package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;

public class GL3DRectangle extends GL3DMesh {
    private double width;
    private double height;
    private int resX;
    private int resY;

    public GL3DRectangle(double width, double height) {
        this(width, height, 1, 1);
    }

    public GL3DRectangle(double width, double height, int resX, int resY) {
        super("Rectangle");
        this.width = width;
        this.height = height;
        this.resX = resX;
        this.resY = resY;
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        double xStart = -this.width / 2;
        double yStart = -this.height / 2;
        double dW = this.width / this.resX;
        double dH = this.height / this.resY;

        for (int r = 0; r <= this.resY; r++) {
            for (int c = 0; c <= this.resX; c++) {
                positions.add(new GL3DVec3d(xStart + c * dW, yStart + r * dH, 0));
                normals.add(new GL3DVec3d(0, 0, 1));
                colors.add(new GL3DVec4d(1, 0, 0, 1));
            }
        }

        for (int r = 0; r < this.resY; r++) {
            for (int c = 0; c < this.resX; c++) {
                int startIndexThisRow = r * (this.resX + 1) + c;
                int startIndexNextRow = (r + 1) * (this.resX + 1) + c;
                indices.add(startIndexThisRow);
                indices.add(startIndexThisRow + 1);
                indices.add(startIndexNextRow);

                indices.add(startIndexThisRow + 1);
                indices.add(startIndexNextRow + 1);
                indices.add(startIndexNextRow);
            }
        }

        return GL3DMeshPrimitive.TRIANGLES;
    }

}
