package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.opengl.GLSharedContext;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;

import com.sun.opengl.util.Animator;

/**
 * Component to initialize OpenGL.
 * 
 * <p>
 * The only purpose of this component is to be visible for one moment during the
 * startup sequence of JHV. It calls {@link GLInfo#update(GL)} to get the
 * application running and initializes some OpenGL classes.
 * 
 * @author Markus Langenberg
 * 
 */
public class GLInitPanel extends GLCanvas {

    private static final long serialVersionUID = 1L;
    private Animator animator;

    /**
     * Default constructor
     */
    public GLInitPanel() {
        super(null, null, GLSharedContext.getSharedContext(), null);

        Thread.setDefaultUncaughtExceptionHandler(new GLUncaughtExceptionHandlerDecorator(Thread.getDefaultUncaughtExceptionHandler()));

        GLListener listener = new GLListener(this);
        addGLEventListener(listener);

        animator = new Animator(this);
        animator.start();

        setPreferredSize(new Dimension(1, 1));
    }

    /**
     * This function is called after OpenGL is initialzed. It disposes itself,
     * since the component will not be used any more.
     */
    private void postInit() {
        animator.stop();
        setVisible(false);
    }

    /**
     * Starts the view chain creation in a separate thread
     */
    public static void startViewChainThread() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                ImageViewerGui.getSingletonInstance().createViewchains();
            }
        }, "CreateViewChainsThread");
        thread.start();
    }

    /**
     * Implementation GLEventHandler to startup OpenGL. It implements all
     * listener functions.
     */
    private class GLListener implements GLEventListener {
        private GLInitPanel parent;

        GLListener(GLInitPanel _parent) {
            super();
            parent = _parent;
        }

        public void display(GLAutoDrawable gLDrawable) {
            final GL gl = gLDrawable.getGL();

            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        }

        public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        }

        public void init(GLAutoDrawable drawable) {
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Dispose GLInitPanel: Stop the animator and make it invisible");
            parent.postInit();

            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Set GL properties");
            final GL gl = drawable.getGL();

            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Shade model: flat");
            gl.glShadeModel(GL.GL_FLAT);
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Clear color: black");
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Enable 1D texture");
            gl.glEnable(GL.GL_TEXTURE_1D);
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Enable 2D texture");
            gl.glEnable(GL.GL_TEXTURE_2D);

            GLInfo.update(gl);

            if (GLInfo.glIsUsable()) {
                GLShaderHelper.initHelper(gl, JHVDirectory.TEMP.getPath());
                GLShaderBuilder.initShaderBuilder(gl);
                // GLTextureHelper.setTextureNonPowerOfTwo(gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two"));
                GLTextureHelper.setTextureNonPowerOfTwo(false);
            } else {
                Message.err("Could not initialize OpenGL", "OpenGL could not be initialized properly during startup. JHelioviewer will start in software mode. OpenGL is not available on the system or incompatible.", false);
            }
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Start viewchain creation");
            startViewChainThread();
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        }
    }

    /**
     * UncaughtExceptionHandler to detect OpenGL exceptions.
     * 
     * All other exceptions are passed to the decorated handler. In case an
     * OpenGL exception is detected, it is assumed that OpenGL is in some way
     * broken. Thus, it is disabled as a precaution. When this happens, this
     * decorator is removed, since it is impossible that an OpenGL exception can
     * appear again.
     * 
     * @author Markus Langenberg
     */
    private class GLUncaughtExceptionHandlerDecorator implements UncaughtExceptionHandler {
        private final UncaughtExceptionHandler baseHandler;

        /**
         * Default constructor
         * 
         * @param handler
         *            Decorated exception handler.
         */
        public GLUncaughtExceptionHandlerDecorator(UncaughtExceptionHandler handler) {
            baseHandler = handler;
        }

        /**
         * {@inheritDoc}
         * 
         * In case an OpenGL exception is detected, a warning is logged, OpenGL
         * is disabled. Thus, the view chain has to be reseted. Also, this
         * decorator is removed.
         */
        public void uncaughtException(Thread t, Throwable e) {
            if (e instanceof GLException) {
                Throwable cause = e.getCause();
                if (cause == null || cause instanceof GLException) {
                    e.printStackTrace();
                    Log.warn("GLExpection detected. Disable OpenGL as a precaution.", cause);
                    Message.err("OpenGL error detected.", "JHelioviewer will run in software mode. OpenGL is not available on the system or incompatible.", false);
                    GLInfo.glUnusable();
                    Thread.setDefaultUncaughtExceptionHandler(baseHandler);
                } else {
                    baseHandler.uncaughtException(t, cause);
                }
            } else {
                baseHandler.uncaughtException(t, e);
            }
        }
    }
}
