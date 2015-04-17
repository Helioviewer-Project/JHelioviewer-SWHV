package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.opengl.GLSharedDrawable;

/**
 * This class represents a basic image component that is used to display the
 * image of all images.
 *
 * @author Stephan Pagel
 *
 * */
public class BasicImagePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected ComponentView componentView;

    protected ImagePanelInputController inputController;

    protected AbstractList<ImagePanelPlugin> plugins;

    protected final Component renderedImageComponent; // don't touch this

    /**
     * Default constructor.
     * */
    public BasicImagePanel() {
        super(new BorderLayout(0, 0));
        renderedImageComponent = GLSharedDrawable.getSingletonInstance().getCanvas();

        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // initialize list of plugins
        plugins = new LinkedList<ImagePanelPlugin>();
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
     * Adds an mouse motion listener to the component.
     */
    @Override
    public void addMouseMotionListener(MouseMotionListener l) {
        renderedImageComponent.addMouseMotionListener(l);
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
    public void removeMouseMotionListener(MouseMotionListener l) {
        renderedImageComponent.removeMouseMotionListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    @Override
    public void removeMouseWheelListener(MouseWheelListener l) {
        renderedImageComponent.removeMouseWheelListener(l);
    }

    /**
     * Returns the component view which acts as the last view in the associated
     * view chain and provides the data for this component.
     *
     * @return associated component view.
     */
    public ComponentView getView() {
        return componentView;
    }

    /**
     * Sets the component view which acts as the last view in the associated
     * view chain and provides the data for this component.
     *
     * @param newView
     *            new component view.
     */
    public void setView(ComponentView newView) {
        renderedImageComponent.removeMouseListener(inputController);
        renderedImageComponent.removeMouseMotionListener(inputController);
        renderedImageComponent.removeMouseWheelListener(inputController);

        componentView = newView;

        if (componentView != null) {
            componentView.setComponent(renderedImageComponent);
            setInputController(inputController);
        }

        for (ImagePanelPlugin p : plugins) {
            p.setImagePanel(this);
            p.setView(componentView);
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
     * @see BasicImagePanel#addPlugin(ImagePanelPlugin)
     */
    public void removePlugin(ImagePanelPlugin oldPlugin) {
        if (oldPlugin == null) {
            return;
        }

        oldPlugin.setView(null);
        oldPlugin.setImagePanel(null);
        plugins.remove(oldPlugin);
    }

    /**
     * Returns the associated input controller.
     *
     * @return input controller of this component.
     */
    public ImagePanelInputController getInputController() {
        return inputController;
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
    public void setInputController(ImagePanelInputController newInputController) {
        addPlugin(newInputController);

        if (inputController != null) {
            renderedImageComponent.removeMouseListener(inputController);
            renderedImageComponent.removeMouseMotionListener(inputController);
            renderedImageComponent.removeMouseWheelListener(inputController);

            if (KeyListener.class.isAssignableFrom(inputController.getClass())) {
                renderedImageComponent.removeKeyListener((KeyListener) inputController);
            }

        }
        removePlugin(inputController);

        if (newInputController != null) {
            renderedImageComponent.addMouseListener(newInputController);
            renderedImageComponent.addMouseMotionListener(newInputController);
            renderedImageComponent.addMouseWheelListener(newInputController);

            if (KeyListener.class.isAssignableFrom(newInputController.getClass())) {
                renderedImageComponent.addKeyListener((KeyListener) newInputController);
            }
        }

        inputController = newInputController;
    }

}
