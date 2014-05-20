package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

public class GL3DText extends GL3DMesh {
    private final double width;
    private final double height;

    private boolean initiated = false;
    private int texture_id;
    private String text;
    private double x0;
    private double y0;
    private double z0;
    private String font;
    private GL3DVec4d textColor;

    public GL3DText(double width, double height, double x0, double y0, double z0, String text, String font, GL3DVec4d textColor) {
        this(width, height, 1, 1);
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.text = text;
        this.font = font;
        this.textColor = textColor;
    }

    public GL3DText(double width, double height, double x0, double y0, double z0, String text, GL3DVec4d textColor) {
        this(width, height, 1, 1);
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.text = text;
        this.font = "Monospaced";
        this.textColor = textColor;
    }

    public GL3DText(double width, double height, int resX, int resY) {
        super("RectangleDouble");
        this.width = width;
        this.height = height;
        this.drawBits.set(Bit.Wireframe, false);
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GL gl = state.gl;
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] { 1.f, 1.f, 1.f }, 0);

        if (!initiated) {
            GLTextureHelper th = new GLTextureHelper();
            this.texture_id = th.genTextureID(gl);
            initiated = true;
        }
        GL3DFont.getSingletonInstance().loadFont(this.font, gl);

        super.shapeDraw(state);
    }

    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        RectangleDouble[] characters = GL3DFont.getSingletonInstance().getCharacters(this.font);
        double xStart = -this.width * this.text.length() / 2.0;
        double yStart = -this.height / 2.0;
        for (int i = 0; i < this.text.length(); i++) {
            int ch = (this.text.charAt(i));
            RectangleDouble rect =characters[ch];
            positions.add(new GL3DVec3d( x0 + xStart +  this.width * i    , y0 - yStart , z0));
            positions.add(new GL3DVec3d( x0 + xStart +  this.width * i    , y0 + yStart , z0));
            positions.add(new GL3DVec3d( x0 + xStart +  this.width * (i+1), y0 + yStart , z0));
            positions.add(new GL3DVec3d( x0 + xStart +  this.width * (i+1), y0 - yStart , z0));

            textCoords.add(new GL3DVec2d(rect.getX(), rect.getY() - rect.getHeight()));
            textCoords.add(new GL3DVec2d(rect.getX(), rect.getY()));
            textCoords.add(new GL3DVec2d(rect.getX() + rect.getWidth(), rect.getY()));
            textCoords.add(new GL3DVec2d(rect.getX() + rect.getWidth(), rect.getY() - rect.getHeight()));

            normals.add(new GL3DVec3d(0, 0, 1));


            int startIndexThisRow = 4 * i;
            indices.add(startIndexThisRow);
            indices.add(startIndexThisRow + 1);
            indices.add(startIndexThisRow + 2);

            indices.add(startIndexThisRow + 2);
            indices.add(startIndexThisRow + 3);
            indices.add(startIndexThisRow);
        }
        return GL3DMeshPrimitive.TRIANGLES;

    }
}