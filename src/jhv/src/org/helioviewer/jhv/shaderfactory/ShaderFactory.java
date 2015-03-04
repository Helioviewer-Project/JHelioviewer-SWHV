package org.helioviewer.jhv.shaderfactory;

import java.io.IOException;
import java.io.InputStream;

import javax.media.opengl.GL2;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
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
        if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D) {
            return fragment3dCGID;
        } else {
            return fragment2dCGID;
        }
    }

    public static int getVertexId() {
        if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D) {
            return vertex3dCGID;
        } else {
            return vertex2dCGID;
        }
    }

    public static void initShader(GL2 gl) {
        if (!init) {
            init = true;
            fragment3dCGID = genShaderID(gl);
            compileShader(gl, "/data/fragment3d.cg", fragment3dCGID, GL2.GL_FRAGMENT_PROGRAM_ARB);
            vertex3dCGID = genShaderID(gl);
            compileShader(gl, "/data/vertex3d.cg", vertex3dCGID, GL2.GL_VERTEX_PROGRAM_ARB);
            fragment2dCGID = genShaderID(gl);
            compileShader(gl, "/data/fragment2d.cg", fragment2dCGID, GL2.GL_FRAGMENT_PROGRAM_ARB);
            vertex2dCGID = genShaderID(gl);
            compileShader(gl, "/data/vertex2d.cg", vertex2dCGID, GL2.GL_VERTEX_PROGRAM_ARB);
        }
    }

    public static int genShaderID(GL2 gl) {
        int[] tmp = new int[1];
        gl.glGenProgramsARB(1, tmp, 0);
        int target = tmp[0];
        return target;
    }

    public static void compileShader(GL2 gl, String inputFile, int target, int type) {
        InputStream fragmentStream = FileUtils.getResourceInputStream(inputFile);
        String fragmentText = FileUtils.convertStreamToString(fragmentStream);
        try {
            fragmentStream.close();
        } catch (IOException e) {
            Log.debug("Stream refuses to close" + e);
        }
        GLShaderHelper shaderHelper = new GLShaderHelper();
        shaderHelper.compileProgram(gl, type, fragmentText, target);
    }

    public static void bindEnvVars(GL2 gl, int target, int id, double[] param) {
        gl.glProgramLocalParameter4dARB(target, id, param[0], param[1], param[2], param[3]);
    }

}
