package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.EventQueue;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;

/**
 * Status panel for displaying whether the image contains helioviewer meta data
 * or not.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * If there is no layer present, this panel will be invisible.
 * 
 * @author Markus Langenberg
 */
public class MetaDataStatusPanel extends ViewStatusPanelPlugin {

    private static final long serialVersionUID = 1L;

    private static final Icon checkIcon = IconBank.getIcon(JHVIcon.CHECK);
    private static final Icon exIcon = IconBank.getIcon(JHVIcon.EX);

    /**
     * Default constructor.
     */
    public MetaDataStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());

        setText("Meta:");
        setIcon(exIcon);
        setVerticalTextPosition(JLabel.CENTER);
        setHorizontalTextPosition(JLabel.LEFT);
        //validate();
        //setPreferredSize(null);

        LayersModel.getSingletonInstance().addLayersListener(this);

    }

    /**
     * {@inheritDoc}
     */
    private void activeLayerChanged_raw(int idx) {
        if (LayersModel.getSingletonInstance().isValidIndex(idx)) {
            View view = LayersModel.getSingletonInstance().getLayer(idx);

            setVisible(true);

            MetaDataView metaDataView = view.getAdapter(MetaDataView.class);
            // this operates on the raw viewchain...
            // it might be a good idea to push that functionality to LayersModel
            if (metaDataView != null) {
                MetaData m = metaDataView.getMetaData();
                if (m instanceof HelioviewerMetaData)
                    setIcon(checkIcon);
                else
                    setIcon(exIcon);
            }
            //validate();
            //setPreferredSize(null);
        } else {
            setVisible(false);
        }
    }

    public void activeLayerChanged(final int idx) {
        if (EventQueue.isDispatchThread()) {
            activeLayerChanged_raw(idx);
        } else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    activeLayerChanged_raw(idx);
                }
            });
        }
    }

}
