package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DSphere extends GL3DMesh {
    private final int resolutionX;
    private final int resolutionY;
    private final double radius;

    private GL3DVec3d center;
    private final GL3DVec3d centerOS = new GL3DVec3d(0, 0, 0);

    public GL3DSphere(double radius, int resolutionX, int resolutionY, GL3DVec4f color) {
        this("Sphere", radius, resolutionX, resolutionY, color);
    }

    public GL3DSphere(String name, double radius, int resolutionX, int resolutionY, GL3DVec4f color) {
        super(name, color);
        this.radius = radius;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
    }

    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {

        for (int latNumber = 0; latNumber <= this.resolutionX; latNumber++) {
            double theta = latNumber * Math.PI / resolutionX;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int longNumber = 0; longNumber <= resolutionY; longNumber++) {
                double phi = longNumber * 2 * Math.PI / resolutionY;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double x = cosPhi * sinTheta;
                double y = cosTheta;
                double z = sinPhi * sinTheta;

                positions.add(new GL3DVec3d(radius * x, radius * y, radius * z));
                normals.add(new GL3DVec3d(x, y, z));
            }
        }

        for (int latNumber = 0; latNumber < this.resolutionX; latNumber++) {
            for (int longNumber = 0; longNumber < resolutionY; longNumber++) {
                int first = (latNumber * (resolutionY + 1)) + longNumber;
                int second = first + resolutionY + 1;

                indices.add(first);
                indices.add(first + 1);
                indices.add(second + 1);
                indices.add(second);

            }
        }

        return GL3DMeshPrimitive.QUADS;
    }

    @Override
    public void shapeInit(GL3DState state) {
        super.shapeInit(state);
        this.center = this.wm.multiply(this.centerOS);
    }

    @Override
    public void shapeUpdate(GL3DState state) {
        this.center = this.wm.multiply(this.centerOS);
    }

    public GL3DVec3d getCenter() {
        return center;
    }

}
