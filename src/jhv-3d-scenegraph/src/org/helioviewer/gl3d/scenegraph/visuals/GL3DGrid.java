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
    private final int lineres = 120;

    private Font font;
    private TextRenderer renderer;
    private final int fontsize = 20;

    public GL3DGrid(String name, int xticks, int yticks, GL3DVec4f color, GL3DVec4d textColor) {
        super(name);
        this.xticks = xticks;
        this.yticks = yticks;
        this.color = color;
        this.textColor = textColor;
        InputStream is = FileUtils.getResourceInputStream("/fonts/DroidSans-Bold.ttf");
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(20.f);
        } catch (FontFormatException e) {
            System.out.println("Not loaded correctly");
            font = new Font("Serif", Font.PLAIN, fontsize);
        } catch (IOException e) {
            System.out.println("Not loaded correctly");
            font = new Font("Serif", Font.PLAIN, fontsize);
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
        font = font.deriveFont((float) (this.fontsize + (state.getActiveCamera().getZTranslation() + 15.) / 3.));
        renderer = new TextRenderer(font, false, true);
        renderer.setUseVertexArrays(true);
        renderer.getSmoothing();
        drawText(gl);
        drawCircles(gl);

    }

    private void drawCircles(GL2 gl) {

        gl.glLineWidth(0.5f);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor3f(1f, .0f, .0f);
        gl.glDisable(GL2.GL_LIGHTING);
        for (int j = 0; j <= this.yticks; j++) {
            double phi = j * Math.PI / this.yticks;
            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 0; i <= lineres; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(0.f, 1.0f, .0f);

                } else {
                    gl.glColor3f(1f, .0f, .0f);
                }
                double theta = 2 * i * Math.PI / lineres;
                gl.glVertex3d(Math.sin(theta) * Math.sin(phi), Math.cos(phi), Math.cos(theta) * Math.sin(phi));
            }
            gl.glEnd();
        }
        for (int j = 0; j <= this.xticks - 1; j++) {
            double theta = 2 * j * Math.PI / this.xticks;
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i <= lineres; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(0.f, 1.0f, .0f);

                } else {
                    gl.glColor3f(1f, .0f, .0f);
                }
                double phi = i * Math.PI / lineres;
                gl.glVertex3d(Math.sin(theta) * Math.sin(phi), Math.cos(phi), Math.cos(theta) * Math.sin(phi));
            }
            gl.glEnd();
        }
    }

    private void drawText(GL2 gl) {
        double size = Constants.SunRadius * 1.06;
        double zdist = 0.0;
        renderer.setColor(Color.WHITE);
        renderer.begin3DRendering();
        for (int i = 1; i < this.xticks; i++) {
            double angle = i * Math.PI / this.xticks;
            String txt = "" + (int) (90 - 1.0 * i / this.xticks * 180);
            renderer.draw3D(txt, (float) (Math.sin(angle) * size) - 0.f, (float) (Math.cos(angle) * size), (float) zdist, 0.08f / font.getSize());
            renderer.draw3D(txt, (float) (-Math.sin(angle) * size) - 0.03f * txt.length(), (float) (Math.cos(angle) * size), (float) zdist, 0.08f / font.getSize());
        }
        renderer.end3DRendering();

        size = Constants.SunRadius * 1.02;

        for (int i = 1; i < this.yticks; i++) {
            String txt = "" + (int) (90 - 1.0 * i / (this.yticks / 2.0) * 180);
            double angle = i * Math.PI / (this.yticks / 2.0);
            renderer.begin3DRendering();
            gl.glPushMatrix();
            gl.glTranslatef((float) (Math.cos(angle) * size), 0f, (float) (Math.sin(angle) * size));
            gl.glRotated(90 - angle / Math.PI * 180., 0.f, 1.f, 0.f);
            renderer.draw3D(txt, 0.f, 0f, 0.f, 0.08f / font.getSize());
            renderer.flush();
            renderer.end3DRendering();
            gl.glPopMatrix();
        }
    }

    public void setFont(String item) {
        font = new Font(item, Font.PLAIN, this.fontsize);
        renderer = new TextRenderer(font, false, true);//, new CustomRenderDelegate(0, Color.WHITE));
        renderer.setUseVertexArrays(true);
        renderer.getSmoothing();
    }
}
