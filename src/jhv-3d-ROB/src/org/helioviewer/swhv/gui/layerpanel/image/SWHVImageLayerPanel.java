package org.helioviewer.swhv.gui.layerpanel.image;

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
import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractRegistrableLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;

public class SWHVImageLayerPanel extends SWHVAbstractRegistrableLayerPanel {
    private static final long serialVersionUID = 1L;
    private SWHVImageLayerController controller;

    public SWHVImageLayerPanel() {
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
                labelName.setText("Image");
                labelName.setOpaque(true);
                labelName.setBackground(null);
                labelName.addMouseListener(new MouseListener() {

                    @Override
                    public void mouseClicked(MouseEvent arg0) {
                        getController().setActive();
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
                        getController().remove();
                    }
                });
                setDimensions();

            }
        });
    }

    public void setController(SWHVImageLayerController controller) {
        this.controller = controller;
    }

    @Override
    public SWHVImageLayerController getController() {
        return this.controller;
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
