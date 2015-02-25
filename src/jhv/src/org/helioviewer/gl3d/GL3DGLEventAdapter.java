package org.helioviewer.gl3d;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.helioviewer.viewmodel.view.opengl.GL3DComponentView;

/**
 * Adapter for a {@link GLEventListener}. Add an adapter to the
 * {@link GL3DComponentView}'s getComponent().
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DGLEventAdapter implements GLEventListener {

    @Override
    public void display(GLAutoDrawable autoDrawable) {
    }

    public void displayChanged(GLAutoDrawable autoDrawable, boolean widt, boolean height) {
    }

    @Override
    public void init(GLAutoDrawable autoDrawable) {
    };

    @Override
    public void reshape(GLAutoDrawable autoDrawable, int x, int y, int width, int height) {
    }

    @Override
    public void dispose(GLAutoDrawable arg0) {
        // TODO Auto-generated method stub

    };
}
