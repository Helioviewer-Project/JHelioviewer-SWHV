package org.helioviewer.gl3d.model.image;

import java.util.List;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.wcs.Cartesian3DCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToTextureConversion;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.TextureCoordinateSystem;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * A GL3DImageCorona maps the coronal part of an image layer onto an image plane
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageCorona extends GL3DImageMesh {
    protected Vector2dInt mappingResolution = new Vector2dInt(2, 2);

    private TextureCoordinateSystem textureCoordinateSystem;
    private SolarImageToTextureConversion solarDisk2TextureConversion;

    // private SolarImageToSolarPlaneConversion solarDisk2ViewSpaceConversion;

    public GL3DImageCorona(String name, GL3DImageTextureView imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram) {
        super(name, imageTextureView, vertexShaderProgram, fragmentShaderProgram);
    }

    public GL3DImageCorona(GL3DImageTextureView imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram) {
        this("Corona", imageTextureView, vertexShaderProgram, fragmentShaderProgram);
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        Region region = this.capturedRegion;

        if (region != null) {
            SolarImageCoordinateSystem solarDiskCS = new SolarImageCoordinateSystem();
            textureCoordinateSystem = new TextureCoordinateSystem(this.imageTextureView.getTextureScale(), region);
            // solarDisk2ViewSpaceConversion =
            // (SolarImageToSolarPlaneConversion)solarDiskCS.getConversion(state.getActiveCamera().getViewSpaceCoordinateSystem());
            solarDisk2TextureConversion = (SolarImageToTextureConversion) solarDiskCS.getConversion(textureCoordinateSystem);

            // Read Boundaries on Solar Disk
            CoordinateVector lowerLeftSolarDisk = solarDiskCS.createCoordinateVector(region.getCornerX(), region.getCornerY());
            CoordinateVector upperRightSolarDisk = solarDiskCS.createCoordinateVector(region.getUpperRightCorner().getX(), region.getUpperRightCorner().getY());

            double absStartX = lowerLeftSolarDisk.getValue(SolarImageCoordinateSystem.X_COORDINATE);
            double absStartY = lowerLeftSolarDisk.getValue(SolarImageCoordinateSystem.Y_COORDINATE);
            double absEndX = upperRightSolarDisk.getValue(SolarImageCoordinateSystem.X_COORDINATE);
            double absEndY = upperRightSolarDisk.getValue(SolarImageCoordinateSystem.Y_COORDINATE);

            double minStartX = absStartX;
            double minStartY = absStartY;
            double maxEndX = absEndX;
            double maxEndY = absEndY;

            int vertexCounter = 0;

            pushVertex(solarDiskCS.createCoordinateVector(minStartX, minStartY), 0, 0, positions, normals, textCoords, colors);
            pushVertex(solarDiskCS.createCoordinateVector(maxEndX, minStartY), 1, 0, positions, normals, textCoords, colors);
            pushVertex(solarDiskCS.createCoordinateVector(maxEndX, maxEndY), 1, 1, positions, normals, textCoords, colors);
            pushVertex(solarDiskCS.createCoordinateVector(minStartX, maxEndY), 0, 1, positions, normals, textCoords, colors);
            indices.add(vertexCounter + 0);
            indices.add(vertexCounter + 1);
            indices.add(vertexCounter + 2);
            indices.add(vertexCounter + 3);
        }
        // Log.debug("GL3DImageMesh.createMesh(): minStart(x:"+minStartX+", y:"+minStartY+"), maxEnd(x:"+maxEndX+", y:"+maxEndY+"), TextureScale("+this.imageTextureView.getTextureScale().getX()+":"+this.imageTextureView.getTextureScale().getY()+")");
        // Log.debug("GL3DImageMesh.createMesh(): minStart(x:"+minStartX/Constants.SunRadius+", y:"+minStartY/Constants.SunRadius+"), maxEnd(x:"+maxEndX/Constants.SunRadius+", y:"+maxEndY/Constants.SunRadius+"), TextureScale("+this.imageTextureView.getTextureScale().getX()+":"+this.imageTextureView.getTextureScale().getY()+")");

        return GL3DMeshPrimitive.QUADS;
    }

    private void pushVertex(CoordinateVector solarCoordinate, double longitude, double latitude, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> texCoords, List<GL3DVec4d> colors) {

        CoordinateVector textureCoordinates = solarDisk2TextureConversion.convert(solarCoordinate);
        double tx = textureCoordinates.getValue(TextureCoordinateSystem.X_COORDINATE);
        double ty = textureCoordinates.getValue(TextureCoordinateSystem.Y_COORDINATE);

        // CoordinateVector cartesianCoordinates =
        // solarDisk2ViewSpaceConversion.convert(solarCoordinate);
        double cx = solarCoordinate.getValue(Cartesian3DCoordinateSystem.X_COORDINATE);
        double cy = solarCoordinate.getValue(Cartesian3DCoordinateSystem.Y_COORDINATE);
        double cz = 0.0;// cartesianCoordinates.getValue(Cartesian3DCoordinateSystem.Z_COORDINATE);

        positions.add(new GL3DVec3d(cx, cy, cz));
        // normals.add(new GL3DVec3d(0, 0, 1));
        colors.add(new GL3DVec4d(0, 1, 0, 1));
        texCoords.add(new GL3DVec2d(tx, ty));
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }
}
