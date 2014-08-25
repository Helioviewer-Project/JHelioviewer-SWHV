package org.helioviewer.swhv.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerCurrentOptionsPanel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerController;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerController;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerModel;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerPanel;

public class SWHVGUI extends JFrame {

    JPanel leftPanel;
    private final JSplitPane horizontalSplitPane;
    private final JPanel rightPanel;

    public SWHVGUI() {
        super();
        this.setSize(GUISettings.STARTUPWIDTH, GUISettings.STARTUPHEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout(0, 0));
        leftPanel = new JPanel();
        this.getContentPane().add(leftPanel, BorderLayout.WEST);
        leftPanel.setPreferredSize(new Dimension(GUISettings.LEFTPANELWIDTH, GUISettings.STARTUPHEIGHT));
        leftPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        SWHVLayerContainerPanel gridPanel = new SWHVLayerContainerPanel();
        SWHVLayerContainerModel layerContainerModel = GlobalStateContainer.getSingletonInstance().getLayerContainerModel();
        SWHVLayerContainerController layerContainerController = new SWHVLayerContainerController(layerContainerModel, gridPanel);
        layerContainerModel.addListener(gridPanel);
        leftPanel.add(gridPanel);
        leftPanel.setBackground(GUISettings.LEFTPANELBACKGROUNDCOLOR);
        leftPanel.setOpaque(true);

        Date endDate = new Date(System.currentTimeMillis());
        Date beginDate = new Date(endDate.getTime() - 1000 * 60 * 60 * 4);
        SWHVDateRangeLayerModel mmodel = new SWHVDateRangeLayerModel();
        SWHVDateRangeLayerPanel mdateRangepanel = new SWHVDateRangeLayerPanel();
        mmodel.addListener(mdateRangepanel);
        SWHVDateRangeLayerController mcontroller = new SWHVDateRangeLayerController(mmodel, mdateRangepanel);
        mmodel.setBeginDate(beginDate);
        mmodel.setEndDate(endDate);
        SWHVLayerCurrentOptionsPanel layerCurrentOptionsPanel = new SWHVLayerCurrentOptionsPanel();
        GlobalStateContainer.getSingletonInstance().getLayerContainerModel().addListener(layerCurrentOptionsPanel);
        mmodel.setRoot();
        leftPanel.add(layerCurrentOptionsPanel);

        rightPanel = new JPanel();

        horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        horizontalSplitPane.setOneTouchExpandable(true);
        horizontalSplitPane.setDividerLocation(-1);
        horizontalSplitPane.setBackground(Color.BLACK);

        Dimension minimumSize = new Dimension(100, 50);
        leftPanel.setMinimumSize(minimumSize);
        rightPanel.setMinimumSize(minimumSize);
        this.add(horizontalSplitPane);
        this.setVisible(true);
    }

    public static void main(String[] args) {
        JFrame frame = new SWHVGUI();
        frame.setVisible(true);
    }

}
