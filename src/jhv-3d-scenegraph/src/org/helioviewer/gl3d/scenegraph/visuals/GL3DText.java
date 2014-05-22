package org.helioviewer.gl3d.scenegraph.visuals;

import java.awt.Color;
import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

public class GL3DText extends GL3DMesh {
    private final double height;

    private boolean initiated = false;
    private int texture_id;
    private final String[] text;
    private final GL3DVec3d[] position3D;
    private final String font;
    private Color textColor;
    private Color backgroundColor;

    public GL3DText(double height, GL3DVec3d[] position3D, String[] text, String font, Color textColor, Color backgroundColor) {
        super("GL3DText");
        this.height = height;
        this.position3D = position3D;
        this.text = text;
        this.font = font;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        GL3DFont.getSingletonInstance().updateFont(this.font, state.gl, this.textColor, this.backgroundColor);
        this.setUnchanged();
    }

    @Override
    public void shapeInit(GL3DState state) {
        super.shapeInit(state);
        GL3DFont.getSingletonInstance().updateFont(this.font, state.gl, this.textColor, this.backgroundColor);
        this.setUnchanged();
        this.markAsChanged();
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GL gl = state.gl;
        gl.glDisable(GL.GL_COLOR_MATERIAL);
        gl.glDisable(GL.GL_LIGHT0);

        gl.glDisable(GL.GL_LIGHTING);
        state.gl.glEnable(GL.GL_BLEND);
        if (!initiated) {
            GLTextureHelper th = new GLTextureHelper();
            this.texture_id = th.genTextureID(gl);
            initiated = true;
        }
        GL3DFont.getSingletonInstance().bindFont(this.font, gl);
        super.shapeDraw(state);
        gl.glEnable(GL.GL_LIGHT0);
    }

    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        RectangleDouble[] characters = GL3DFont.getSingletonInstance().getCharacters(this.font, this.textColor, this.backgroundColor);
        double fontHeight = characters[0].getHeight();
        int counter = 0;

        for (int k = 0; k < this.position3D.length; k++) {
            double totalWidth = 0;
            for (int i = 0; i < this.text[k].length(); i++) {
                int ch = (this.text[k].charAt(i));
                RectangleDouble rect = characters[ch];
                double width = rect.getWidth();
                totalWidth += width;
            }
            double scaledTotalWidth = totalWidth * this.height / fontHeight;
            double xStart = -scaledTotalWidth / 2.0;
            double yStart = -this.height / 2.0;

            double adaptedScaledTotalWidth = 0;
            for (int i = 0; i < this.text[k].length(); i++) {
                int ch = (this.text[k].charAt(i));
                RectangleDouble rect = characters[ch];
                positions.add(new GL3DVec3d(position3D[k].x + xStart + adaptedScaledTotalWidth, position3D[k].y - yStart, position3D[k].z));
                positions.add(new GL3DVec3d(position3D[k].x + xStart + adaptedScaledTotalWidth, position3D[k].y + yStart, position3D[k].z));
                adaptedScaledTotalWidth += 1. * characters[ch].getWidth() * this.height / characters[ch].getHeight();
                positions.add(new GL3DVec3d(position3D[k].x + xStart + adaptedScaledTotalWidth, position3D[k].y + yStart, position3D[k].z));
                positions.add(new GL3DVec3d(position3D[k].x + xStart + adaptedScaledTotalWidth, position3D[k].y - yStart, position3D[k].z));

                textCoords.add(new GL3DVec2d(rect.getX(), rect.getY() - rect.getHeight()));
                textCoords.add(new GL3DVec2d(rect.getX(), rect.getY()));
                textCoords.add(new GL3DVec2d(rect.getX() + rect.getWidth(), rect.getY()));
                textCoords.add(new GL3DVec2d(rect.getX() + rect.getWidth(), rect.getY() - rect.getHeight()));

                int startIndexThisRow = 4 * counter;
                indices.add(startIndexThisRow);
                indices.add(startIndexThisRow + 1);
                indices.add(startIndexThisRow + 2);

                indices.add(startIndexThisRow + 2);
                indices.add(startIndexThisRow + 3);
                indices.add(startIndexThisRow);
                counter++;
            }
        }
        System.out.println("END");
        return GL3DMeshPrimitive.TRIANGLES;

    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}