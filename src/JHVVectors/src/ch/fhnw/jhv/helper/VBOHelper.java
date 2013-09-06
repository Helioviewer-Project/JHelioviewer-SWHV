package ch.fhnw.jhv.helper;

import javax.media.opengl.GL;

/**
 * Helper Class for VBO related stuff
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VBOHelper {

    /**
     * Check if the computer supports VBO's
     * 
     * @param gl
     *            GL
     */
    public static void checkVBOSupport(GL gl) {

        // Check version.
        String versionStr = gl.glGetString(GL.GL_VERSION);
        System.out.println("GL version:" + versionStr);
        versionStr = versionStr.substring(0, 4);
        float version = new Float(versionStr).floatValue();
        boolean versionOK = (version >= 1.59f) ? true : false;
        System.out.println("GL version:" + versionStr + "  ->" + versionOK);

        // Check if extension is available.
        boolean extensionOK = gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
        System.out.println("VBO extension: " + extensionOK);
        // Check for VBO functions.
        boolean functionsOK = gl.isFunctionAvailable("glGenBuffersARB") && gl.isFunctionAvailable("glBindBufferARB") && gl.isFunctionAvailable("glBufferDataARB") && gl.isFunctionAvailable("glDeleteBuffersARB");

        System.out.println("is glGenBufferARB enabled: " + gl.isFunctionAvailable("glGenBuffersARB"));
        System.out.println("is glBindBufferARB enabled: " + gl.isFunctionAvailable("glBindBufferARB"));
        System.out.println("is glBufferDataARB enabled: " + gl.isFunctionAvailable("glBufferDataARB"));
        System.out.println("is glDeleteBuffersARB enabled: " + gl.isFunctionAvailable("glDeleteBuffersARB"));

        System.out.println("All functions ok: " + functionsOK);

        if (!extensionOK || !functionsOK) {
            // VBO not supported.
            System.out.println("VBOs not supported.");
            return;
        }
    }
}
