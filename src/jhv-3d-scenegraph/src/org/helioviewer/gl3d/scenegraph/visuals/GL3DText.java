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
    private final double[] x0;
    private final double[] y0;
    private final double[] z0;
    private final String font;
    private final Color textColor;

    public GL3DText(double height, double []x0, double []y0, double []z0, String[] text, String font, Color textColor) {
        super("GL3DText");
        this.height = height;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.text = text;
        this.font = font;
        this.textColor = textColor;
    }

    public GL3DText(double height, double [] x0, double [] y0, double [] z0, String[] text, Color textColor) {
        super("GL3DText");
        this.height = height;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.text = text;
        this.font = "Serif";
        this.textColor = textColor;
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GL gl = state.gl;
        gl.glDisable ( GL.GL_COLOR_MATERIAL ) ;
        gl.glDisable(GL.GL_LIGHT0);

        //gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] { 1.f, 1.f, 1.f, 1.f }, 0);
        gl.glDisable(GL.GL_LIGHTING);
        state.gl.glDisable(GL.GL_BLEND);
        //state.gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE_MINUS_SRC_COLOR);
        if (!initiated) {
            GLTextureHelper th = new GLTextureHelper();
            this.texture_id = th.genTextureID(gl);
            initiated = true;
        }
        GL3DFont.getSingletonInstance().loadFont(this.font, gl);

        super.shapeDraw(state);
        gl.glEnable(GL.GL_LIGHT0);

    }



    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        RectangleDouble[] characters = GL3DFont.getSingletonInstance().getCharacters(this.font);
        double fontHeight = characters[0].getHeight();
        int counter = 0;

        for (int k=0; k<this.x0.length; k++) {
            double totalWidth = 0;
            for(int i=0; i<this.text[k].length();i++){
                int ch = (this.text[k].charAt(i));
                RectangleDouble rect =characters[ch];
                double width = rect.getWidth();
                totalWidth += width;
            }
            double scaledTotalWidth = totalWidth * this.height/fontHeight;
            double xStart = -scaledTotalWidth / 2.0;
            double yStart = -this.height / 2.0;

            double adaptedScaledTotalWidth =0;
            for (int i = 0; i < this.text[k].length(); i++) {
                int ch = (this.text[k].charAt(i));
                RectangleDouble rect =characters[ch];
                positions.add(new GL3DVec3d( x0[k] + xStart +  adaptedScaledTotalWidth, y0[k] - yStart , z0[k]));
                positions.add(new GL3DVec3d( x0[k] + xStart +  adaptedScaledTotalWidth, y0[k] + yStart , z0[k]));
                adaptedScaledTotalWidth += 1.* characters[ch].getWidth() * this.height / characters[ch].getHeight();
                positions.add(new GL3DVec3d( x0[k] + xStart +  adaptedScaledTotalWidth, y0[k] + yStart , z0[k]));
                positions.add(new GL3DVec3d( x0[k] + xStart +  adaptedScaledTotalWidth, y0[k] - yStart , z0[k]));

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
                counter ++;
            }
        }
        System.out.println("END");
        return GL3DMeshPrimitive.TRIANGLES;

    }
}