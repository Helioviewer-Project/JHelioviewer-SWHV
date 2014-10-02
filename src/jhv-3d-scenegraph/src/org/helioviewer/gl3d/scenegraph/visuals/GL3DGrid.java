package org.helioviewer.gl3d.scenegraph.visuals;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

import com.jogamp.opengl.util.awt.TextRenderer;

public class GL3DGrid extends GL3DGroup {
    private final double lonstepDegrees;
    private final double latstepDegrees;
    private final GL3DVec4f color;
    private final GL3DVec4d textColor;
    private final int lineres = 120;
    private float scale = .8f;
    private Font font;
    private TextRenderer renderer;
    private final int fontsize = 20;

    public GL3DGrid(String name, double lonstepDegrees, double latstepDegrees, GL3DVec4f color, GL3DVec4d textColor) {
        super(name);
        this.lonstepDegrees = lonstepDegrees;
        this.latstepDegrees = latstepDegrees;
        this.color = color;
        this.textColor = textColor;
        InputStream is = FileUtils.getResourceInputStream("/fonts/DroidSans-Bold.ttf");
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(20.f);
        } catch (FontFormatException e) {
            Log.warn("Font Not loaded correctly, fallback to default");
            font = new Font("Serif", Font.PLAIN, fontsize);
        } catch (IOException e) {
            Log.warn("Font Not loaded correctly, fallback to default");
            font = new Font("Serif", Font.PLAIN, fontsize);
        } finally {
        }
        renderer = new TextRenderer(font, false, true);//, new CustomRenderDelegate(0, Color.WHITE));
        renderer.setUseVertexArrays(true);
        renderer.getSmoothing();
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
        float relhi = (float) ((-state.getActiveCamera().getZTranslation() - 1) / 10.f) / scale;
        float cfontsize = this.fontsize / relhi;
        cfontsize = cfontsize < 10.f ? 10.f : cfontsize;
        font = font.deriveFont(cfontsize);
        renderer = new TextRenderer(font, false, true);
        renderer.setUseVertexArrays(true);
        renderer.getSmoothing();
        drawText(gl);
        drawCircles(gl);

    }

    private void drawCircles(GL2 gl) {
        //latitude positive
        gl.glLineWidth(0.5f);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor3f(1f, .0f, .0f);
        gl.glDisable(GL2.GL_LIGHTING);
        double phi = Math.PI / 2.;
        double latstep = latstepDegrees / 180. * Math.PI;
        for (; phi < Math.PI; phi = phi + latstep) {
            //+ j * Math.PI / this.xticks;
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
        //latitude negative
        phi = Math.PI / 2. - latstep;
        for (; phi > 0.; phi = phi - latstep) {
            //+ j * Math.PI / this.xticks;
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

        //longitude positive
        double theta = 0.;
        double lonstep = lonstepDegrees / 180. * Math.PI;
        for (; theta <= Math.PI; theta = theta + lonstep) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i <= lineres; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(0.f, 1.0f, .0f);

                } else {
                    gl.glColor3f(1f, .0f, .0f);
                }
                phi = i * Math.PI / lineres;
                gl.glVertex3d(Math.sin(theta) * Math.sin(phi), Math.cos(phi), Math.cos(theta) * Math.sin(phi));
            }
            gl.glEnd();
        }

        //longitude negative
        theta = -lonstep;
        for (; theta >= -Math.PI; theta = theta - lonstep) {
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i <= lineres; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(0.f, 1.0f, .0f);
                } else {
                    gl.glColor3f(1f, .0f, .0f);
                }
                phi = i * Math.PI / lineres;
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
        for (double phi = 0; phi <= 90; phi = phi + this.latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = String.format("%.1f", phi);

            renderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            if (phi != 90) {
                renderer.draw3D(txt, (float) (-Math.sin(angle) * size - scale * 0.03f * txt.length() * 20. / font.getSize()), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            }
        }
        for (double phi = -this.latstepDegrees; phi >= -90; phi = phi - this.latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = String.format("%.1f", phi);
            renderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            if (phi != -90) {
                renderer.draw3D(txt, (float) (-Math.sin(angle) * size - scale * 0.03f * txt.length() * 20. / font.getSize()), (float) (Math.cos(angle) * size - scale * 0.02f * 20. / font.getSize()), (float) zdist, scale * 0.08f / font.getSize());
            }
        }
        renderer.end3DRendering();

        size = Constants.SunRadius * 1.02;

        for (double theta = 0; theta <= 180; theta = theta + this.lonstepDegrees) {
            String txt = String.format("%.1f", theta);
            double angle = (90 - theta) * Math.PI / 180.;
            renderer.begin3DRendering();
            gl.glPushMatrix();
            gl.glTranslatef((float) (Math.cos(angle) * size), 0f, (float) (Math.sin(angle) * size));
            gl.glRotated(theta, 0.f, 1.f, 0.f);
            renderer.draw3D(txt, 0.f, 0f, 0.f, scale * 0.08f / font.getSize());
            renderer.flush();
            renderer.end3DRendering();
            gl.glPopMatrix();
        }
        for (double theta = -this.lonstepDegrees; theta > -180.; theta = theta - this.lonstepDegrees) {
            String txt = String.format("%.1f", theta);
            double angle = (90 - theta) * Math.PI / 180.;
            renderer.begin3DRendering();
            gl.glPushMatrix();
            gl.glTranslatef((float) (Math.cos(angle) * size), 0f, (float) (Math.sin(angle) * size));
            gl.glRotated(theta, 0.f, 1.f, 0.f);
            renderer.draw3D(txt, 0.f, 0f, 0.f, scale * 0.08f / font.getSize());
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

    public void setFontScale(float scale) {
        this.scale = scale;
    }
}
