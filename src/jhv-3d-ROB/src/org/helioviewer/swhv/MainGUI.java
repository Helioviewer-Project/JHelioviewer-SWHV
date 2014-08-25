package org.helioviewer.swhv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.message.Message;
import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.KakaduEngine;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.resourceloader.ResourceLoader;
import org.helioviewer.swhv.gui.GUISettings;
import org.helioviewer.swhv.gui.ImageDataPanel;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerCurrentOptionsPanel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerController;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerPanel;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerController;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerModel;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerPanel;
import org.helioviewer.swhv.metadata.SWHVMetadataContainer;
import org.helioviewer.swhv.time.GlobalTime;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

import com.jogamp.common.jvm.JNILibLoaderBase;

public class MainGUI extends JFrame {
    static class JoglLoaderDummy implements JNILibLoaderBase.LoaderAction {

        @Override
        public boolean loadLibrary(String arg0, boolean arg1, ClassLoader arg2) {
            return true;
        }

        @Override
        public void loadLibrary(String arg0, String[] arg1, boolean arg2, ClassLoader arg3) {
        }
    }

    private static final long serialVersionUID = -1442153374916511471L;
    private JPanel leftPanel;
    private final Component rightPanel;
    private final GL3DPanel gl3dPanel;

    public MainGUI() {
        super();
        this.setSize(350 + 1024, 1024);
        //MigLayout layout = new MigLayout("fill", "0[350px:350px:350px,right,fill]0[512px:1024px,fill]0", "0[512px:1024px,fill]0[fill]0");
        JPanel panel = new JPanel(new BorderLayout());

        gl3dPanel = new GL3DPanel();

        createLeftPanelAlt();
        panel.add(leftPanel, BorderLayout.WEST);
        rightPanel = gl3dPanel.getCanvas();
        panel.add(rightPanel, BorderLayout.CENTER);

        getContentPane().add(panel);
        this.addComponents();
        this.setupListeners();
        this.setVisible(true);
        GlobalTime gt = new GlobalTime();
        gt.addTimeListener(gl3dPanel);
        SWHVMetadataContainer mdc = SWHVMetadataContainer.getSingletonInstance();
        gt.addTimeListener(mdc);
        gt.runTime();

    }

    public void createLeftPanel() {
        leftPanel = new JPanel();
        leftPanel.setBackground(Color.BLUE);
        final ImageDataPanel imageDataPanel = new ImageDataPanel(gl3dPanel.getSolarObject());
        leftPanel.add(imageDataPanel);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imageDataPanel.loadButtonPressed();
            }
        });
        leftPanel.add(addButton);
    }

    public void createLeftPanelAlt() {
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

    }

    private void addComponents() {

    }

    private void setupListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {

        LogSettings.init("/log4j.initial.properties", "/log4j.initial.properties", ".", false);
        Settings.getSingletonInstance().load();

        // If the user has not specified any desired look and feel yet, the
        // system default theme will be used
        if (Settings.getSingletonInstance().getProperty("display.laf") == null || Settings.getSingletonInstance().getProperty("display.laf").length() <= 0) {
            Log.info("Use default look and feel");
            Settings.getSingletonInstance().setProperty("display.laf", UIManager.getSystemLookAndFeelClassName());
        }
        Settings.getSingletonInstance().save();
        KakaduEngine engine = new KakaduEngine();
        URI libs = JHVDirectory.LIBS.getFile().toURI();
        URI libsBackup = JHVDirectory.LIBS_LAST_CONFIG.getFile().toURI();

        URI libsRemote = null;
        Log.info("Try to load Kakadu libraries");
        System.setProperty("jhv.os", "mac");
        System.setProperty("jhv.java.arch", "64");

        if (null == ResourceLoader.getSingletonInstance().loadResource("kakadu", null, new File("/Users/freekv/JHelioviewer/Libs/").toURI(), new File("/Users/freekv/JHelioviewer/Libs/").toURI(), libsBackup, System.getProperties())) {
            Log.fatal("Could not load Kakadu libraries");
            Message.err("Error loading Kakadu libraries", "Fatal error! The kakadu libraries could not be loaded. The log output may contain additional information.", true);
            return;
        } else {
            Log.info("Successfully loaded Kakadu libraries");
        }

        try {
            Log.debug("Setup Kakadu message handlers.");
            engine.startKduMessageSystem();
        } catch (JHV_KduException e) {
            Log.fatal("Failed to setup Kakadu message handlers.", e);
            Message.err("Error starting Kakadu message handler", e.getMessage(), true);
        }
        /* ----------Setup OpenGL ----------- */

        final URI finalLibs = libs;
        final URI finalLibsRemote = libsRemote;
        final URI finalLibsBackup = libsBackup;

        // Has to run in EventQueue due to bug in NVidia Driver 260.99
        try {
            EventQueue.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    Log.info("Try to load OpenGL libraries");

                    if (null == ResourceLoader.getSingletonInstance().loadResource("jogl2.2.0ROB", finalLibsRemote, finalLibs, finalLibs, finalLibsBackup, System.getProperties())) {
                        Log.error("Could not load OpenGL libraries");
                        Message.err("Error loading OpenGL libraries", "The OpenGL libraries could not be loaded. JHelioviewer will run in software mode.", false);
                        GLInfo.glUnusable();
                    } else {
                        System.setProperty("jogamp.gluegen.UseTempJarCache", "false");
                        JNILibLoaderBase.setLoadingAction(new JoglLoaderDummy());
                        Log.info("Successfully loaded OpenGL libraries");
                    }
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        new MainGUI();
    }
}
