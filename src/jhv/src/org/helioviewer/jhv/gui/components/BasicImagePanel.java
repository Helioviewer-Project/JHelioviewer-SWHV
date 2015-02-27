package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * This class represents a basic image component that is used to display the
 * image of all images.
 *
 * @author Stephan Pagel
 *
 * */
public class BasicImagePanel extends JPanel implements ComponentListener, ViewListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    protected ComponentView componentView;
    protected ViewportView viewportView;
    protected RegionView regionView;
    protected MetaDataView metaDataView;

    protected ImagePanelInputController inputController;

    protected AbstractList<ImagePanelPlugin> plugins;

    protected Component renderedImageComponent; // don't touch this

    protected AbstractList<ScreenRenderer> postRenderers;

    protected boolean updateViewportView = true;

    protected Image backgroundImage;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public BasicImagePanel() {
        super(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // initialize list of plugins
        plugins = new LinkedList<ImagePanelPlugin>();

        // initialize container for post renderer
        postRenderers = new LinkedList<ScreenRenderer>();

        // add component listener
        addComponentListener(this);
    }

    public void setUpdateViewportView(boolean doUpdate) {
        updateViewportView = doUpdate;
    }

    public void setBackgroundImage(Image img) {
        backgroundImage = img;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null)
            g.drawImage(backgroundImage, 0, 0, null);
    }

    /**
     * Adds an mouse listener to the component.
     */
    @Override
    public void addMouseListener(MouseListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.addMouseListener(l);
    }

    /**
     * Adds an mouse motion listener to the component.
     */
    @Override
    public void addMouseMotionListener(MouseMotionListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.addMouseMotionListener(l);
    }

    /**
     * Adds an mouse wheel listener to the component.
     */
    @Override
    public void addMouseWheelListener(MouseWheelListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.addMouseWheelListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    @Override
    public void removeMouseListener(MouseListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.removeMouseListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    @Override
    public void removeMouseMotionListener(MouseMotionListener l) {
        if (renderedImageComponent != null)
            renderedImageComponent.removeMouseMotionListener(l);
    }

    /**
     * Removes an mouse listener from the component.
     */
    @Override
    public void removeMouseWheelListener(MouseWheelListener l) {
        if (renderedImageComponent != null)
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
        if (renderedImageComponent != null) {
            if (componentView != null) // on first invocation
                componentView.removeViewListener(this);

            renderedImageComponent.removeMouseListener(inputController);
            renderedImageComponent.removeMouseMotionListener(inputController);
            renderedImageComponent.removeMouseWheelListener(inputController);
        }

        componentView = newView;

        viewportView = ViewHelper.getViewAdapter(componentView, ViewportView.class);
        regionView = ViewHelper.getViewAdapter(componentView, RegionView.class);
        metaDataView = ViewHelper.getViewAdapter(componentView, MetaDataView.class);

        if (componentView != null) {
            componentView.setComponent(renderedImageComponent);

            componentView.addViewListener(this);
            setInputController(inputController);
            setPostRenderers();

            componentView.getAdapter(ViewportView.class).setViewport(getViewport(), new ChangeEvent());
        }

        for (ImagePanelPlugin p : plugins) {
            p.setImagePanel(this);
            p.setView(componentView);
        }
        if (viewportView != null && regionView != null && metaDataView != null) {
            if (updateViewportView)
                viewportView.setViewport(getViewport(), new ChangeEvent());

            Viewport v = viewportView.getViewport();
            Region r = regionView.getRegion();
            MetaData m = metaDataView.getMetaData();

            if (v != null && r != null && m != null)
                regionView.setRegion(ViewHelper.expandRegionToViewportAspectRatio(v, r, m), new ChangeEvent());
        }
        repaint();
    }

    /**
     * Sets the existing post renderer to the (new) component view.
     */
    private void setPostRenderers() {

        if (componentView != null) {
            for (ScreenRenderer r : postRenderers)
                componentView.addPostRenderer(r);
        }
    }

    /**
     * Returns the provided viewport of this component
     *
     * @return provided viewport of this component.
     * */
    public Viewport getViewport() {
        return StaticViewport.createAdaptedViewport(Math.max(1, GLTextureHelper.getPixelHIFactorWidth() * getWidth() - 2), Math.max(1, GLTextureHelper.getPixelHIFactorHeight() * getHeight() - 2));
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

        if (renderedImageComponent != null) {
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
        }

        inputController = newInputController;
    }

    /**
     * Adds the passed post renderer to the image component.
     *
     * @param postRenderer
     *            new post renderer for the image component.
     */
    public void addPostRenderer(ScreenRenderer postRenderer) {

        if (postRenderer != null) {
            postRenderers.add(postRenderer);

            if (componentView != null)
                componentView.addPostRenderer(postRenderer);
        }
    }

    /**
     * Adds the passed post renderer from the image component.
     *
     * @param postRenderer
     *            post renderer which has to be removed from image component.
     */
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            postRenderers.remove(postRenderer);

            if (componentView != null)
                componentView.removePostRenderer(postRenderer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {

            viewportView = ViewHelper.getViewAdapter(componentView, ViewportView.class);
            regionView = ViewHelper.getViewAdapter(componentView, RegionView.class);
            metaDataView = ViewHelper.getViewAdapter(componentView, MetaDataView.class);
            if (viewportView != null && regionView != null && metaDataView != null) {
                if (updateViewportView)
                    viewportView.setViewport(getViewport(), new ChangeEvent());

                Viewport v = viewportView.getViewport();
                Region r = regionView.getRegion();
                MetaData m = metaDataView.getMetaData();

                if (v != null && r != null && m != null)
                    regionView.setRegion(ViewHelper.expandRegionToViewportAspectRatio(v, r, m), new ChangeEvent());
            }
            repaint();
            return;
        }

        // if (aEvent.reasonOccurred(SubImageDataChangedReason.class) ||
        // aEvent.reasonOccurred(RegionChangedReason.class))
        // repaint();
    }

    // ////////////////////////////////////////////////////////////////
    // Component Listener
    // ////////////////////////////////////////////////////////////////

    /**
     * Method will be called when component was hidden.
     */
    @Override
    public void componentHidden(ComponentEvent e) {
    }

    /**
     * Method will be called when component was moved.
     */
    @Override
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * Method will be called when component was resized. Resets the viewport and
     * region.
     */
    @Override
    public void componentResized(ComponentEvent e) {

        if (viewportView != null && regionView != null && metaDataView != null) {
            if (updateViewportView)
                viewportView.setViewport(getViewport(), new ChangeEvent());

            Viewport v = viewportView.getViewport();
            Region r = regionView.getRegion();
            MetaData m = metaDataView.getMetaData();

            if (v != null && r != null && m != null)
                regionView.setRegion(ViewHelper.expandRegionToViewportAspectRatio(v, r, m), new ChangeEvent());
        } else {
            repaint();
        }
    }

    /**
     * Method will be called when component was shown.
     */
    @Override
    public void componentShown(ComponentEvent e) {
    }

}
