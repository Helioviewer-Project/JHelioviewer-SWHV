package org.helioviewer.gl3d.model.image;

import java.util.List;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.GL3DHelper;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.view.GL3DImageTextureView;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToTextureConversion;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.TextureCoordinateSystem;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * A GL3DImageCorona maps the coronal part of an image layer onto an image plane
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageCorona extends GL3DImageMesh {
    protected Vector2dInt mappingResolution = new Vector2dInt(2, 2);
    private TextureCoordinateSystem textureCoordinateSystem;

    Region lastRegion = null;
    private GL3DImageLayer layer = null;


    public GL3DImageCorona(String name, GL3DImageTextureView imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram, GL3DImageLayer imageLayer) {
        super(name, imageTextureView, vertexShaderProgram, fragmentShaderProgram);
        this.layer = imageLayer;
    }

    public GL3DImageCorona(GL3DImageTextureView imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram ,GL3DImageLayer imageLayer) {
        this("Corona", imageTextureView, vertexShaderProgram, fragmentShaderProgram, imageLayer);
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        Region region = this.capturedRegion;
        if (region != null) {
            MetaData metaData = this.layer.metaDataView.getMetaData();

            SolarImageCoordinateSystem solarDiskCS = new SolarImageCoordinateSystem();
            textureCoordinateSystem = new TextureCoordinateSystem(this.imageTextureView.getTextureScale(), region);
            // Read Boundaries on Solar Disk
            CoordinateVector orientationVector = this.layer.getOrientation();
            CoordinateConversion toViewSpace = this.layer.getCoordinateSystem().getConversion(state.getActiveCamera().getViewSpaceCoordinateSystem());

            GL3DVec3d orientation = GL3DHelper.toVec(toViewSpace.convert(orientationVector));
            orientation.normalize();

            {
                int vertexCounter = 0;

                pushVertex(metaData.getPhysicalUpperLeft(), positions, normals, textCoords, colors,0.0,1.0);
                pushVertex(metaData.getPhysicalUpperRight(), positions, normals, textCoords, colors,1.0,1.0);
                pushVertex(metaData.getPhysicalLowerRight(), positions, normals, textCoords, colors,1.0,0.0);
                pushVertex(metaData.getPhysicalLowerLeft(), positions, normals, textCoords, colors,0.0,0.0);
                indices.add(vertexCounter + 0);
                indices.add(vertexCounter + 1);
                indices.add(vertexCounter + 2);
                indices.add(vertexCounter + 3);
            }
        }

        return GL3DMeshPrimitive.QUADS;
    }

    private void pushVertex(Vector2dDouble position, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> texCoords, List<GL3DVec4d> colors, double tx, double ty) {
        positions.add(new GL3DVec3d(position.getX(), position.getY(), 0));
        colors.add(new GL3DVec4d(0, 0, 0, 1));
        texCoords.add(new GL3DVec2d(tx, ty));
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }

    public Region getCapturedRegion() { return capturedRegion; }

}
