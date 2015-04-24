package org.helioviewer.viewmodel.view.opengl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.view.jp2view.J2KRenderGlobalOptions;

/**
 * Class to check and manage some OpenGL properties.
 *
 * @author Markus Langenberg
 * @author Andre Dau
 *
 */
public class GLInfo {

    /*
     * TBD
     *
     * - pixel scale at startup
     *
     * if (!GLInfo.glIsUsable()) { Message.err("Could not initialize OpenGL",
     * "OpenGL could not be initialized properly during startup. JHelioviewer
     * will start in Software Mode. For detailed information please read the log
     * output. ", false); }
     */

    public static int maxTextureSize;
    public static int[] pixelScale = new int[] { 1, 1 };

    /**
     * Updates the OpenGL settings by reading the OpenGL properties.
     *
     */
    public static void update(GLCanvas canvas) {
        final GL2 gl = canvas.getGL().getGL2();

        String version = gl.glGetString(GL2.GL_VERSION);
        Log.debug(">> GLInfo > Version string: " + version);
        Matcher versionMatcher = Pattern.compile("\\d+(\\.(\\d+))*").matcher(version);
        if (!versionMatcher.find()) {
            Log.error(">> GLInfo > Could not strip version from version string. Display complete version in status bar");
        } else {
            version = versionMatcher.group();
        }

        String extensionStr = gl.glGetString(GL2.GL_EXTENSIONS);
        Log.debug(">> GLInfo.update(GL) > Extensions: " + extensionStr);

        boolean glUsable = true;
        if (!gl.isExtensionAvailable("GL_VERSION_1_3")) {
            Log.error(">> GLInfo.update(GL) > OpenGL 1.3 not supported. JHelioviewer will run in software mode.");
            glUsable = false;
        }

        if (glUsable == true) {
            int[] out = new int[1];

            out[0] = 0;
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, out, 0);
            maxTextureSize = out[0];
            Log.debug(">> GLInfo > max texture size: " + out[0]);

            pixelScale = canvas.getCurrentSurfaceScale(pixelScale);

            J2KRenderGlobalOptions.setDoubleBufferingOption(true);
        }
    }

    public static boolean checkGLErrors(GL2 gl, String message) {
        GLU glu = new GLU();
        int glErrorCode, errors = 0;

        while ((glErrorCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode) + " - @" + message);
            errors++;
        }

        if (errors != 0)
            return true;
        else
            return false;
    }

}
