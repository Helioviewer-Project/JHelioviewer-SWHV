package org.helioviewer.gl3d.scenegraph.visuals;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

import com.jogamp.opengl.util.awt.TextRenderer;

public class GL3DGrid extends GL3DGroup {
    private final int xticks;
    private final int yticks;
    private final GL3DVec4f color;
    private final GL3DVec4d textColor;
    private final int lineres = 100;

    private Font font;
    private final TextRenderer renderer;

    public GL3DGrid(String name, int xticks, int yticks, GL3DVec4f color, GL3DVec4d textColor) {
        super(name);
        this.xticks = xticks;
        this.yticks = yticks;
        this.color = color;
        this.textColor = textColor;
        InputStream is = FileUtils.getResourceInputStream("/fonts/DroidSans-Bold.ttf");
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException e) {
            System.out.println("Not loaded correctly");
            font = new Font("Serif", Font.PLAIN, 40);
        } catch (IOException e) {
            System.out.println("Not loaded correctly");
            font = new Font("Serif", Font.PLAIN, 40);
        } finally {
        }
        renderer = new TextRenderer(font, false, true);//, new CustomRenderDelegate(0, Color.WHITE));
        renderer.setUseVertexArrays(true);
        renderer.getSmoothing();
    }

    private void loadGrid() {
        GL3DSphere sphere = new GL3DSphere(Constants.SunRadius * 1.02, this.xticks, this.yticks, this.color);
        sphere.getDrawBits().on(Bit.Wireframe);
        this.addNode(sphere);
    }

    @Override
    public void update(GL3DState state) {
        if (!this.isInitialised) {
            this.init(state);
        }
        state.pushMV();
        GL3DQuatd differentialRotation = state.getActiveCamera().getLocalRotation();
        this.m = differentialRotation.toMatrix().inverse();
        this.wm = (this.m);
        state.buildInverseAndNormalMatrix();
        this.wmI = new GL3DMat4d(state.getMVInverse());
        this.shapeUpdate(state);
        state.popMV();
    }

    @Override
    public void shapeDraw(GL3DState state) {
        this.markAsChanged();
        state.gl.glColor3d(1., 1., 0.);
        GL2 gl = state.gl;
        super.shapeDraw(state);
        double size = Constants.SunRadius * 1.12;
        double zdist = Constants.SunRadius * 0.0;
        renderer.setColor(Color.WHITE);
        renderer.begin3DRendering();
        for (int i = 1; i < this.xticks; i++) {
            double angle = i * Math.PI / this.xticks;
            String txt = "" + (int) (90 - 1.0 * i / this.xticks * 180);
            renderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size), (float) zdist, 0.002f);
            renderer.draw3D(txt, (float) (-Math.sin(angle) * size), (float) (Math.cos(angle) * size), (float) zdist, 0.002f);
        }
        for (int i = 1; i < this.yticks; i++) {
            String txt = "" + (int) (90 - 1.0 * i / (this.yticks / 2.0) * 180);
            double angle = i * Math.PI / (this.yticks / 2.0);
            renderer.draw3D(txt, (float) (Math.cos(angle) * size), 0f, (float) (Math.sin(angle) * size), 0.002f);
        }
        renderer.flush();
        renderer.end3DRendering();
        final double deg_to_rad = Math.PI / 180;
        gl.glColor3f(1f, .0f, .0f);
        gl.glDisable(GL2.GL_LIGHTING);
        for (int j = 0; j <= this.yticks; j++) {
            double phi = j * Math.PI / this.yticks;
            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 0; i <= lineres; i++) {
                double theta = 2 * i * Math.PI / lineres;
                gl.glVertex3d(Math.sin(theta) * Math.sin(phi), Math.cos(phi), Math.cos(theta) * Math.sin(phi));
            }
            gl.glEnd();
        }
        for (int j = 0; j <= this.xticks - 1; j++) {
            double theta = 2 * j * Math.PI / this.yticks;
            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 0; i <= lineres; i++) {
                double phi = i * Math.PI / lineres;
                gl.glVertex3d(Math.sin(theta) * Math.sin(phi), Math.cos(phi), Math.cos(theta) * Math.sin(phi));
            }
            gl.glEnd();
        }
    }
}
