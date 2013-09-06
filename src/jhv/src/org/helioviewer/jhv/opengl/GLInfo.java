package org.helioviewer.jhv.opengl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.jp2view.J2KRenderGlobalOptions;

/**
 * Class to check and manage some OpenGL properties.
 * 
 * @author Markus Langenberg
 * @author Andre Dau
 * 
 */
public class GLInfo {

    // data
    private static boolean glUsable = true;
    private static boolean glActivated = true;
    private static boolean glInitiated = false;
    private static String version;

    private GLInfo() {
    }

    /**
     * Updates the OpenGL settings by reading the OpenGL properties.
     * 
     * By default, activates OpenGL if possible.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @see #setGlEnabled(boolean)
     */
    public static void update(GL gl) {
        version = gl.glGetString(GL.GL_VERSION);
        Log.debug(">> GLInfo.update(GL) > Version string: " + version);
        Matcher versionMatcher = Pattern.compile("\\d+(\\.(\\d+))*").matcher(version);
        if (!versionMatcher.find()) {
            Log.error(">> GLInfo.update(GL) > Could not strip version from version string. Display complete version in status bar");
        } else {
            version = versionMatcher.group();
        }

        String extensionStr = gl.glGetString(GL.GL_EXTENSIONS);
        Log.debug(">> GLInfo.update(GL) > Extensions: " + extensionStr);

        glUsable = true;
        if (!gl.isExtensionAvailable("GL_VERSION_1_3")) {
            Log.error(">> GLInfo.update(GL) > OpenGL 1.3 not supported. JHelioviewer will run in software mode.");
            glUsable = false;
        }

        if (!gl.isExtensionAvailable("GL_ARB_multitexture")) {
            Log.error(">> GLInfo.update(GL) > GL_ARB_multitexture extension not supported. JHelioviewer will run in software mode.");
            glUsable = false;
        }

        if (!gl.isExtensionAvailable("GL_ARB_fragment_program")) {
            Log.error(">> GLInfo.update(GL) > GL_ARB_fragment_program extension not supported. JHelioviewer will run in software mode.");
            glUsable = false;
        } else {
            int[] out = new int[1];
            gl.glGetProgramivARB(GL.GL_VERTEX_PROGRAM_ARB, GL.GL_MAX_PROGRAM_INSTRUCTIONS_ARB, out, 0);
            Log.debug(">> GLInfo.update(GL) > GL_MAX_PROGRAM_INSTRUCTIONS = " + out[0]);
            gl.glGetProgramivARB(GL.GL_VERTEX_PROGRAM_ARB, GL.GL_MAX_PROGRAM_PARAMETERS_ARB, out, 0);
            Log.debug(">> GLInfo.update(GL) > GL_MAX_PROGRAM_PARAMETERS   = " + out[0]);
            gl.glGetProgramivARB(GL.GL_VERTEX_PROGRAM_ARB, GL.GL_MAX_PROGRAM_TEMPORARIES_ARB, out, 0);
            Log.debug(">> GLInfo.update(GL) > GL_MAX_PROGRAM_TEMPORARIES   = " + out[0]);

            gl.glGetIntegerv(GL.GL_MAX_DRAW_BUFFERS_ARB, out, 0);
            Log.debug(">> GLInfo.update(GL) > GL_MAX_DRAW_BUFFERS = " + out[0]);

        }

        if (!glUsable && glActivated)
            glActivated = false;

        J2KRenderGlobalOptions.setDoubleBufferingOption(glActivated);

        Log.debug(">> GLInfo.update(GL) > GL usable: " + glUsable + " - GL activated: " + glActivated);
        glInitiated = true;
    }

    /**
     * Returns, whether OpenGL is activated.
     * 
     * OpenGL can be deactivated for two reasons: The machine satisfies the
     * minimal requirements of the user switched it of manually.
     * 
     * @return true, if OpenGL is activated, false otherwise
     * @see #setGlEnabled(boolean)
     */
    public static boolean glIsEnabled() {
        return glActivated;
    }

    /**
     * Returns, whether OpenGL is usable.
     * 
     * OpenGL may not be usable if the machine does not satisfy the minimal
     * requirements.
     * 
     * @return true, if OpenGL is usable, false otherwise
     * @see #glIsUsable
     */
    public static boolean glIsUsable() {
        return glUsable;
    }

    /**
     * Sets openGL unusable after reporting some error
     * 
     * @see #glIsUsable
     */
    public static void glUnusable() {
        glUsable = false;
        setGlEnabled(false);
    }

    /**
     * Manually switches OpenGL on or off.
     * 
     * If the machine does not satisfy the minimal requirements for using
     * OpenGL, this functions does nothing.
     * 
     * @param enabled
     *            true to use OpenGL, false otherwise
     */
    public static void setGlEnabled(boolean enabled) {
        boolean before = glActivated;

        if (glUsable) {
            glActivated = enabled;
        } else {
            glActivated = false;
        }
        J2KRenderGlobalOptions.setDoubleBufferingOption(glActivated);

        if (before != glActivated && (!glActivated || glInitiated)) {
            if (ImageViewerGui.getSingletonInstance().viewchainCreated()) {
                ImageViewerGui.getSingletonInstance().createViewchains();
            }
        }
    }

    /**
     * Returns the OpenGL version available on this machine.
     * 
     * @return OpenGL version
     */
    public static String getVersion() {
        return version;
    }
}
