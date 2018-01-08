package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;

public class GLGrab {

    public final int w;
    public final int h;
    private FBObject fbo;
    private TextureAttachment fboTex;

    public GLGrab(int _w, int _h) {
        w = _w;
        h = _h;
    }

    private void init(GL2 gl) {
        fbo = new FBObject();
        fbo.init(gl, w, h, 0);
        fboTex = fbo.attachTexture2D(gl, 0, true, GL2.GL_LINEAR, GL2.GL_LINEAR, GL2.GL_CLAMP_TO_EDGE, GL2.GL_CLAMP_TO_EDGE);

        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.reset(gl, fbo.getWidth(), fbo.getHeight(), GLInfo.GLSAMPLES);
        fbo.unbind(gl);
    }

    public void dispose(GL2 gl) {
        if (fbo != null) {
            fbo.detachAll(gl);
            fbo.destroy(gl);
            fboTex.free(gl);
        }
    }

    public void renderFrame(Camera camera, GL2 gl, Buffer buffer) {
        if (fbo == null)
            init(gl);

        int _x = Displayer.fullViewport.x;
        int _y = Displayer.fullViewport.yGL;
        int _w = Displayer.fullViewport.width;
        int _h = Displayer.fullViewport.height;

        Displayer.setGLSize(0, 0, fbo.getWidth(), fbo.getHeight());
        Displayer.reshapeAll();

        fbo.bind(gl);
        if (Displayer.mode == Displayer.DisplayMode.Orthographic) {
            GLListener.renderScene(camera, gl);
        } else {
            GLListener.renderSceneScale(camera, gl);
        }
        GLListener.renderFloatScene(camera, gl);
        fbo.unbind(gl);

        fbo.use(gl, fboTex);

        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
        gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, buffer);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

        fbo.unuse(gl);

        Displayer.setGLSize(_x, _y, _w, _h);
        Displayer.reshapeAll();
    }

}
