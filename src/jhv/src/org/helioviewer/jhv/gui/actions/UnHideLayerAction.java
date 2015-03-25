package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.helioviewer.jhv.gui.components.layerTable.renderers.DescriptorIconRenderer;
import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Action to show the given layer.
 *
 * @author Malte Nuhn
 */
public class UnHideLayerAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    /**
     * Controlled layer by this action.
     */
    private final JHVJP2View view;

    /**
     * Creates a action to show a given layer
     *
     * @param view
     *            Layer to control
     */
    public UnHideLayerAction(JHVJP2View view) {
        super("Show Layer");

        LayerDescriptor ld = LayersModel.getSingletonInstance().getDescriptor(view);
        ld.isVisible = false;

        this.putValue(Action.SMALL_ICON, DescriptorIconRenderer.getIconTooltip(ld).a);
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        LayersModel.getSingletonInstance().setVisibleLink(view, true);
    }

}
