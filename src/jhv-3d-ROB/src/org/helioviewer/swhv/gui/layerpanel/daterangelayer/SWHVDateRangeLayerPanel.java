package org.helioviewer.swhv.gui.layerpanel.daterangelayer;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.swhv.gui.GUISettings;
import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;

public class SWHVDateRangeLayerPanel extends SWHVAbstractLayerPanel implements SWHVDateRangeLayerModelListener {
    private static final long serialVersionUID = 1L;

    private JLabel labelDate;

    private SWHVDateRangeLayerController controller;

    public SWHVDateRangeLayerPanel() {
        setToolTipText("Select your date range");
        SwingUtilities.invokeLater(new Runnable() {
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
                        getDateRangeLayerController().toggleFold();
                    }
                });

                labelDate = new JLabel();
                labelDate.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
                labelDate.getPreferredSize().height = GUISettings.LAYERHEIGHT;
                labelDate.setText("No date yet");
                labelDate.setOpaque(true);
                labelDate.setBackground(null);
                labelDate.addMouseListener(new MouseListener() {

                    @Override
                    public void mouseClicked(MouseEvent arg0) {
                        getDateRangeLayerController().setActive();
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
                add(labelDate);

                addRemoveButton();
                removeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        getDateRangeLayerController().remove();
                    }
                });
                setDimensions();

            }
        });
    }

    public void updateDateRangeLabels(final SWHVDateRangeLayerModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                labelDate.setText(model.getBeginDate() + " - " + model.getEndDate() + " ");
            }
        });
    }

    @Override
    public void beginDateChanged(final SWHVDateRangeLayerModel model) {
        this.updateDateRangeLabels(model);
    }

    @Override
    public void endDateChanged(final SWHVDateRangeLayerModel model) {
        this.updateDateRangeLabels(model);
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

    @Override
    public void updateLevel(final SWHVDateRangeLayerModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                spacer.setPreferredSize(new Dimension(GUISettings.INDENTLEVELSTEP * model.getLevel(), GUISettings.LAYERHEIGHT));
                spacer.setMaximumSize(new Dimension(GUISettings.INDENTLEVELSTEP * model.getLevel(), GUISettings.LAYERHEIGHT));
                spacer.setMinimumSize(new Dimension(GUISettings.INDENTLEVELSTEP * model.getLevel(), GUISettings.LAYERHEIGHT));
                spacer.revalidate();
                spacer.repaint();
            }
        });
    }

    @Override
    public void updateFolded(final SWHVDateRangeLayerModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (model.isFolded()) {
                    treeButton.setIcon(IconBank.getIcon(JHVIcon.BACK));
                } else {
                    treeButton.setIcon(IconBank.getIcon(JHVIcon.FORWARD));
                }
            }
        });
    }

    /*
     * Generic controller things
     */
    @Override
    public SWHVLayerController getController() {
        return this.controller;
    }

    public SWHVDateRangeLayerController getDateRangeLayerController() {
        return this.controller;
    }

    public void setController(SWHVDateRangeLayerController controller) {
        this.controller = controller;
    }

    public String getDateLabelText() {
        return this.labelDate.getText();
    }

}
