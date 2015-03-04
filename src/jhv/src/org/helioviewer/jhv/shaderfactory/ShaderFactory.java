package org.helioviewer.jhv.shaderfactory;

import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;

public class ShaderFactory {
    private static int fragment3dCGID = -1;

    public ShaderFactory() {
    }

    public static int getFragment3dCGId() {
        return fragment3dCGID;
    }

    public void initFragment3dCG(GL2 gl) {
        if (fragment3dCGID == -1) {
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
    }
}
