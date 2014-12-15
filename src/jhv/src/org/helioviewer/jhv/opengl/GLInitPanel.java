package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.awt.GLJPanel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;

/**
 * Component to initialize OpenGL2.
 *
 * <p>
 * The only purpose of this component is to be visible for one moment during the
 * startup sequence of JHV. It calls {@link GLInfo#update(GL2)} to get the
 * application running and initializes some OpenGL classes.
 *
 * @author Markus Langenberg
 *
 */
public class GLInitPanel extends GLJPanel {

    private static final long serialVersionUID = 1L;
    public boolean isInit = false;
    public boolean isDisposed = false;

    /**
     * Default constructor
     */
    public GLInitPanel(GLCapabilities capabilities) {
        super(capabilities);

        Thread.setDefaultUncaughtExceptionHandler(new GLUncaughtExceptionHandlerDecorator(Thread.getDefaultUncaughtExceptionHandler()));
        GLListener listener = new GLListener(this);
        addGLEventListener(listener);

        setPreferredSize(new Dimension(1, 1));
        setMinimumSize(new Dimension(1, 1));
        setVisible(true);
    }

    /**
     * This function is called after OpenGL is initialzed. It disposes itself,
     * since the component will not be used any more.
     */
    private void postInit() {
        setVisible(false);
    }

    /**
     * Starts the view chain creation in a separate thread
     */
    public static void startViewChainThread() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ImageViewerGui.getSingletonInstance().createViewchains();
            }
        });
    }

    /**
     * Implementation GLEventHandler to startup OpenGL2. It implements all
     * listener functions.
     */
    private class GLListener implements GLEventListener {
        private final GLInitPanel parent;

        GLListener(GLInitPanel _parent) {
            super();
            parent = _parent;

        }

        @Override
        public void display(GLAutoDrawable gLDrawable) {
            final GL2 gl = (GL2) gLDrawable.getGL();

            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            isInit = true;

            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Dispose GLInitPanel: Stop the animator and make it invisible");

            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Set GL properties");
            final GL2 gl = (GL2) drawable.getGL();
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Shade model: flat");
            gl.glShadeModel(GL2.GL_FLAT);
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Clear color: black");
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Enable 1D texture");
            // gl.glEnable(GL2.GL_TEXTURE_1D);
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Enable 2D texture");
            // gl.glEnable(GL2.GL_TEXTURE_2D);

            GLInfo.update(gl);

            if (GLInfo.glIsUsable()) {
                GLShaderHelper.initHelper(gl, JHVDirectory.TEMP.getPath());
                GLShaderBuilder.initShaderBuilder(gl);

                GLTextureHelper.setTextureNonPowerOfTwo(false);

                //Fix for retina displays
                GLTextureHelper.setPixelHIFactorWidth(parent.getCurrentSurfaceScale(new int[2])[0]);
                GLTextureHelper.setPixelHIFactorHeight(parent.getCurrentSurfaceScale(new int[2])[1]);
                int scale = parent.getCurrentSurfaceScale(new int[2])[0];
                Object obj = Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor");

                if (obj instanceof Float) {
                    Float f = (Float) obj;
                    if (f > scale) {
                        scale = f.intValue();
                    }
                }
                Displayer.screenScale = scale;

            } else {
                Message.err("Could not initialize OpenGL", "OpenGL could not be initialized properly during startup. JHelioviewer will start in software mode. OpenGL is not available on the system or incompatible.", false);
            }
            Log.debug(">> GLInitPanel.init(GLAutoDrawable) > Start viewchain creation");
            isInit = true;
            parent.postInit();

        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            final GL2 gl = (GL2) drawable.getGL();

            gl.glViewport(x, y, width, height);
        }

        @Override
        public void dispose(GLAutoDrawable arg0) {
            isDisposed = true;
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
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (e instanceof GLException) {
                Throwable cause = e.getCause();
                if (cause == null || cause instanceof GLException) {
                    e.printStackTrace();
                    Log.warn("GLException detected. Disable OpenGL as a precaution.", cause);
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
