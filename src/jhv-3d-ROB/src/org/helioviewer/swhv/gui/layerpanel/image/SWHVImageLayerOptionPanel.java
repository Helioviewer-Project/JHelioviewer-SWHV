package org.helioviewer.swhv.gui.layerpanel.image;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.swhv.GL3DPanel;
import org.helioviewer.swhv.gui.GUISettings;
import org.helioviewer.swhv.gui.ImageDataPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractOptionPanel;

public class SWHVImageLayerOptionPanel extends SWHVAbstractOptionPanel {
    private static final long serialVersionUID = 1L;

    private JPanel mainPanelContainer;
    private final SWHVImageLayerModel model;

    public SWHVImageLayerOptionPanel(SWHVImageLayerModel model) {
        this.model = model;
        createMainPanel();
        setPreferredSize(new Dimension(GUISettings.LEFTPANELWIDTH, GUISettings.OPTIONSHEIGHT));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addTab("Choose image", mainPanelContainer);
            }
        });
    }

    private void createMainPanel() {
        mainPanelContainer = new JPanel();
        mainPanelContainer.add(new JLabel("Image options"));
        final ImageDataPanel imageDataPanel = new ImageDataPanel(GL3DPanel.sun);
        mainPanelContainer.add(imageDataPanel);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imageDataPanel.loadButtonPressed();
            }
        });
        mainPanelContainer.add(addButton);
    }

    @Override
    public void setActive() {
    }
}
