package org.helioviewer.gl3d.model.image;

import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToTextureConversion;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.TextureCoordinateSystem;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * Maps the solar disc part of an image layer onto an adaptive mesh that either
 * covers the entire solar disc or the just the part that is visible in the view
 * frustum.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageSphere extends GL3DImageMesh {
    protected Vector2dInt mappingResolution = new Vector2dInt((int) Math.pow(2, 6), (int) Math.pow(2, 6));

    private TextureCoordinateSystem textureCoordinateSystem;
    private SolarImageToTextureConversion solarImageToTextureConversion;
    private SolarImageToSolarSphereConversion solarImageToSolarSphereConversion;

    private SolarImageCoordinateSystem solarImageCS = new SolarImageCoordinateSystem();
    private SolarSphereCoordinateSystem solarSphereCS = new SolarSphereCoordinateSystem();

    public GL3DImageSphere(GL3DImageTextureView imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram) {
        super("Sphere", imageTextureView, vertexShaderProgram, fragmentShaderProgram);
    }

    public void shapeDraw(GL3DState state) {
        // this is the first one!
        if (parent.getParent().getFirst() == this.getParent()) {
            state.gl.glDisable(GL.GL_CULL_FACE);
            state.gl.glEnable(GL.GL_DEPTH_TEST);
            state.gl.glDisable(GL.GL_BLEND);
        } else {
            state.gl.glEnable(GL.GL_CULL_FACE);
            state.gl.glDisable(GL.GL_DEPTH_TEST);
            state.gl.glEnable(GL.GL_BLEND);
        }
        super.shapeDraw(state);
        // state.gl.glDisable(GL.GL_CULL_FACE);
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        if (this.capturedRegion != null) {
            // double height =
            // ((MetaDataView)this.imageTextureView.getAdapter(MetaDataView.class)).getMetaData().getPhysicalImageHeight();
            // double width =
            // ((MetaDataView)this.imageTextureView.getAdapter(MetaDataView.class)).getMetaData().getPhysicalImageWidth();
            // Log.debug("GL3dImageSphere: Phyiscal Size: "+width+":"+height+" "+this.getName());

            Region region = this.capturedRegion;

            textureCoordinateSystem = new TextureCoordinateSystem(textureScale, region);
            solarImageToSolarSphereConversion = (SolarImageToSolarSphereConversion) solarImageCS.getConversion(solarSphereCS);
            solarImageToTextureConversion = (SolarImageToTextureConversion) solarImageCS.getConversion(textureCoordinateSystem);

            solarImageToSolarSphereConversion.setAutoAdjustToValidValue(true);

            // Read Boundaries on Solar Disk
            CoordinateVector lowerLeftSolarDisk = solarImageCS.createCoordinateVector(region.getLowerLeftCorner().getX(), region.getLowerLeftCorner().getY());
            CoordinateVector upperRightSolarDisk = solarImageCS.createCoordinateVector(region.getUpperRightCorner().getX(), region.getUpperRightCorner().getY());

            double absStartX = lowerLeftSolarDisk.getValue(SolarImageCoordinateSystem.X_COORDINATE);
            double absStartY = lowerLeftSolarDisk.getValue(SolarImageCoordinateSystem.Y_COORDINATE);
            double absEndX = upperRightSolarDisk.getValue(SolarImageCoordinateSystem.X_COORDINATE);
            double absEndY = upperRightSolarDisk.getValue(SolarImageCoordinateSystem.Y_COORDINATE);

            double minStartX = Math.min(Math.max(-Constants.SunRadius, absStartX), Constants.SunRadius);
            double minStartY = Math.min(Math.max(-Constants.SunRadius, absStartY), Constants.SunRadius);
            double maxEndX = Math.max(Math.min(Constants.SunRadius, absEndX), -Constants.SunRadius);
            double maxEndY = Math.max(Math.min(Constants.SunRadius, absEndY), -Constants.SunRadius);

            double rangeX = maxEndX - minStartX;
            double rangeY = maxEndY - minStartY;
            double deltaX = rangeX / mappingResolution.getX();
            double deltaY = rangeY / mappingResolution.getY();

            int vertexCounter = 0;

            double lastSolarImageX = minStartX;

            GL3DVec3d eye = new GL3DVec3d();
            GL3DVec3d at = new GL3DVec3d();
            GL3DVec3d up = new GL3DVec3d();
            GL3DVec3d right = new GL3DVec3d();
            state.getActiveCamera().getVM().readLookAt(eye, at, up, right);
            eye.multiply(state.getActiveCamera().getZTranslation()).negate();

            for (int x = 0; x < mappingResolution.getX(); x++) {
                double lastSolarImageY = minStartY;
                double solarImageDeltaX = deltaX;

                for (int y = 0; y < mappingResolution.getY(); y++) {

                    double solarImageDeltaY = deltaY;

                    CoordinateVector v0 = solarImageCS.createCoordinateVector(lastSolarImageX, lastSolarImageY);
                    CoordinateVector v1 = solarImageCS.createCoordinateVector(lastSolarImageX + solarImageDeltaX, lastSolarImageY);
                    CoordinateVector v2 = solarImageCS.createCoordinateVector(lastSolarImageX + solarImageDeltaX, lastSolarImageY + solarImageDeltaY);
                    CoordinateVector v3 = solarImageCS.createCoordinateVector(lastSolarImageX, lastSolarImageY + solarImageDeltaY);

                    // only use faces that touch the sphere
                    if (solarImageCS.isInsideDisc(v0) || solarImageCS.isInsideDisc(v1) || solarImageCS.isInsideDisc(v2) || solarImageCS.isInsideDisc(v3)) {
                        createVertex(v0, positions, normals, textCoords, colors);
                        createVertex(v1, positions, normals, textCoords, colors);
                        createVertex(v2, positions, normals, textCoords, colors);
                        createVertex(v3, positions, normals, textCoords, colors);

                        if (lastSolarImageX < 0 && lastSolarImageY < 0 || lastSolarImageX >= 0 && lastSolarImageY >= 0) {// bottom
                                                                                                                         // left
                                                                                                                         // ||
                                                                                                                         // top
                                                                                                                         // right
                            indices.add(vertexCounter + 0);
                            indices.add(vertexCounter + 1);
                            indices.add(vertexCounter + 2);

                            indices.add(vertexCounter + 0);
                            indices.add(vertexCounter + 2);
                            indices.add(vertexCounter + 3);
                            // calcAngle(eye, _v0, _v1, _v2);
                            // calcAngle(eye, _v0, _v2, _v3);
                        } else if (lastSolarImageX >= 0 && lastSolarImageY < 0 || lastSolarImageX < 0 && lastSolarImageY >= 0) {// bottom
                                                                                                                                // right
                                                                                                                                // ||
                                                                                                                                // top
                                                                                                                                // left
                            indices.add(vertexCounter + 0);
                            indices.add(vertexCounter + 1);
                            indices.add(vertexCounter + 3);

                            indices.add(vertexCounter + 1);
                            indices.add(vertexCounter + 2);
                            indices.add(vertexCounter + 3);
                            // calcAngle(eye, _v0, _v1, _v3);
                            // calcAngle(eye, _v1, _v2, _v3);
                        }

                        vertexCounter += 4;

                    }

                    lastSolarImageY += solarImageDeltaY;
                }
                lastSolarImageX += solarImageDeltaX;
            }

        }

        // Log.debug("");
        return GL3DMeshPrimitive.TRIANGLES;
    }

    // private void calcAngle(GL3DVec3d eye, GL3DVec3d _v0, GL3DVec3d _v1,
    // GL3DVec3d _v2) {
    // GL3DVec3d quadCenter = new
    // GL3DVec3d().add(_v0).add(_v1).add(_v2).divide(3.0);
    // GL3DVec3d viewAxis = GL3DVec3d.subtract(eye, quadCenter).normalize();
    //
    // GL3DVec3d u = GL3DVec3d.subtract(_v1, _v0);
    // GL3DVec3d w = GL3DVec3d.subtract(_v2, _v0);
    // GL3DVec3d quadNormal = GL3DVec3d.cross(u, w).normalize();
    //
    // double angle = Math.acos(quadNormal.dot(viewAxis));
    //
    // Log.debug("GL3DImageSphere: Triangle Angle to Cam: "+Math.toDegrees(angle)+" ViewAxis: "+viewAxis+" Normal: "+quadNormal);
    // }
    //
    private GL3DVec3d createVertex(CoordinateVector solarImageCoordinate, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> texCoords, List<GL3DVec4d> colors) {

        CoordinateVector solarSphereCoordinate = solarImageToSolarSphereConversion.convert(solarImageCoordinate);
        double x = solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.X_COORDINATE);
        double y = solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.Y_COORDINATE);
        double z = solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.Z_COORDINATE);
        GL3DVec3d position = new GL3DVec3d(x, y, z);
        positions.add(position);

        CoordinateVector textureCoordinate = solarImageToTextureConversion.convert(solarSphereCoordinate);
        double tx = textureCoordinate.getValue(TextureCoordinateSystem.X_COORDINATE);
        double ty = textureCoordinate.getValue(TextureCoordinateSystem.Y_COORDINATE);
        texCoords.add(new GL3DVec2d(tx, ty));

        // colors.add(new GL3DVec4d(0, 1, 0, 0.5));
        // normals.add(new GL3DVec3d(x, y, z).divide(Constants.SunRadius));
        return position;
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }
}
