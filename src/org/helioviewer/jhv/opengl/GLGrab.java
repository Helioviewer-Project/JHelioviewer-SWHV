package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;

public class GLGrab {

    private static int w;
    private static int h;

    public static void attach(int _w, int _h) {
        w = _w;
        h = _h;
    }

    public static int getWidth() {
        return w;
    }

    public static int getHeight() {
        return h;
    }

    public static void renderFrame(Camera camera, GL2 gl, Buffer buffer) {
        FBObject fbo = new FBObject();
        fbo.init(gl, w, h, 0);
        TextureAttachment fboTex = fbo.attachTexture2D(gl, 0, true, GL2.GL_LINEAR, GL2.GL_LINEAR, GL2.GL_CLAMP_TO_EDGE, GL2.GL_CLAMP_TO_EDGE);

        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.reset(gl, fbo.getWidth(), fbo.getHeight(), GLInfo.GLSAMPLES);

        int _x = Display.fullViewport.x;
        int _y = Display.fullViewport.yGL;
        int _w = Display.fullViewport.width;
        int _h = Display.fullViewport.height;

        Display.setGLSize(0, 0, fbo.getWidth(), fbo.getHeight());
        Display.reshapeAll();

        fbo.bind(gl);
        if (Display.mode == Display.DisplayMode.Orthographic) {
            GLListener.renderScene(camera, gl);
        } else {
            GLListener.renderSceneScale(camera, gl);
        }
        fbo.unbind(gl);

        fbo.use(gl, fboTex);

        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
        gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, buffer);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

        fbo.unuse(gl);
        fbo.detachAll(gl);
        fbo.destroy(gl);
        fboTex.free(gl);

        Display.setGLSize(_x, _y, _w, _h);
        Display.reshapeAll();
    }

}
