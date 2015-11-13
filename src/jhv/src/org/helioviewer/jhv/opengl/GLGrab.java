package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.MainComponent;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;

public class GLGrab {

    private final FBObject fbo = new FBObject();
    private TextureAttachment fboTex;
    private int w;
    private int h;

    public GLGrab(int _w, int _h) {
        w = _w;
        h = _h;
    }

    public void init(GL2 gl) {
        fbo.init(gl, w, h, 0);
        fboTex = fbo.attachTexture2D(gl, 0, true);

        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.unbind(gl);
    }

    public void dispose(GL2 gl) {
        fbo.detachAll(gl);
        fbo.destroy(gl);
    }

    public BufferedImage renderFrame(GL2 gl) {
        BufferedImage screenshot;

        int _w = Displayer.getGLWidth();
        int _h = Displayer.getGLHeight();

        GLHelper.unitScale = true;
        Displayer.setGLSize(fbo.getWidth(), fbo.getHeight());
        Displayer.reshapeAll();
        {
            fbo.bind(gl);
            Camera camera = Displayer.getCamera();
            MainComponent.renderScene(camera, gl);
            MainComponent.renderFloatScene(camera, gl);
            fbo.unbind(gl);

            fbo.use(gl, fboTex);

            screenshot = new BufferedImage(fbo.getWidth(), fbo.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] array = ((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData();
            ByteBuffer fb = ByteBuffer.wrap(array);

            gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
            gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
            gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, fb);
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

            fbo.unuse(gl);
        }
        Displayer.setGLSize(_w, _h);
        Displayer.reshapeAll();
        GLHelper.unitScale = false;

        return screenshot;
    }

}
