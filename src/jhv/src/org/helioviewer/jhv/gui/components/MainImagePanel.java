package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
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
    private final LinkedList<ImagePanelPlugin> plugins = new LinkedList<ImagePanelPlugin>();

    private final CameraMouseController mouseController = new CameraMouseController();
    private final Component renderComponent = GLSharedDrawable.getSingletonInstance().getCanvas();
    private final ComponentView componentView = new ComponentView();

    public MainImagePanel() {
        super(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        add(renderComponent);

        componentView.setComponent(renderComponent);
        componentView.activate();

        mouseController.setImagePanel(this);
        renderComponent.addMouseListener(mouseController);
        renderComponent.addMouseMotionListener(mouseController);
        renderComponent.addMouseWheelListener(mouseController);

        if (KeyListener.class.isAssignableFrom(mouseController.getClass())) {
            renderComponent.addKeyListener((KeyListener) mouseController);
        }
    }

    public ComponentView getComponentView() {
        return componentView;
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
        newPlugin.setView(componentView);
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
    public void removePlugin(ImagePanelPlugin oldPlugin) {
        if (oldPlugin == null) {
            return;
        }

        oldPlugin.setView(null);
        oldPlugin.setImagePanel(null);
        plugins.remove(oldPlugin);
    }

    @Override
    public void addMouseListener(MouseListener l) {
        if (!Arrays.asList(renderComponent.getMouseListeners()).contains(l)) {
            renderComponent.addMouseListener(l);
        }
    }

    @Override
    public void addMouseWheelListener(MouseWheelListener l) {
        renderComponent.addMouseWheelListener(l);
    }

    @Override
    public void removeMouseListener(MouseListener l) {
        renderComponent.removeMouseListener(l);
    }

    @Override
    public void removeMouseWheelListener(MouseWheelListener l) {
        renderComponent.removeMouseWheelListener(l);
    }

    @Override
    public void addMouseMotionListener(MouseMotionListener l) {
        if (l != null) {
            mouseMotionListeners.add(l);
        }
    }

    @Override
    public void removeMouseMotionListener(MouseMotionListener l) {
        if (l != null) {
            mouseMotionListeners.remove(l);
        }
    }

}
