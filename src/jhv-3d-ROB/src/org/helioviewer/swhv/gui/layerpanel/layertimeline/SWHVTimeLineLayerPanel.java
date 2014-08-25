package org.helioviewer.swhv.gui.layerpanel.layertimeline;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.helioviewer.swhv.gui.GUISettings;
import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;

public class SWHVTimeLineLayerPanel extends SWHVAbstractLayerPanel {
    private static final long serialVersionUID = 1L;

    private SWHVTimeLineLayerController controller;

    public SWHVTimeLineLayerPanel() {

        SwingUtilities.invokeLater(new Runnable() {
            private JLabel labelName;

            @Override
            public void run() {
                setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
                setOpaque(true);
                setBackground(GUISettings.NONACTIVECOLOR);
                getPreferredSize().height = GUISettings.LAYERHEIGHT;
                setBorder(BorderFactory.createLineBorder(GUISettings.LINEBORDERCOLOR));

                addSpacer();
                addTreeButton();
                treeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        getController().toggleFold();
                    }
                });

                labelName = new JLabel();
                labelName.setFont(new Font(GUISettings.MAINFONT, Font.PLAIN, GUISettings.LABELHEIGHTLAYERSPANEL));
                labelName.getPreferredSize().height = GUISettings.LAYERHEIGHT;
                labelName.setText("TimeLine");
                labelName.setOpaque(true);
                labelName.setBackground(null);

                labelName.addMouseListener(new MouseListener() {

                    @Override
                    public void mouseClicked(MouseEvent arg0) {
                        getTimeLineLayerController().setActive();
                    }

                    @Override
                    public void mouseEntered(MouseEvent arg0) {
                    }

                    @Override
                    public void mouseExited(MouseEvent arg0) {
                    }

                    @Override
                    public void mousePressed(MouseEvent arg0) {
                    }

                    @Override
                    public void mouseReleased(MouseEvent arg0) {
                    }
                });
                add(labelName);

                addRemoveButton();
                removeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        getTimeLineLayerController().remove();
                    }
                });
                setDimensions();

            }
        });
    }

    @Override
    public SWHVLayerController getController() {
        return this.controller;
    }

    public SWHVTimeLineLayerController getTimeLineLayerController() {
        return this.controller;
    }

    public void setController(SWHVTimeLineLayerController controller) {
        this.controller = controller;
    }

    @Override
    public void updateActive(final SWHVLayerModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (model.isActive()) {
                    setBackground(GUISettings.ACTIVECOLOR);
                } else {
                    setBackground(GUISettings.NONACTIVECOLOR);
                }
            }
        });
    }
}
