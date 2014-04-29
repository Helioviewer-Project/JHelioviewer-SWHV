package org.helioviewer.gl3d.scenegraph.visuals;

import java.util.List;

import javax.media.opengl.GL;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

public class GL3DText extends GL3DMesh {
    private double width;
    private double height;

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
        this.font = "default";
        this.textColor = textColor;
    }

    public GL3DText(double width, double height, int resX, int resY) {
        super("Rectangle");
        this.width = width;
        this.height = height;
        this.drawBits.set(Bit.Wireframe, false);
    }

    public void shapeDraw(GL3DState state) {
        GL gl = state.gl;
        if (!initiated) {
            GLTextureHelper th = new GLTextureHelper();
            this.texture_id = th.genTextureID(gl);
            initiated = true;
        }
        GL3DFont.getSingletonInstance().loadFont(this.font, gl);
        super.shapeDraw(state);
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        double xStart = -this.width * this.text.length() / 2.0;
        double yStart = -this.height / 2.0;
        for (int i = 0; i < this.text.length(); i++) {
            int ch = (int) (this.text.charAt(i));
            for (int r = 0; r <= 1; r++) {
                for (int c = 0; c <= 1; c++) {
                    positions.add(new GL3DVec3d((x0 + xStart + (c + i) * this.width), (y0 + yStart + (r) * this.height), z0));
                    textCoords.add(new GL3DVec2d((c + (ch % 16)) / 16.0, (1 - r + (ch / 16)) / 16.0));
                    normals.add(new GL3DVec3d(0, 0, 1));
                    colors.add(this.textColor);
                }
            }

            for (int r = 0; r < 1; r++) {
                for (int c = 0; c < 1; c++) {
                    int startIndexThisRow = r * 2 + c + 4 * i;
                    int startIndexNextRow = (r + 1) * 2 + c + 4 * i;
                    indices.add(startIndexThisRow);
                    indices.add(startIndexThisRow + 1);
                    indices.add(startIndexNextRow);

                    indices.add(startIndexThisRow + 1);
                    indices.add(startIndexNextRow + 1);
                    indices.add(startIndexNextRow);
                }
            }
        }
        return GL3DMeshPrimitive.TRIANGLES;
    }
}