package org.helioviewer.swhv;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.KakaduEngine;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.resourceloader.ResourceLoader;
import org.helioviewer.swhv.objects3d.Cube;
import org.helioviewer.swhv.objects3d.Solar3DObject;
import org.helioviewer.swhv.objects3d.SolarObject;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

public class joglexperiment2 implements GLEventListener, ViewListener {
    private static Frame frame;
    private SolarObject solarObject;
    GLProfile profile;
    private JHVJPXView jpxView;
    private final int imageWidth = 512;
    private final int imageHeight = 512;
    private GLCanvas canvas;

    private long previousTime;
    private final int numberOfIterations = 0;
    private double avg;
    private Solar3DObject s3d;
    private static Trackball tb;
    private Cube cube;

    /**
     * @param args
     *            the command line arguments
     */
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
        joglexperiment2 test = new joglexperiment2();
        test.getCanvas().setSize(new Dimension(1024, 1024));
        frame = new Frame("Test");
        frame.setLayout(new FlowLayout());
        frame.add(test.getCanvas());
        //ImageDataPanel imageDataPanel = new ImageDataPanel();
        //frame.add(imageDataPanel, BorderLayout.WEST);

        frame.setSize(test.getCanvas().getWidth(), test.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
        //frame.addMouseMotionListener(tb);

    }

    public joglexperiment2() {
        previousTime = System.currentTimeMillis();
        KakaduEngine engine = new KakaduEngine();
        URI libs = JHVDirectory.LIBS.getFile().toURI();
        URI libsBackup = JHVDirectory.LIBS_LAST_CONFIG.getFile().toURI();
        //System.exit(1);

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

        // The following code-block attempts to start the native message
        // handling
        try {
            Log.debug("Setup Kakadu message handlers.");
            engine.startKduMessageSystem();
        } catch (JHV_KduException e) {
            Log.fatal("Failed to setup Kakadu message handlers.", e);
            Message.err("Error starting Kakadu message handler", e.getMessage(), true);
        }

        initGL();

        setJPXView();

        this.solarObject = new SolarObject();
        s3d = new Solar3DObject();
        cube = new Cube();
    }

    public void setJPXView() {
        String startTime = "2014-02-25T00:00:00.000Z";
        String endTime = "2014-02-25T12:00:00.000Z";
        ImageInfoView view;
        try {
            view = APIRequestManager.newLoad(new File("/Users/freekv/JHelioViewer/Downloads/SWAP.jpx").toURI(), true, null);
            jpxView = view.getAdapter(JHVJPXView.class);
            //SWHVMetadataContainer.getSingletonInstance().parseMetadata(jpxView);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (jpxView != null) {
            JP2Image image = jpxView.getJP2Image();
            ResolutionSet rs = image.getResolutionSet();
            for (int i = 0; i < rs.getMaxResolutionLevels(); i++) {
                Log.debug("resolution level " + i + " : " + rs.getResolutionLevel(i));
            }
            this.jpxView.getJP2Image().getResolutionSet().getNextResolutionLevel(new Dimension(1024, 1024));
            ChangeEvent e = new ChangeEvent();

            this.jpxView.setViewport(new ViewportAdapter(new StaticViewport(1024, 1024)), e);
            Interval<Integer> interval = image.getCompositionLayerRange();
            Log.debug("the interval is : " + interval);
            Log.debug("the start of the interval : " + interval.getStart());
            Log.debug("the end of the interval : " + interval.getEnd());
        }

    }

    private void initGL() {
        profile = GLProfile.getGL2GL3();
        GLCapabilities capabilities = new GLCapabilities(profile);
        canvas = new GLCanvas(capabilities);
        canvas.setSize(imageWidth, imageHeight);
        canvas.addGLEventListener(this);
        tb = new Trackball();
        tb.trackballReshape(imageWidth, imageHeight);
        tb.trackballInit(0);
        this.canvas.addMouseMotionListener(tb);
        this.canvas.addMouseListener(tb);
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        canvas.setAutoSwapBufferMode(false);

        GL3 gl = glad.getGL().getGL3();

        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_BACK);
        gl.glFrontFace(GL3.GL_CW);
        solarObject.initializeObject(gl);
        s3d.initializeObject(gl);
        cube.initializeObject(gl);
        //jpxView.addViewListener(solarObject);
        jpxView.addViewListener(this);
        this.jpxView.setCurrentFrame(1, new ChangeEvent(), true);
        gl.setSwapInterval(0);
        int delay = 1000; //milliseconds
        ActionListener taskPerformer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                canvas.display();
                if (jpxView.getCurrentFrameNumber() + 1 < jpxView.getMaximumFrameNumber()) {
                    jpxView.setCurrentFrame(jpxView.getCurrentFrameNumber() + 1, new ChangeEvent(), true);
                } else {
                    jpxView.setCurrentFrame(0, new ChangeEvent(), true);
                }
                long ct = System.currentTimeMillis();
                bt = ct;
            }
        };
        new Timer(40, taskPerformer).start();
    }

    static long bt = System.currentTimeMillis();

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {

        GL3 gl = glad.getGL().getGL3();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
        //solarObject.render(gl);
        //s3d.render(gl, tb.tbMatrix());
        cube.render(gl, tb.trackballMatrix());
        glad.swapBuffers();

    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        GL3 gl = glad.getGL().getGL3();
        gl.glViewport(x, y, w, h);
        this.canvas.reshape(x + 250, y, w, h);
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(SubImageDataChangedReason.class)) {

        }
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas) {
        this.canvas = canvas;
    }
}
