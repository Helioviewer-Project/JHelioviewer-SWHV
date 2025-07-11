package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL3;

public class GLGrab {

    public final int w;
    public final int h;
    private FBObject fbo;
    private TextureAttachment fboTex;

    public GLGrab(int _w, int _h) {
        w = _w;
        h = _h;
    }

    private void init(GL3 gl) {
        fbo = new FBObject();
        fbo.init(gl, w, h, 0);
        fboTex = fbo.attachTexture2D(gl, 0, true, GL3.GL_LINEAR, GL3.GL_LINEAR, GL3.GL_CLAMP_TO_EDGE, GL3.GL_CLAMP_TO_EDGE);

        fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.reset(gl, fbo.getWidth(), fbo.getHeight(), GLInfo.GLSAMPLES);
        fbo.unbind(gl);
    }

    public void dispose(GL3 gl) {
        if (fbo != null) {
            fbo.detachAll(gl);
            fbo.destroy(gl);
            fboTex.free(gl);
        }
    }

    public void renderFrame(Camera camera, GL3 gl, Buffer buffer) {
        if (fbo == null)
            init(gl);

        int _x = Display.fullViewport.x;
        int _y = Display.fullViewport.yGL;
        int _w = Display.fullViewport.width;
        int _h = Display.fullViewport.height;

        Display.setGLSize(0, 0, fbo.getWidth(), fbo.getHeight());
        Display.reshapeAll();

        fbo.bind(gl);
        if (Display.mode == Display.ProjectionMode.Orthographic) {
            GLListener.renderScene(camera, gl);
        } else {
            GLListener.renderSceneScale(camera, gl);
        }
        fbo.unbind(gl);

        fbo.use(gl, fboTex);

        gl.glBindFramebuffer(GL3.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
        gl.glPixelStorei(GL3.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL3.GL_BGR, GL3.GL_UNSIGNED_BYTE, buffer);
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);

        fbo.unuse(gl);

        Display.setGLSize(_x, _y, _w, _h);
        Display.reshapeAll();
    }

}
