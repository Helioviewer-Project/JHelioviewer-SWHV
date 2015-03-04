package org.helioviewer.jhv.shaderfactory;

import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;

public class ShaderFactory {
    private static boolean init = false;
    private static int fragment3dCGID = -1;
    private static int fragment2dCGID = -1;
    private static int vertex2dCGID = -1;
    private static int vertex3dCGID = -1;

    public ShaderFactory() {
    }

    public static int getFragmentId() {
        if (Displayer.getSingletonInstance().getState() == Displayer.STATE3D) {
            return fragment3dCGID;
        } else {
            return fragment2dCGID;
        }
    }

    public static void initShader(GL2 gl) {
        if (!init) {
            initFragmentShader3d(gl);
            initFragmentShader2d(gl);
        }
    }

    public static void initFragmentShader3d(GL2 gl) {
        InputStream fragmentStream = FileUtils.getResourceInputStream("/data/fragment3d.cg");
        String fragmentText = FileUtils.convertStreamToString(fragmentStream);
        try {
            fragmentStream.close();
        } catch (IOException e) {
            Log.debug("Stream refuses to close" + e);
        }
        int[] tmp = new int[1];
        gl.glGenProgramsARB(1, tmp, 0);
        fragment3dCGID = tmp[0];
        GLShaderHelper shaderHelper = new GLShaderHelper();
        shaderHelper.compileProgram(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, fragmentText, fragment3dCGID);
    }

    public static void initFragmentShader2d(GL2 gl) {
        InputStream fragmentStream = FileUtils.getResourceInputStream("/data/fragment3d.cg");
        String fragmentText = FileUtils.convertStreamToString(fragmentStream);
        try {
            fragmentStream.close();
        } catch (IOException e) {
            Log.debug("Stream refuses to close" + e);
        }
        int[] tmp = new int[1];
        gl.glGenProgramsARB(1, tmp, 0);
        fragment2dCGID = tmp[0];
        GLShaderHelper shaderHelper = new GLShaderHelper();
        shaderHelper.compileProgram(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, fragmentText, fragment2dCGID);
    }
}
