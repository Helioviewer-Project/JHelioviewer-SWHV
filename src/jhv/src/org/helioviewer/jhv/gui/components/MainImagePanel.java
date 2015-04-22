package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.controller.CameraMouseController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.opengl.GLSharedDrawable;

/**
 * This class represents an image component that is used to display the image of
 * all images.
 *
 * @author caplins
 * @author Alen Agheksanterian
 * @author Benjamin Wamsler
 * @author Stephan Pagel
 * @author Markus Langenberg
 */
public class MainImagePanel extends JPanel {

    private final ArrayList<MouseMotionListener> mouseMotionListeners = new ArrayList<MouseMotionListener>();

    private final CameraMouseController mouseController = new CameraMouseController();
    private final Component renderedImageComponent = GLSharedDrawable.getSingletonInstance().getCanvas();

    protected ComponentView componentView;
    protected AbstractList<ImagePanelPlugin> plugins;

    /**
     * Default constructor.
     * */
    public MainImagePanel() {
        super(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // initialize list of plugins
        plugins = new LinkedList<ImagePanelPlugin>();
        add(renderedImageComponent);

        mouseController.setImagePanel(this);
    }

    /**
     * Adds an mouse listener to the component.
     */
    @Override
    public void addMouseListener(MouseListener l) {
        if (!Arrays.asList(renderedImageComponent.getMouseListeners()).contains(l)) {
            renderedImageComponent.addMouseListener(l);
        }
    }

    /**
     * Adds an mouse wheel listener to the component.
     */
    @Override
    public void addMouseWheelListener(MouseWheelListener l) {
        renderedImageComponent.addMouseWheelListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    @Override
    public void removeMouseListener(MouseListener l) {
        renderedImageComponent.removeMouseListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    @Override
    public void removeMouseWheelListener(MouseWheelListener l) {
        renderedImageComponent.removeMouseWheelListener(l);
    }

    /**
     * Sets the component view which acts as the last view in the associated
     * view chain and provides the data for this component.
     *
     * @param newView
     *            new component view.
     */
    public void setView(ComponentView newView) {
        for (MouseMotionListener l : mouseMotionListeners) {
            renderedImageComponent.removeMouseMotionListener(l);
        }

        renderedImageComponent.removeMouseListener(mouseController);
        renderedImageComponent.removeMouseMotionListener(mouseController);
        renderedImageComponent.removeMouseWheelListener(mouseController);

        componentView = newView;
        if (componentView != null) {
            componentView.setComponent(renderedImageComponent);
            setInputController(mouseController);
        }

        for (ImagePanelPlugin p : plugins) {
            p.setImagePanel(this);
            p.setView(componentView);
        }

        if (newView != null) {
            for (MouseMotionListener l : mouseMotionListeners) {
                renderedImageComponent.addMouseMotionListener(l);
            }
        }
    }

    /**
     * Adds a new plug-in to the component. Plug-ins in this case are controller
     * which e.g. has to react on inputs made to this component.
     *
     * @param newPlugin
     *            new plug-in which has to to be added to this component
     */
    public void addPlugin(ImagePanelPlugin newPlugin) {
        if (newPlugin == null) {
            return;
        }

        newPlugin.setImagePanel(this);
        if (componentView != null) {
            newPlugin.setView(componentView);
        }
        plugins.add(newPlugin);
    }

    /**
     * Removes a plug-in from the component.
     *
     * @param oldPlugin
     *            plug-in which has to to be removed from this component
     *
     * @see MainImagePanel#addPlugin(ImagePanelPlugin)
     */
    private void removePlugin(ImagePanelPlugin oldPlugin) {
        if (oldPlugin == null) {
            return;
        }

        oldPlugin.setView(null);
        oldPlugin.setImagePanel(null);
        plugins.remove(oldPlugin);
    }

    /**
     * Sets the passed input controller as active one and removes the existing.
     *
     * <p>
     * Note, that every input controller is also registered as a plugin.
     *
     * @param newInputController
     *            new input controller.
     * @see #addPlugin(ImagePanelPlugin)
     */
    private void setInputController(CameraMouseController newInputController) {
        renderedImageComponent.addMouseListener(newInputController);
        renderedImageComponent.addMouseMotionListener(newInputController);
        renderedImageComponent.addMouseWheelListener(newInputController);

        if (KeyListener.class.isAssignableFrom(newInputController.getClass())) {
            renderedImageComponent.addKeyListener((KeyListener) newInputController);
        }
    }

    @Override
    public void addMouseMotionListener(MouseMotionListener l) {
        if (l != null) {
            mouseMotionListeners.add(l);
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void removeMouseMotionListener(MouseMotionListener l) {
        if (l != null) {
            mouseMotionListeners.remove(l);
        }
    }

}
