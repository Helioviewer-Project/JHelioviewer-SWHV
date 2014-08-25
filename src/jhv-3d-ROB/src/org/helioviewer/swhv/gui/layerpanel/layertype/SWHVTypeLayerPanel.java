package org.helioviewer.swhv.gui.layerpanel.layertype;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.swhv.gui.GUISettings;
import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerController;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerPanel;

public class SWHVTypeLayerPanel extends SWHVAbstractLayerPanel {
    private static final long serialVersionUID = 1L;

    private SWHVTypeLayerController controller;
    private JButton addTypeButton;

    /*
     * Constructor put on the Eventqueue to void multithreading problems
     */
    public SWHVTypeLayerPanel() {
        setToolTipText("Select your date range");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
                setOpaque(true);
                setBackground(GUISettings.NONACTIVECOLOR);
                getPreferredSize().height = GUISettings.LAYERHEIGHT;
                setBorder(BorderFactory.createLineBorder(GUISettings.LINEBORDERCOLOR));
                addTypeButton();
                addTypeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Date endDate = new Date(System.currentTimeMillis());
                        Date beginDate = new Date(endDate.getTime() - 1000 * 60 * 60 * 4);
                        SWHVDateRangeLayerModel model = new SWHVDateRangeLayerModel();
                        SWHVDateRangeLayerPanel panel = new SWHVDateRangeLayerPanel();
                        model.addListener(panel);
                        SWHVDateRangeLayerController controller = new SWHVDateRangeLayerController(model, panel);
                        model.setBeginDate(beginDate);
                        model.setEndDate(endDate);
                        model.setRoot();
                    }
                });
                setDimensions();
            }
        });
    }

    public void addTypeButton() {
        addTypeButton = new JButton();
        addTypeButton.setIcon(IconBank.getIcon(JHVIcon.BLANK));
        addTypeButton.setContentAreaFilled(false);
        addTypeButton.setBorderPainted(false);
        addTypeButton.setBackground(null);
        add(addTypeButton);
        addTypeButton.setPreferredSize(new Dimension(GUISettings.LAYERHEIGHT, GUISettings.LAYERHEIGHT));
        addTypeButton.setMargin(new Insets(0, 0, 0, 0));
        addTypeButton.setOpaque(true);
    }

    /*
     * GUI methods put on the Eventqueue to void multithreading problems
     */

    @Override
    public void updateActive(final SWHVLayerModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                validate();
                repaint();
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

    public SWHVTypeLayerController getButtonListLayerController() {
        return this.controller;
    }

    public void setController(SWHVTypeLayerController swhvTypeLayerController) {
        this.controller = swhvTypeLayerController;
    }

}
