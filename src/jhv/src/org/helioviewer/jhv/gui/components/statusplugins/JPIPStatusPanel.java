package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JWindow;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * Status panel for displaying the status of the JPIP connection.
 * 
 * <p>
 * If the status changes, a small popup will appear.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * This panel is not visible, if the active layer is not an remote JPIP image.
 * 
 * @author Markus Langenberg
 */
public class JPIPStatusPanel extends ViewStatusPanelPlugin {

    private static final long serialVersionUID = 1L;
    private ConnectionStatus lastStatus = ConnectionStatus.CONNECTED;
    private StatusChangedWindow statusChangedWindow = new StatusChangedWindow();

    private static final Icon connectedIcon = IconBank.getIcon(JHVIcon.CONNECTED);
    private static final Icon disconnectedIcon = IconBank.getIcon(JHVIcon.DISCONNECTED);

    public static enum ConnectionStatus {
        CONNECTED, DISCONNECTED, LOCAL
    };

    /**
     * Default constructor.
     */
    public JPIPStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(55, 20));
        setText("JPIP:");
        setIcon(disconnectedIcon);
        setVerticalTextPosition(JLabel.CENTER);
        setHorizontalTextPosition(JLabel.LEFT);

        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    public void updateStatus(ConnectionStatus connectionStatus) {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            setIcon(connectedIcon);
            setText("JPIP:");
        } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
            setIcon(disconnectedIcon);
            setText("JPIP:");
        } else {
            setText("Local");
            setIcon(null);
        }

        if (lastStatus != connectionStatus) {
            lastStatus = connectionStatus;
            statusChangedWindow.popUp(lastStatus);
        }
    }

    /**
     * Internal function for accessing this pane from within the popup.
     * 
     * @return Reference to this
     */
    private JLabel getLabel() {
        return this;
    }

    /**
     * Popup indicating that the status has changed.
     */
    private class StatusChangedWindow extends JWindow implements Runnable {

        private static final long serialVersionUID = 1L;
        private JLabel label = new JLabel("", JLabel.CENTER);

        /**
         * Shows the popup.
         * 
         * @param newStatus
         *            New status to show
         */
        public void popUp(ConnectionStatus newStatus) {
            setVisible(false);
            remove(label);
            if (newStatus == ConnectionStatus.CONNECTED) {
                label = new JLabel("JPIP Server is online");
            } else if (newStatus == ConnectionStatus.DISCONNECTED) {
                label = new JLabel("JPIP Server is offline");
            } else {
                label = new JLabel("Local image");
            }
            label.setPreferredSize(new Dimension(label.getPreferredSize().width + 10, label.getPreferredSize().height + 10));
            add(label);
            setPreferredSize(label.getPreferredSize());
            pack();
            setLocation(getLabel().getLocationOnScreen().x - 40, getLabel().getLocationOnScreen().y - 40);
            setVisible(true);

            Thread closeThread = new Thread(this, "Close JPIP Popup");
            closeThread.start();
        }

        /**
         * Closes the popup after three seconds
         */
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.setVisible(false);
        }
    }

    /**
     * In case the layer itself changed and the JPIP Panel is still visible:
     * update the Panel
     * <p>
     * If the layer is not valid : hide the Panel.
     */

    public void layerChanged(int idx) {

        if (isVisible()) {

            if (LayersModel.getSingletonInstance().isValidIndex(idx)) {
                updateStatus(idx);
            } else {
                setVisible(false);

            }

        }
    }

    /**
     * As long as the new active layer is valid, update the connection panel
     */

    public void activeLayerChanged(int idx) {
        updateStatus(idx);
    }

    private void updateStatus(int layer) {
        if (LayersModel.getSingletonInstance().isValidIndex(layer)) {
            View view = LayersModel.getSingletonInstance().getLayer(layer);
            boolean connected = LayersModel.getSingletonInstance().isConnectedToJPIP(view);
            boolean isRemote = LayersModel.getSingletonInstance().isRemote(view);

            if (isRemote) {
                // updateStatus(connected ? ConnectionStatus.CONNECTED :
                // ConnectionStatus.DISCONNECTED);
            } else {
                // updateStatus(ConnectionStatus.LOCAL);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void layerDownloaded(int idx) {
        updateStatus(idx);
    }

}
