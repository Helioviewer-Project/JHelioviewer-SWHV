package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;
import javax.swing.text.NumberFormatter;

import org.apache.log4j.Level;
import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.ViewchainFactory;
import org.helioviewer.jhv.gui.components.MainImagePanel;
import org.helioviewer.jhv.gui.controller.MainImagePanelMousePanController;
import org.helioviewer.jhv.gui.controller.ZoomController;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ReaderErrorReason;
import org.helioviewer.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.factory.GLViewFactory;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.jp2view.J2KRenderGlobalOptions;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.opengl.GLComponentView;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * Dialog o export movies to standard video formats.
 * 
 * <p>
 * This class includes everything needed to export movies to an external format.
 * Therefore, it copies the existing view chain and performs all its operations
 * on this copy. To encode the final result, the command line toll from FFmpeg
 * is used.
 * 
 * @author Markus Langenberg
 * @author Andre Dau
 */
public class ExportMovieDialog extends JDialog implements ChangeListener, ActionListener, DocumentListener, ViewListener, ShowableDialog, MouseWheelListener {

    private static final long serialVersionUID = 1L;

    private static final AspectRatio[] aspectRatioPresets = { new AspectRatio(1, 1), new AspectRatio(4, 3), new AspectRatio(16, 9), new AspectRatio(16, 10), new AspectRatio(0, 0) };

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 200;
    private int tileWidth;
    private int tileHeight;

    private static final String SETTING_RATIO = "export.aspect.ratio";
    private static final String SETTING_IMG_WIDTH = "export.image.width";
    private static final String SETTING_IMG_HEIGHT = "export.image.height";
    private static final String SETTING_TOTAL_HEIGHT = "export.total.height";
    private static final String SETTING_SPEED = "export.speed";
    private static final String SETTING_FULL_QUALITY = "export.full.quality";
    private static final String SETTING_TRACKING = "export.tracking";
    private static final String SETTING_SOFT_SBTL = "export.soft.subtitle";
    private static final String SETTING_HARD_SBTL = "export.hard.subtitle";
    private static final String SETTING_HARD_SBTL_RATIO = "export.subtitle.aspect.ratio";
    private static final String SOFTWARE_MODUS_SETTING = "export.software.rendering";
    private static final String SETTING_TILE_WIDTH = "export.tile.width";
    private static final String SETTING_TILE_HEIGHT = "export.tile.height";

    private static final int READER_ERROR_LIMIT = 3;

    private JComboBox aspectRatioSelection;
    private Map<View, File> subtitleFiles;
    private File subtitleFileAll;
    private Writer subtitleWriterAll;
    private Map<View, Writer> subtitleWriters;
    private String txtTargetFile;
    private JFormattedTextField txtImageWidth, txtImageHeight, txtTotalHeight;
    private MainImagePanel imagePanel;
    private JSpinner speedSpinner;
    private JCheckBox loadFirstCheckBox;
    private JCheckBox useDifferentialRotationTracking;
    private JCheckBox embedSoftSubtitle;
    private JCheckBox embedHardSubtitle;
    private JCheckBox embedHardSubtitleAspectRatio;
    private JButton cmdExport, cmdCancel;
    private JProgressBar progressBar;
    private JComboBox layerSelection;
    private JButton zoom1to1;
    private JButton zoomFit;
    private JSpinner zoomSpinner;

    private JDialog glRenderingDialog;

    private MovieView masterMovieView;
    private int linkedMovieManagerInstance = -1;
    private LinkedList<JP2ImageOriginalParent> jp2ImageOriginalParents;
    private View topmostView;
    private GLComponentView glComponentView;
    private HashMap<TimedMovieView, StatusStruct> currentViewStatus = new HashMap<TimedMovieView, StatusStruct>();
    private Thread exportThread;
    private Thread initThread;
    private int currentFrame = 0;
    private boolean exportFinished = false;
    private boolean initializationDone;
    private MovieFileFilter selectedOutputFormat = new MP4Filter();
    private int hardSubtitleFontSizeFactor = 20;

    private HashMap<JComponent, Boolean> enableState;
    private List<JComponent> guiElements;

    private Semaphore viewChangedSemaphore = new Semaphore(1);

    private BufferedImage output;

    private ZoomController zoomController = new ZoomController();

    private boolean savedDoubleBufferinOption = J2KRenderGlobalOptions.getDoubleBufferingOption();

    private HashMap<JHVJP2View, Integer> readerErrorCounter = new HashMap<JHVJP2View, Integer>();

    private class StatusStruct {
        public ImmutableDateTime currentDateTime = new ImmutableDateTime(0);
        public boolean regionHasUpdated = true;
        public long lastRegionUpdateId = -1;
        public long lastTimestampUpdateId = -1;
    }

    private class JP2ImageOriginalParent {
        public JP2Image jp2Image;
        public JHVJP2View originalParent;

        public JP2ImageOriginalParent(JP2Image _jp2Image, JHVJP2View _parent) {
            jp2Image = _jp2Image;
            originalParent = _parent;
        }
    }

    /**
     * Default constructor
     */
    public ExportMovieDialog() {
        super(ImageViewerGui.getMainFrame(), "Export Movie", true);
        Log.info("Initialize movie export dialog");
        initializationDone = false;

        guiElements = new ArrayList<JComponent>();

        LayeredView layeredView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(LayeredView.class);
        if (layeredView.getNumberOfVisibleLayer() == 0) {
            Message.err("No visible layers!", "There are no visible layers loaded which can be exported", false);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    dispose();
                }
            });
        } else {
            Log.debug(">> ExportMovieDialog() > Pause movies");
            for (int i = 0; i < layeredView.getNumLayers(); i++) {
                MovieView movieView = layeredView.getLayer(i).getAdapter(MovieView.class);
                if (movieView != null) {
                    movieView.pauseMovie();
                }
            }

            Log.debug(">> ExportMovieDialog() > Start initializing GUI");
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    exportFinished = true;
                    finish();
                }
            });

            setLayout(new BorderLayout());
            setResizable(false);

            // Parameters

            JPanel parameterPanel = new JPanel(new GridBagLayout());
            parameterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            parameterPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(2, 2, 2, 2);

            // Target File
            // c.weightx = 1.0;

            txtTargetFile = new String(JHVDirectory.EXPORTS.getPath() + "JHV_movie_created_");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
            txtTargetFile += dateFormat.format(new Date());
            txtTargetFile += selectedOutputFormat.getExtension();

            // Viewchain

            // Copy main view chain

            Log.debug(">> ExportMovieDialog() > Create preview viewchain");

            ViewchainFactory viewChainFactory;
            ComponentView componentView;
            ComponentView mainComponentView = ImageViewerGui.getSingletonInstance().getMainView();
            if (mainComponentView instanceof GLComponentView) {
                ((GLComponentView) mainComponentView).stopAnimation();
            }
            if (!Boolean.parseBoolean(Settings.getSingletonInstance().getProperty(SOFTWARE_MODUS_SETTING)) && GLInfo.glIsUsable()) {
                Log.info("Export movie rendering modus: OpenGL");
                // Create view chain factory
                viewChainFactory = new ViewchainFactory(new GLViewFactory());
                componentView = glComponentView = (GLComponentView) viewChainFactory.createViewchainMain(mainComponentView, true);
            } else {
                Log.info("Export movie rendering modus: Software");
                viewChainFactory = new ViewchainFactory(true);
                componentView = viewChainFactory.createViewchainMain(mainComponentView, true);
            }

            Log.debug(">> ExportMovieDialog() > Delete invisible layers from preview view chain");
            // Delete invisible layers
            LayeredView mainLayeredView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(LayeredView.class);
            LayeredView exportLayeredView = componentView.getAdapter(LayeredView.class);

            for (int i = mainLayeredView.getNumLayers() - 1; i >= 0; i--) {
                if (!mainLayeredView.isVisible(mainLayeredView.getLayer(i))) {
                    exportLayeredView.removeLayer(i);
                }
            }

            // Remember and change parent views of all JP2Images
            Log.debug(">> ExportMovieDialog() > Relink jp2 images to preview view chain");

            jp2ImageOriginalParents = new LinkedList<JP2ImageOriginalParent>();

            for (int i = 0; i < exportLayeredView.getNumLayers(); i++) {
                JHVJP2View jp2View = exportLayeredView.getLayer(i).getAdapter(JHVJP2View.class);
                if (jp2View != null) {
                    JP2Image jp2Image = jp2View.getJP2Image();
                    jp2ImageOriginalParents.add(new JP2ImageOriginalParent(jp2Image, jp2Image.getParentView()));
                    jp2Image.setParentView(jp2View);
                }
            }
            this.topmostView = componentView.getView();

            // Layer selection and zoom 1:1 / zoom to fit

            JPanel zoomPanel = new JPanel();

            layerSelection = new JComboBox();
            for (int i = 0; i < exportLayeredView.getNumLayers(); ++i) {
                layerSelection.addItem(exportLayeredView.getLayer(i).getAdapter(JHVJP2View.class).getName());
            }
            zoom1to1 = new JButton("Zoom 1:1");
            zoomFit = new JButton("Zoom to Fit");
            zoomSpinner = new JSpinner(new SpinnerNumberModel(new Double(1), new Double(0.01), null, new Double(0.01f)));
            JSpinner.NumberEditor editor = new JSpinner.NumberEditor(zoomSpinner, "0%");
            zoomSpinner.setEditor(editor);
            zoom1to1.addActionListener(this);
            zoomFit.addActionListener(this);
            zoomSpinner.addChangeListener(this);
            zoomSpinner.addMouseWheelListener(this);
            layerSelection.addActionListener(this);
            guiElements.add(zoomSpinner);
            guiElements.add(layerSelection);
            guiElements.add(zoom1to1);
            guiElements.add(zoomFit);

            c.gridy = 1;
            c.gridwidth = 1;
            c.weightx = 0;
            parameterPanel.add(zoom1to1, c);
            parameterPanel.add(zoomFit, c);
            c.gridwidth = 2;
            zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.X_AXIS));
            zoomPanel.add(new JLabel("Zoom:   "));
            zoomPanel.add(zoomSpinner);
            zoomPanel.add(new JLabel("  of Layer"));
            parameterPanel.add(layerSelection, c);
            c.gridwidth = 1;
            c.weightx = 1;
            parameterPanel.add(new JLabel(" "), c);
            c.weightx = 0;
            parameterPanel.add(zoomPanel, c);
            // Image Panel
            Log.debug(">> ExportMovieDialog() > Create preview image panel");
            JPanel imagePanelContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            imagePanel = new MainImagePanel();
            imagePanel.setUpdateViewportView(false);
            imagePanel.setAutoscrolls(true);
            imagePanel.setFocusable(false);
            imagePanel.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            MainImagePanelMousePanController mainImagePanelMousePanController = new MainImagePanelMousePanController();
            imagePanel.setInputController(mainImagePanelMousePanController);
            imagePanel.setLoading(false);
            imagePanelContainer.add(imagePanel);
            imagePanel.setView(componentView);
            imagePanel.removeMouseWheelListener(mainImagePanelMousePanController);
            imagePanel.addMouseWheelListener(this);
            c.gridy++;
            c.weightx = 0;
            c.gridwidth = 2;
            c.gridheight = 8;
            parameterPanel.add(imagePanelContainer, c);

            c.gridheight = 1;

            // Image dimensions
            c.gridwidth = 2;

            aspectRatioSelection = new JComboBox(aspectRatioPresets);
            aspectRatioSelection.addActionListener(this);
            parameterPanel.add(aspectRatioSelection, c);
            c.gridwidth = 1;
            parameterPanel.add(new JLabel(), c);
            parameterPanel.add(new JLabel("Aspect ratio:"), c);
            guiElements.add(aspectRatioSelection);

            // Image Width
            c.gridy++;
            c.gridwidth = 2;
            txtImageWidth = new JFormattedTextField(new Integer(640));
            txtImageWidth.getDocument().addDocumentListener(this);
            txtImageWidth.addActionListener(this);
            ((NumberFormat) ((NumberFormatter) txtImageWidth.getFormatter()).getFormat()).setGroupingUsed(false);
            parameterPanel.add(txtImageWidth, c);
            c.gridwidth = 1;
            c.weightx = 1;
            parameterPanel.add(new JLabel(), c);
            c.weightx = 0;
            parameterPanel.add(new JLabel("Image Width:"), c);
            guiElements.add(txtImageWidth);

            // Image Height
            c.gridy++;
            txtImageHeight = new JFormattedTextField(new Integer(640));
            txtImageHeight.getDocument().addDocumentListener(this);
            txtImageHeight.addActionListener(this);
            ((NumberFormat) ((NumberFormatter) txtImageHeight.getFormatter()).getFormat()).setGroupingUsed(false);
            c.gridwidth = 2;
            parameterPanel.add(txtImageHeight, c);
            c.gridwidth = 1;
            c.weightx = 1;
            parameterPanel.add(new JLabel(), c);
            c.weightx = 0;
            parameterPanel.add(new JLabel("Image Height:"), c);
            guiElements.add(txtImageHeight);

            // Total height
            c.gridy++;
            txtTotalHeight = new JFormattedTextField(new Integer(640));
            txtTotalHeight.setEnabled(false);
            txtTotalHeight.getDocument().addDocumentListener(this);
            txtTotalHeight.addActionListener(this);
            ((NumberFormat) ((NumberFormatter) txtTotalHeight.getFormatter()).getFormat()).setGroupingUsed(false);
            c.gridwidth = 2;
            parameterPanel.add(txtTotalHeight, c);
            c.gridwidth = 1;
            c.weightx = 1;
            parameterPanel.add(new JLabel(), c);
            c.weightx = 0;
            parameterPanel.add(new JLabel("Total Height With Subtitles:"), c);
            guiElements.add(txtTotalHeight);

            // Speed
            c.gridy++;
            c.gridwidth = 2;
            speedSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 99, 1));
            speedSpinner.addMouseWheelListener(this);
            parameterPanel.add(speedSpinner, c);
            c.gridwidth = 1;
            c.weightx = 1;
            parameterPanel.add(new JLabel(), c);
            c.weightx = 0;
            parameterPanel.add(new JLabel("Speed (fps):"), c);
            guiElements.add(speedSpinner);

            c.gridy++;
            parameterPanel.add(new JLabel(" "), c);

            Log.debug(">> ExportMovieDialog() > Set initial region and zoom of preview");

            MetaData metaData = imagePanel.getView().getAdapter(MetaDataView.class).getMetaData();
            Region initialRegion = ViewHelper.cropInnerRegionToOuterRegion(metaData.getPhysicalRegion(), ImageViewerGui.getSingletonInstance().getMainView().getAdapter(RegionView.class).getRegion());
            initialRegion = ViewHelper.contractRegionToViewportAspectRatio(imagePanel.getViewport(), initialRegion, metaData);
            imagePanel.getView().getAdapter(RegionView.class).setRegion(initialRegion, new ChangeEvent());

            zoomController.setImagePanel(imagePanel);

            // Load First = Load all data via JPIP before exporting
            c.gridy++;
            c.gridwidth = 4;
            loadFirstCheckBox = new JCheckBox("Download full quality before export", false);
            parameterPanel.add(loadFirstCheckBox, c);
            guiElements.add(loadFirstCheckBox);

            // Use differential rotation tracking
            c.gridy++;
            c.gridwidth = 6;
            StandardSolarRotationTrackingView trackingView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(StandardSolarRotationTrackingView.class);
            useDifferentialRotationTracking = new JCheckBox("Use differential rotation tracking", trackingView != null && trackingView.getEnabled());
            parameterPanel.add(useDifferentialRotationTracking, c);
            guiElements.add(useDifferentialRotationTracking);

            // Soft subtitle
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 6;
            embedSoftSubtitle = new JCheckBox("Embed soft subtitle (can be turned on and off during playback)", true);
            parameterPanel.add(embedSoftSubtitle, c);
            guiElements.add(embedSoftSubtitle);

            // Hard subtitle
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 6;
            embedHardSubtitle = new JCheckBox("Embed hard subtitle (can NOT be turned on and off during playback)", false);
            embedHardSubtitle.addActionListener(this);
            parameterPanel.add(embedHardSubtitle, c);
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 6;
            embedHardSubtitleAspectRatio = new JCheckBox("Use total movie height instead of image height for aspect ratio", false);
            embedHardSubtitleAspectRatio.setEnabled(false);
            embedHardSubtitleAspectRatio.addActionListener(this);
            guiElements.add(embedHardSubtitle);
            guiElements.add(embedHardSubtitleAspectRatio);

            parameterPanel.add(embedHardSubtitleAspectRatio, c);

            // Stretch
            c.gridy++;
            c.weighty = 1.0;
            parameterPanel.add(Box.createVerticalGlue(), c);

            // Buttons
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));

            cmdExport = new JButton("Export");
            cmdExport.addActionListener(this);
            guiElements.add(cmdExport);

            cmdCancel = new JButton("Cancel");
            cmdCancel.addActionListener(this);
            guiElements.add(cmdCancel);

            buttonPane.add(Box.createHorizontalGlue());

            // Order depends on operation system
            if (System.getProperty("os.name").toUpperCase().contains("WIN")) {
                buttonPane.add(cmdExport);
                buttonPane.add(cmdCancel);
            } else {
                buttonPane.add(cmdCancel);
                buttonPane.add(cmdExport);
            }

            c.gridy++;
            parameterPanel.add(buttonPane, c);

            add(parameterPanel);
            enableState = new HashMap<JComponent, Boolean>();

            // Progressbar
            progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
            add(progressBar, BorderLayout.SOUTH);
            Log.debug(">> ExportMovieDialog() > Disable GUI until init is finished");
            disableGUI();
            initThread = new Thread(new Runnable() {
                public void run() {
                    initViewchain();
                    Log.debug(">> ExportMovieDialog() > Viewchain initialization is finished");
                    loadExportSettings();
                    zoomSpinner.setValue(getCurrentZoom());
                    Log.debug(">> ExportMovieDialog() > Settings loaded");
                    enableGUI();
                }
            }, "InitMovieExport");
            initThread.start();
        }
    }

    private void disableGUI() {
        for (JComponent comp : guiElements) {
            enableState.put(comp, comp.isEnabled());
            comp.setEnabled(false);
        }
    }

    private void enableGUI() {
        for (JComponent comp : guiElements) {
            if (!comp.isEnabled())
                comp.setEnabled(enableState.get(comp));
        }
    }

    private void saveExportSettings() {
        Settings settings = Settings.getSingletonInstance();
        settings.setProperty(SETTING_RATIO, aspectRatioSelection.getSelectedItem().toString());
        settings.setProperty(SETTING_IMG_WIDTH, txtImageWidth.getValue().toString());
        settings.setProperty(SETTING_IMG_HEIGHT, txtImageHeight.getValue().toString());
        settings.setProperty(SETTING_TOTAL_HEIGHT, txtTotalHeight.getValue().toString());
        settings.setProperty(SETTING_SPEED, speedSpinner.getValue().toString());
        settings.setProperty(SETTING_FULL_QUALITY, Boolean.toString(loadFirstCheckBox.isSelected()));
        settings.setProperty(SETTING_TRACKING, Boolean.toString(useDifferentialRotationTracking.isSelected()));
        settings.setProperty(SETTING_SOFT_SBTL, Boolean.toString(embedSoftSubtitle.isSelected()));
        settings.setProperty(SETTING_HARD_SBTL, Boolean.toString(embedHardSubtitle.isSelected()));
        settings.setProperty(SETTING_HARD_SBTL_RATIO, Boolean.toString(embedHardSubtitleAspectRatio.isSelected()));
        settings.save();
    }

    private void loadExportSettings() {
        Settings settings = Settings.getSingletonInstance();
        String val;

        try {
            val = settings.getProperty(SETTING_RATIO);
            if (val != null && !val.isEmpty()) {
                int width, height;
                if (val.equals("Custom")) {
                    width = height = 0;
                } else {
                    String[] parts = val.split(" : ");
                    width = Integer.parseInt(parts[0]);
                    height = Integer.parseInt(parts[1]);
                }
                for (int i = 0; i < aspectRatioPresets.length; ++i) {
                    if (aspectRatioPresets[i].width == width && aspectRatioPresets[i].height == height) {
                        aspectRatioSelection.setSelectedItem(aspectRatioPresets[i]);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_SPEED);
            if (val != null && !val.isEmpty()) {
                speedSpinner.setValue(Integer.parseInt(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_FULL_QUALITY);
            if (val != null && !val.isEmpty()) {
                loadFirstCheckBox.setSelected(Boolean.parseBoolean(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_TRACKING);
            if (val != null && !val.isEmpty()) {
                useDifferentialRotationTracking.setSelected(Boolean.parseBoolean(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_SOFT_SBTL);
            if (val != null && !val.isEmpty()) {
                embedSoftSubtitle.setSelected(Boolean.parseBoolean(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_HARD_SBTL);
            if (val != null && !val.isEmpty()) {
                embedHardSubtitle.setSelected(Boolean.parseBoolean(val));
                if (embedHardSubtitle.isSelected()) {
                    embedHardSubtitleAspectRatio.setEnabled(true);
                    txtTotalHeight.setEnabled(true);
                }
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_HARD_SBTL_RATIO);
            if (val != null && !val.isEmpty()) {
                embedHardSubtitleAspectRatio.setSelected(Boolean.parseBoolean(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_IMG_HEIGHT);
            if (val != null && !val.isEmpty()) {
                txtImageHeight.setValue(Math.round(Float.parseFloat(val)));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_TOTAL_HEIGHT);
            if (val != null && !val.isEmpty()) {
                txtTotalHeight.setValue(Math.round(Float.parseFloat(val)));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_IMG_WIDTH);
            if (val != null && !val.isEmpty()) {
                txtImageWidth.setValue(Math.round(Float.parseFloat(val)));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_TILE_WIDTH);
            if (val != null && !val.isEmpty()) {
                tileWidth = Math.round(Float.parseFloat(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

        try {
            val = settings.getProperty(SETTING_TILE_HEIGHT);
            if (val != null && !val.isEmpty()) {
                tileHeight = Math.round(Float.parseFloat(val));
            }
        } catch (Throwable t) {
            Log.error(t);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == txtImageHeight || e.getSource() == txtImageWidth || e.getSource() == txtTotalHeight) {
            updateViewport(((JFormattedTextField) e.getSource()).getDocument());
        }
        // Start export
        else if (e.getSource() == cmdExport) {

            // Open save-dialog
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileHidingEnabled(false);
            fileChooser.setCurrentDirectory(JHVDirectory.EXPORTS.getFile());
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(new MOVFilter());
            fileChooser.addChoosableFileFilter(new MP4Filter());
            fileChooser.addChoosableFileFilter(new JPEGSetFilter());
            fileChooser.addChoosableFileFilter(new PNGSetFilter());
            fileChooser.addChoosableFileFilter(new JPEGArchiveFilter());
            fileChooser.addChoosableFileFilter(new PNGArchiveFilter());

            // if txtTargetFile's set the selectedOutputFormat and fileChooser's
            // filter according to txtTargetFile's extension
            if (txtTargetFile.toLowerCase().endsWith(selectedOutputFormat.getExtension())) {
                for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()) {
                    if (fileFilter.getClass() == selectedOutputFormat.getClass()) {
                        fileChooser.setFileFilter(fileFilter);
                        selectedOutputFormat = (MovieFileFilter) fileFilter;
                    }
                }
            } else {
                for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()) {
                    if (txtTargetFile.endsWith(((MovieFileFilter) fileFilter).getExtension())) {
                        fileChooser.setFileFilter(fileFilter);
                        selectedOutputFormat = (MovieFileFilter) fileFilter;
                    }
                }
            }

            txtTargetFile = txtTargetFile.substring(0, txtTargetFile.lastIndexOf(selectedOutputFormat.getExtension()));

            fileChooser.setSelectedFile(new File(txtTargetFile));
            int retVal = fileChooser.showDialog(ImageViewerGui.getMainFrame(), "OK");

            if (retVal == JFileChooser.APPROVE_OPTION) {
                String selectedFile = fileChooser.getSelectedFile().toString();

                MovieFileFilter newFilter = (MovieFileFilter) fileChooser.getFileFilter();

                // Prefer extension in text field over file filter dialog
                // selection
                for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()) {
                    MovieFileFilter movieFileFilter = (MovieFileFilter) fileFilter;
                    if (selectedFile.endsWith(movieFileFilter.getExtension())) {
                        newFilter = movieFileFilter;
                        break;
                    }
                }

                // Add new extension, if not present yet
                if (!selectedFile.endsWith(newFilter.getExtension())) {
                    selectedFile += newFilter.getExtension();
                }

                // this already has the file extension added - this is not the
                // case if the operation gets canceled by the user
                txtTargetFile = selectedFile;
                selectedOutputFormat = newFilter;

                if (exportThread != null)
                    return;
                saveExportSettings();

                exportThread = new Thread(new Runnable() {
                    public void run() {
                        exportMovie();
                    }
                }, "ExportMovie");
                exportThread.start();
            } else {
                // add the extension again, since we removed it before
                txtTargetFile = txtTargetFile + selectedOutputFormat;
                Log.info("export save file canceled");
            }

            // Cancel export if running, close dialog in any case
        } else if (e.getSource() == cmdCancel) {
            exportFinished = true;
            new Thread() {
                public void run() {
                    finish();
                }
            }.start();
        } else if (e.getSource() == aspectRatioSelection) {
            AspectRatio aspectRatio = (AspectRatio) aspectRatioSelection.getSelectedItem();
            if (aspectRatio.getWidth() != 0) {
                txtImageWidth.setValue(txtImageWidth.getValue());
            }
        } else if (e.getSource() == embedHardSubtitle) {
            txtTotalHeight.setEnabled(embedHardSubtitle.isSelected());
            embedHardSubtitleAspectRatio.setEnabled(embedHardSubtitle.isSelected());
            txtImageWidth.setValue(txtImageWidth.getValue());
        } else if (e.getSource() == embedHardSubtitleAspectRatio) {
            if (embedHardSubtitle.isSelected()) {
                txtImageHeight.setValue(txtImageHeight.getValue());
            }
        } else if (e.getSource() == zoom1to1 && topmostView.getAdapter(LayeredView.class).getLayer(layerSelection.getSelectedIndex()) != null) {
            zoomController.zoom1to1(topmostView, topmostView.getAdapter(LayeredView.class).getLayer(layerSelection.getSelectedIndex()), StaticViewport.createAdaptedViewport((Integer) txtImageWidth.getValue(), (Integer) txtImageHeight.getValue()));
            zoomSpinner.setValue(1.0);
        } else if (e.getSource() == zoomFit  && topmostView.getAdapter(LayeredView.class).getLayer(layerSelection.getSelectedIndex()) != null) {
            zoomController.zoomFit(topmostView.getAdapter(LayeredView.class).getLayer(layerSelection.getSelectedIndex()).getAdapter(MetaDataView.class), topmostView.getAdapter(RegionView.class));
            zoomSpinner.setValue(getCurrentZoom());
        } else if (e.getSource() == layerSelection) {
            zoomSpinner.setValue(getCurrentZoom());
        }
    }

    /**
     * Updates the zoom after the zoom spinner has changed.
     */
    public void stateChanged(javax.swing.event.ChangeEvent e) {
        if (e.getSource() == zoomSpinner) {
            double zoomSpinnerValue = (Double) zoomSpinner.getValue();
            zoomController.zoom(topmostView, zoomSpinnerValue / getCurrentZoom());
        }
    }

    /**
     * Update viewport after text fields (width, height, total height) have
     * changed
     */
    public void insertUpdate(DocumentEvent e) {
        updateViewport(e.getDocument());
    }

    /**
     * Update viewport after text fields (width, height, total height) have
     * changed
     */
    public void removeUpdate(DocumentEvent e) {
        updateViewport(e.getDocument());
    }

    public void changedUpdate(DocumentEvent evt) {

    }

    // Little Hack in order to avoid cyclic updating in updateViewport()
    // of the text fields.
    // Otherwise one gets an illegal state exception
    private boolean updatingImageHeight = false;
    private boolean updatingImageWidth = false;
    private boolean updatingTotalHeight = false;

    /**
     * Update viewport after text fields (width, height, total height) have
     * changed
     * 
     * @param document
     *            The Document object corresponding to the text field
     */
    private synchronized void updateViewport(Document document) {
        calculateViewport(document);
        int width = (Integer) txtImageWidth.getValue();
        int height = (Integer) txtImageHeight.getValue();
        if (width > height) {
            topmostView.getAdapter(ViewportView.class).setViewport(StaticViewport.createAdaptedViewport(imagePanel.getView().getComponent().getWidth(), (int) Math.round((double) imagePanel.getView().getComponent().getHeight() * height / width)), new ChangeEvent());
        } else {
            topmostView.getAdapter(ViewportView.class).setViewport(StaticViewport.createAdaptedViewport((int) Math.round((double) imagePanel.getView().getComponent().getWidth() * width / height), imagePanel.getView().getComponent().getHeight()), new ChangeEvent());
        }
        zoomSpinner.setValue(getCurrentZoom());
    }

    private synchronized void calculateViewport(Document document) {
        if (document == txtImageWidth.getDocument()) {
            updatingImageWidth = true;
            AspectRatio aspectRatio = (AspectRatio) aspectRatioSelection.getSelectedItem();
            int width = (Integer) txtImageWidth.getValue();
            int height = (Integer) txtImageHeight.getValue();
            if (aspectRatio.getWidth() != 0) {
                height = (int) Math.ceil((double) width / aspectRatio.width * aspectRatio.height);
                if (embedHardSubtitle.isSelected() && embedHardSubtitleAspectRatio.isSelected()) {
                    if (!updatingTotalHeight) {
                        txtTotalHeight.setValue(height);
                    }
                } else {
                    if (!updatingImageHeight) {
                        txtImageHeight.setValue(height);
                    }
                }
            }
            updatingImageWidth = false;
        } else if (document == txtImageHeight.getDocument()) {
            updatingImageHeight = true;
            AspectRatio aspectRatio = (AspectRatio) aspectRatioSelection.getSelectedItem();
            int height = (Integer) txtImageHeight.getValue();
            int width = (Integer) txtImageWidth.getValue();
            if (aspectRatio.getWidth() != 0) {
                if (embedHardSubtitle.isSelected() && embedHardSubtitleAspectRatio.isSelected()) {
                    if (!updatingTotalHeight) {
                        double factor = 1.0 / (1.0 - (double) aspectRatio.getWidth() * Math.log(1 + topmostView.getAdapter(LayeredView.class).getNumberOfVisibleLayer()) / aspectRatio.getHeight() / hardSubtitleFontSizeFactor);
                        txtTotalHeight.setValue((int) Math.floor(factor * height));
                    }
                } else {
                    width = (int) Math.floor((double) height / aspectRatio.height * aspectRatio.width);
                    if (!updatingImageWidth) {
                        txtImageWidth.setValue(width);
                    }
                    if (!updatingTotalHeight) {
                        int totalHeight = (int) ((Integer) txtImageHeight.getValue() + (Integer) txtImageWidth.getValue() * Math.log(1 + topmostView.getAdapter(LayeredView.class).getNumberOfVisibleLayer()) / hardSubtitleFontSizeFactor);
                        txtTotalHeight.setValue(totalHeight);
                    }
                }
            } else {
                if (!updatingTotalHeight) {
                    int totalHeight = (int) ((Integer) txtImageHeight.getValue() + (Integer) txtImageWidth.getValue() * Math.log(1 + topmostView.getAdapter(LayeredView.class).getNumberOfVisibleLayer()) / hardSubtitleFontSizeFactor);
                    txtTotalHeight.setValue(totalHeight);
                }
            }
            updatingImageHeight = false;
        } else if (document == txtTotalHeight.getDocument()) {
            updatingTotalHeight = true;
            if (embedHardSubtitle.isSelected()) {
                AspectRatio aspectRatio = (AspectRatio) aspectRatioSelection.getSelectedItem();
                if (aspectRatio.getWidth() != 0 && embedHardSubtitleAspectRatio.isSelected()) {
                    int width = (int) Math.floor((Integer) txtTotalHeight.getValue() / (double) aspectRatio.height * aspectRatio.width);
                    if (!updatingImageWidth) {
                        txtImageWidth.setValue(width);
                    }
                    if (!updatingImageHeight) {
                        int height = (int) Math.ceil(((Integer) txtTotalHeight.getValue()) - (Integer) txtImageWidth.getValue() * Math.log(1 + topmostView.getAdapter(LayeredView.class).getNumberOfVisibleLayer()) / (double) hardSubtitleFontSizeFactor);
                        txtImageHeight.setValue(height);
                    }
                } else {
                    if (aspectRatio.getWidth() == 0) {
                        if (!updatingImageHeight) {
                            int height = (int) Math.ceil(((Integer) txtTotalHeight.getValue()) - (Integer) txtImageWidth.getValue() * Math.log(1 + topmostView.getAdapter(LayeredView.class).getNumberOfVisibleLayer()) / (double) hardSubtitleFontSizeFactor);
                            txtImageHeight.setValue(height);
                        }
                    } else {
                        if (!updatingImageHeight) {
                            double factor = 1.0 / (1.0 + (double) aspectRatio.getWidth() * Math.log(1 + topmostView.getAdapter(LayeredView.class).getNumberOfVisibleLayer()) / aspectRatio.getHeight() / hardSubtitleFontSizeFactor);
                            txtImageHeight.setValue((int) Math.ceil(factor * (Integer) txtTotalHeight.getValue()));
                        }
                    }
                }
            }
            updatingTotalHeight = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void viewChanged(View sender, ChangeEvent aEvent) {
        if (viewChangedSemaphore.tryAcquire()) {
            try {
                // If the view chain is not initialized yet or the export is
                // already
                // complete, return
                if (topmostView == null || exportFinished || !initializationDone) {
                    return;
                }

                if (aEvent.reasonOccurred(ReaderErrorReason.class)) {
                    ReaderErrorReason errorReason = aEvent.getLastChangedReasonByType(ReaderErrorReason.class);
                    if (readerErrorCounter.containsKey(errorReason.getJHVJP2View())) {
                        int errorCount = readerErrorCounter.get(errorReason.getJHVJP2View());
                        if (errorCount >= READER_ERROR_LIMIT - 1) {
                            readerErrorCounter.put(errorReason.getJHVJP2View(), 0);
                            showReaderErrorPopup(errorReason.getJHVJP2View());
                        } else {
                            readerErrorCounter.put(errorReason.getJHVJP2View(), errorCount + 1);
                        }
                        return;
                    }
                }

                // Update meta data list
                // TimestampChangedReasons are used to determine if a frame has
                // been
                // completely loaded
                // RegionChangedReasons are used in tracking mode to determine
                // if the
                // region shift is complete

                for (TimedMovieView movieView : currentViewStatus.keySet()) {
                    TimestampChangedReason timestampChangedReason = aEvent.getLastChangedReasonByTypeAndView(TimestampChangedReason.class, movieView);
                    RegionUpdatedReason regionUpdatedReason = aEvent.getLastChangedReasonByTypeAndView(RegionUpdatedReason.class, movieView);
                    StatusStruct status = currentViewStatus.get(movieView);

                    if (timestampChangedReason != null && timestampChangedReason.getId() > status.lastTimestampUpdateId) {
                        status.currentDateTime = timestampChangedReason.getNewDateTime();
                        status.lastTimestampUpdateId = timestampChangedReason.getId();
                    }
                    if (regionUpdatedReason != null && regionUpdatedReason.getId() > status.lastRegionUpdateId) {
                        status.regionHasUpdated = true;
                        status.lastRegionUpdateId = regionUpdatedReason.getId();
                    }
                }
                // Check, if all regions are up to date
                for (StatusStruct status : currentViewStatus.values()) {
                    if (!status.regionHasUpdated) {
                        return;
                    }
                }

                // Compare all time stamps from the meta data (which are updated
                // after
                // decoding the image
                // with all time stamps from the movie view (which are updated
                // immediately, thus are out of sync
                // a little). If the are not equal, the decoder is still
                // running, thus
                // the frame is not ready
                // for being exported.
                for (TimedMovieView view : currentViewStatus.keySet()) {
                    if (view.getCurrentFrameDateTime().compareTo(currentViewStatus.get(view).currentDateTime) != 0) {
                        return;
                    }
                }

                for (JHVJP2View jp2View : readerErrorCounter.keySet()) {
                    readerErrorCounter.put(jp2View, 0);
                }

                // Get frame
                BufferedImage input;
                if (glComponentView != null) {
                    input = glComponentView.getBufferedImage();
                } else {
                    input = ((JavaBufferedImageData) topmostView.getAdapter(SubimageDataView.class).getSubimageData()).getBufferedImage();
                }
                if (input == null) {
                    return;
                }

                // Convert to RGB image
                int hardSubtitleOffsetY = input.getHeight();
                int hardSubtitleOffsetX = 5;

                int totalHeight = embedHardSubtitle.isSelected() ? (Integer) txtTotalHeight.getValue() : input.getHeight();
                if (output == null || output.getWidth() != input.getWidth() || output.getHeight() != totalHeight) {
                    output = new BufferedImage(input.getWidth(), totalHeight, BufferedImage.TYPE_3BYTE_BGR);
                }

                // Fill background with black color (in case the frame has
                // opaque areas)

                Graphics2D g = output.createGraphics();
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, output.getWidth(), output.getHeight());
                g.drawImage(input, null, 0, 0);
                int hardSubtitleFontSize = Math.max(1, (output.getHeight() - input.getHeight() - 5) / topmostView.getAdapter(LayeredView.class).getNumberOfVisibleLayer());
                g.setFont(new Font("Arial", Font.BOLD, hardSubtitleFontSize));
                g.setColor(Color.WHITE);

                // Write subtitles
                String frameNumberText = "{" + (currentFrame - 1) + "}{" + (currentFrame - 1) + "}";
                SimpleDateFormat subtitleFormatter = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss'Z'");
                StringBuffer subtitleTextAll = new StringBuffer(frameNumberText);

                for (View view : subtitleFiles.keySet()) {
                    File subFile = subtitleFiles.get(view);
                    ImageInfoView infoView = view.getAdapter(ImageInfoView.class);
                    String subText = infoView.getName();
                    TimedMovieView movieView = view.getAdapter(TimedMovieView.class);
                    if (movieView != null) {
                        subText += " - " + subtitleFormatter.format(movieView.getCurrentFrameDateTime().getTime());
                    }
                    if (embedSoftSubtitle.isSelected()) {
                        Writer writer = subtitleWriters.get(view);
                        try {
                            writer.append(frameNumberText + subText + System.getProperty("line.separator"));
                        } catch (IOException e) {
                            Log.error("> ExportMovieDialog.viewChanged(View, ChangeEvent) >> Error writing to file: " + subFile.getAbsolutePath(), e);
                        }
                        subtitleTextAll.append(subText + "|");
                    }
                    if (embedHardSubtitle.isSelected()) {
                        hardSubtitleOffsetY += hardSubtitleFontSize;
                        g.drawString(subText, hardSubtitleOffsetX, hardSubtitleOffsetY);
                    }
                }
                if (embedSoftSubtitle.isSelected()) {
                    subtitleTextAll.replace(subtitleTextAll.length() - 1, subtitleTextAll.length(), System.getProperty("line.separator"));
                    try {
                        subtitleWriterAll.append(subtitleTextAll);
                    } catch (IOException e) {
                        Log.error("> ExportMovieDialog.viewChanged(View, ChangeEvent) >> Error writing to file: " + subtitleFileAll.getAbsolutePath(), e);
                    }
                }
                // Write the image to temporary file
                try {
                    File targetFile;
                    if (selectedOutputFormat.useTempDirectoryForIntermediateImages()) {
                        String frameNumber = Integer.toString(currentFrame);
                        while (frameNumber.length() < 4) {
                            frameNumber = "0" + frameNumber;
                        }
                        targetFile = new File(JHVDirectory.TEMP.getPath() + "frame" + frameNumber + selectedOutputFormat.getIntermediateExtension());
                    } else {
                        String frameNumber = Integer.toString(currentFrame);
                        while (frameNumber.length() < 4) {
                            frameNumber = "0" + frameNumber;
                        }
                        targetFile = new File(txtTargetFile.replace(selectedOutputFormat.getIntermediateExtension(), "_" + frameNumber + selectedOutputFormat.getIntermediateExtension()));
                    }
                    ImageIO.write(output, selectedOutputFormat.getIntermediateExtension().replace(".", ""), targetFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                currentFrame++;

                // If last frame is reached, merge temporary files to movie
                if (masterMovieView.getCurrentFrameNumber() == masterMovieView.getMaximumFrameNumber()) {
                    closeSubtitleWriters(false);
                    // If intermediate and final format are the same, nothing is
                    // left to
                    // do
                    if (selectedOutputFormat.getExtension() != selectedOutputFormat.getIntermediateExtension()) {

                        // ZIP-File: Compress intermediate files
                        if (selectedOutputFormat.getExtension().equalsIgnoreCase(".zip")) {
                            try {
                                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(txtTargetFile)));
                                out.setMethod(ZipOutputStream.DEFLATED);

                                final int bufferSize = 2048;
                                byte data[] = new byte[bufferSize];

                                File[] inputFiles = JHVDirectory.TEMP.getFile().listFiles(new FilenameFilter() {
                                    public boolean accept(File dir, String name) {
                                        return (name.contains("frame") && name.toLowerCase().endsWith(selectedOutputFormat.getIntermediateExtension()));
                                    }
                                });

                                for (File file : inputFiles) {
                                    BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(file), bufferSize);
                                    ZipEntry entry = new ZipEntry(file.getName());
                                    out.putNextEntry(entry);

                                    int count;
                                    while ((count = fileStream.read(data, 0, bufferSize)) != -1) {
                                        out.write(data, 0, count);
                                    }
                                    fileStream.close();
                                }
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // MOVIE-File: Create Movie using FFmpeg
                        } else {

                            // Build command for calling FFmpeg
                            LinkedList<String> commandLineArgs = new LinkedList<String>();
                            commandLineArgs.add("-s");
                            commandLineArgs.add(txtImageWidth.getValue() + "x" + txtImageHeight.getValue());
                            commandLineArgs.add("-sameq");
                            commandLineArgs.add("-y");
                            commandLineArgs.add("-r");
                            commandLineArgs.add(speedSpinner.getValue().toString());
                            commandLineArgs.add("-i");
                            commandLineArgs.add(JHVDirectory.TEMP.getPath() + "frame%04d.bmp");
                            commandLineArgs.add(txtTargetFile);

                            // Run FFmpeg
                            try {
                                Process p = FileUtils.invokeExecutable("ffmpeg", commandLineArgs);
                                FileUtils.logProcessOutput(p, "ffmpeg", Level.DEBUG, true);
                                p.waitFor();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            // Build command for MP4Box

                            if (FileUtils.isExecutableRegistered("mp4box")) {
                                if (embedSoftSubtitle.isSelected()) {
                                    commandLineArgs.clear();
                                    commandLineArgs.add("-ipod");
                                    commandLineArgs.add("-tmp");
                                    commandLineArgs.add(new File(txtTargetFile).getParent());
                                    commandLineArgs.add("-fps");
                                    commandLineArgs.add(speedSpinner.getValue().toString());
                                    commandLineArgs.add("-add");
                                    commandLineArgs.add(subtitleFileAll.getAbsolutePath() + ":group=2:lang=en");
                                    for (File ttxtFile : subtitleFiles.values()) {
                                        if (ttxtFile != null && ttxtFile.exists()) {
                                            commandLineArgs.add("-add");
                                            commandLineArgs.add(ttxtFile.getAbsolutePath() + ":group=2:lang=en:disabled");
                                        }
                                    }
                                    commandLineArgs.add(txtTargetFile);
                                    // Run MP4Box

                                    try {
                                        Process p = FileUtils.invokeExecutable("mp4box", commandLineArgs);
                                        FileUtils.logProcessOutput(p, "mp4box", Level.DEBUG, true);
                                        p.waitFor();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                Log.debug("> ExportMovieDialog.viewChanged(View, ChangeEvent) >> MP4Box is not installed. Skip embedding soft subtitles");
                            }
                        }
                    }

                    exportFinished = true;

                    // Close dialog. This has to happen in a different thread,
                    // because
                    // changing a view listener during viewChanged() causes an
                    // exception.
                    Timer timer = new Timer("CloseMovieExport");
                    timer.schedule(new TimerTask() {
                        public void run() {
                            finish();
                        }
                    }, 200);
                    return;
                }

                StandardSolarRotationTrackingView solarRotationTrackingView = topmostView.getAdapter(StandardSolarRotationTrackingView.class);
                if (solarRotationTrackingView != null && solarRotationTrackingView.getEnabled()) {
                    TimedMovieView masterMovieView = LinkedMovieManager.getActiveInstance().getMasterMovie();
                    if (masterMovieView.getCurrentFrameNumber() > 0) {
                        for (StatusStruct status : currentViewStatus.values()) {
                            status.regionHasUpdated = false;
                        }
                    }
                }

                masterMovieView.setCurrentFrame(masterMovieView.getCurrentFrameNumber() + 1, new ChangeEvent(), true);

                if (SwingUtilities.isEventDispatchThread()) {
                    viewChangedUpdateGUI();
                } else {
                    SwingUtilities.invokeLater(new ViewChangedUpdateGUIRunnable());
                }
            } finally {
                viewChangedSemaphore.release();
            }
        }
    }

    private void showReaderErrorPopup(JHVJP2View jp2View) {
        String msg = "There seem to be some connection problems for the following layer: " + jp2View.getName() + System.getProperty("line.separator");
        Object[] options;
        int optionType;
        if (jp2View.getReaderMode() == ReaderMode.ONLYFIREONCOMPLETE) {
            msg += "Do you want to keep trying to download the full quality data" + System.getProperty("line.separator") + "or do you want to try creating the movie with the available data (possibly not the best quality)?";
            options = new Object[] { "Use available data", "Keep trying", "Cancel export" };
            optionType = JOptionPane.YES_NO_CANCEL_OPTION;
        } else {
            msg += "Do you want to keep trying or do you want to cancel the movie export?";
            options = new Object[] { "Cancel export", "Keep trying", };
            optionType = JOptionPane.YES_NO_OPTION;
        }
        int chosenOption = JOptionPane.showOptionDialog(this, msg, "Connection problems for layer " + jp2View.getName(), optionType, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
        if (jp2View.getReaderMode() == ReaderMode.ONLYFIREONCOMPLETE) {
            if (chosenOption == 0) {
                jp2View.setReaderMode(ReaderMode.SIGNAL_RENDER_ONCE);
            } else if (chosenOption == 2) {
                exportFinished = true;
                new Thread() {
                    public void run() {
                        finish();
                    }
                }.start();
            }
        } else {
            if (chosenOption == 0) {
                exportFinished = true;
                new Thread() {
                    public void run() {
                        finish();
                    }
                }.start();
            }
        }
    }

    private void viewChangedUpdateGUI() {
        progressBar.setValue(masterMovieView == null ? progressBar.getMaximum() : masterMovieView.getCurrentFrameNumber());
    }

    private class ViewChangedUpdateGUIRunnable implements Runnable {
        public void run() {
            viewChangedUpdateGUI();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void showDialog() {

        if (!FileUtils.isExecutableRegistered("ffmpeg")) {
            Message.err("Could not find FFmpeg tool", "The FFmpeg tool could not be found. Movie export will not be available", false);
        } else {
            if (!FileUtils.isExecutableRegistered("mp4box")) {
                Message.err("Could not find MP4Box tool", "The MP4Box tool could not be found. Exported movie will not contain subtitles.", false);
            }
            pack();
            setSize(getPreferredSize());
            setLocationRelativeTo(ImageViewerGui.getMainFrame());
            setVisible(true);
        }
    }

    /**
     * Deletes all resources and closes the dialog.
     */
    synchronized public void finish() {
        System.setProperty("export.movie.debug.on", "false");
        closeSubtitleWriters(false);
        // Destroy all threads still running
        if (exportThread != null && !exportThread.equals(Thread.currentThread())) {
            exportThread.interrupt();
            exportThread = null;
        }
        if (initThread != null && !initThread.equals(Thread.currentThread())) {
            initThread.interrupt();
            initThread = null;
        }

        for (JP2ImageOriginalParent jp2ImageOriginalParent : jp2ImageOriginalParents) {
            jp2ImageOriginalParent.jp2Image.setParentView(jp2ImageOriginalParent.originalParent);
        }

        // Delete linked movie manager to unlink all movies
        if (linkedMovieManagerInstance > 0) {
            LinkedMovieManager.deleteInstance(linkedMovieManagerInstance);
            linkedMovieManagerInstance = -1;
        }

        //

        // delete view chain
        if (topmostView != null) {
            topmostView.removeViewListener(this);

            // Remove all layers from LayeredView, so that
            // JHVJP2View.abolish() is called correctly
            LayeredView layeredView = topmostView.getAdapter(LayeredView.class);
            if (layeredView != null) {
                layeredView.removeAllLayers();
            }
            topmostView = null;
        }

        masterMovieView = null;
        J2KRenderGlobalOptions.setDoubleBufferingOption(savedDoubleBufferinOption);

        if (ImageViewerGui.getSingletonInstance().getMainView() instanceof GLComponentView) {
            ((GLComponentView) ImageViewerGui.getSingletonInstance().getMainView()).startAnimation();
        }
        System.gc();

        // hide dialog
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (glRenderingDialog != null) {
                    glRenderingDialog.dispose();
                }
                dispose();
            }
        });
    }

    /**
     * Initializes the copy of the view chain.
     * 
     * This code is put into a separate function to be able to run it in a new
     * thread. As a result, the application will not block the whole time.
     */
    private void initViewchain() {
        if (glComponentView != null) {
            // Give other layers a chance to finish
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
            initializationDone = true;
        } else {
            while (!Thread.interrupted()) {

                // Get topmost view, which is not the ComponentView
                SubimageDataView topmostSubimageView = topmostView.getAdapter(SubimageDataView.class);
                // If viewport is correct, go on
                if (topmostSubimageView.getSubimageData() != null && topmostSubimageView.getSubimageData().getWidth() == imagePanel.getViewport().getWidth()) {
                    // Give other layers a chance to finish
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        return;
                    }
                    initializationDone = true;
                    break;
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    /**
     * Get the current zoom level of the currently selected layer
     * 
     * @return zoom level of the currently selected layer
     * 
     */
    private double getCurrentZoom() {
        if (topmostView != null && txtImageWidth != null && txtImageHeight != null) {
            LayeredView layeredView = topmostView.getAdapter(LayeredView.class);
            View activeView = layeredView.getLayer(layerSelection.getSelectedIndex());
            Viewport viewport = StaticViewport.createAdaptedViewport((Integer) txtImageWidth.getValue(), (Integer) txtImageHeight.getValue());
            Region outerRegion = topmostView.getAdapter(RegionView.class).getRegion();
            return ZoomController.getZoom(activeView, outerRegion, viewport);
        }
        return 0;
    }

    /**
     * Opens and prepares a subtitle file for writing
     * 
     * @param subtitleFile
     *            the subtitle file
     * @return the corresponding writer object
     * @throws IOException
     */
    private Writer openSubtitleWriter(File subtitleFile) throws IOException {
        if (subtitleFile.exists()) {
            subtitleFile.delete();
        }

        subtitleFile.createNewFile();
        subtitleFile.deleteOnExit();
        Writer writer = new BufferedWriter(new FileWriter(subtitleFile));
        return writer;
    }

    /**
     * Closes all subtitle writes
     * 
     * @param deleteFiles
     *            true, of the files should be deleted after closing
     */
    private void closeSubtitleWriters(boolean deleteFiles) {
        // Timestamps for movies as subtitles
        if (subtitleFiles != null) {
            for (View view : subtitleFiles.keySet()) {
                File subFile = subtitleFiles.get(view);
                Writer writer = subtitleWriters.get(view);
                if (writer != null) {
                    try {
                        writer.close();
                        subtitleWriters.remove(view);
                    } catch (IOException e) {
                        Log.error("> ExportMovieDialog.viewChanged(View, ChangeEvent) >> Error closing file: " + subFile.getAbsolutePath(), e);
                    }
                }
                if (deleteFiles && subFile != null) {
                    subFile.delete();
                    subtitleFiles.remove(view);
                }
            }
        }
        if (subtitleWriterAll != null) {
            try {
                subtitleWriterAll.close();
            } catch (IOException e) {
                Log.error("> ExportMovieDialog.viewChanged(View, ChangeEvent) >> Error closing file: " + subtitleFileAll.getAbsolutePath(), e);
            }
        }
        if (deleteFiles && subtitleFileAll != null) {
            subtitleFileAll.delete();
        }
    }

    /**
     * Starts exporting the movies.
     */
    private void exportMovie() {
        if (System.getProperty("export.movie.debug") != null) {
            System.setProperty("export.movie.debug.on", "true");
        }
        imagePanel.removeMouseWheelListener(this);
        imagePanel.setInputController(null);
        topmostView.removeViewListener(imagePanel.getView());
        if (glComponentView != null) {
            glComponentView.stopAnimation();

            // make copy of buffered image
            BufferedImage background = glComponentView.getBufferedImage();
            ColorModel cm = background.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = background.copyData(null);
            background = new BufferedImage(cm, raster, isAlphaPremultiplied, null);

            imagePanel.setBackgroundImage(background);
            imagePanel.setView(null);
            glComponentView.requestRebuildShaders();
            glComponentView.getComponent().setPreferredSize(new Dimension(tileWidth, tileHeight));
            glRenderingDialog = new JDialog(ImageViewerGui.getMainFrame());
            glRenderingDialog.setResizable(false);
            glRenderingDialog.add(glComponentView.getComponent());
            glRenderingDialog.pack();
            // Dialog must be made visible at least once in order for the canvas
            // to draw
            glRenderingDialog.setVisible(true);
            glRenderingDialog.setVisible(false);
        }
        savedDoubleBufferinOption = J2KRenderGlobalOptions.getDoubleBufferingOption();
        J2KRenderGlobalOptions.setDoubleBufferingOption(true);
        Viewport movieViewport = StaticViewport.createAdaptedViewport((Integer) txtImageWidth.getValue(), (Integer) txtImageHeight.getValue());
        Region movieRegion = ViewHelper.expandRegionToViewportAspectRatio(movieViewport, topmostView.getAdapter(RegionView.class).getRegion(), topmostView.getAdapter(MetaDataView.class).getMetaData());

        topmostView.getAdapter(ViewportView.class).setViewport(movieViewport, new ChangeEvent());
        topmostView.getAdapter(RegionView.class).setRegion(movieRegion, new ChangeEvent());
        // Disable all GUI elements
        layerSelection.setEnabled(false);
        zoom1to1.setEnabled(false);
        zoomFit.setEnabled(false);
        aspectRatioSelection.setEnabled(false);
        txtImageHeight.setEnabled(false);
        txtImageWidth.setEnabled(false);
        loadFirstCheckBox.setEnabled(false);
        useDifferentialRotationTracking.setEnabled(false);
        speedSpinner.setEnabled(false);
        cmdExport.setEnabled(false);
        embedSoftSubtitle.setEnabled(false);
        embedHardSubtitle.setEnabled(false);
        embedHardSubtitleAspectRatio.setEnabled(false);
        txtTotalHeight.setEnabled(false);
        zoom1to1.setEnabled(false);
        zoomFit.setEnabled(false);
        zoomSpinner.setEnabled(false);

        masterMovieView = null;
        subtitleFiles = new HashMap<View, File>();
        subtitleWriters = new HashMap<View, Writer>();

        // If selected file filter does not fit txtTargetFile, select a new one
        if (!txtTargetFile.toLowerCase().endsWith(selectedOutputFormat.getExtension())) {
            if (txtTargetFile.lastIndexOf("/") >= txtTargetFile.lastIndexOf(".")) {
                txtTargetFile = txtTargetFile + selectedOutputFormat.getExtension();
            } else {
                String extension = txtTargetFile.toLowerCase().substring(txtTargetFile.lastIndexOf("."), txtTargetFile.length());

                if (extension.equals(".mp4")) {
                    selectedOutputFormat = new MP4Filter();
                } else if (extension.equals(".mov")) {
                    selectedOutputFormat = new MOVFilter();
                } else if (extension.equals(".png")) {
                    selectedOutputFormat = new PNGSetFilter();
                } else if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                    selectedOutputFormat = new JPEGSetFilter();
                } else if (extension.equals(".zip")) {
                    selectedOutputFormat = new PNGArchiveFilter();
                }
            }
        }
        if ((selectedOutputFormat instanceof MP4Filter || selectedOutputFormat instanceof MOVFilter) && !FileUtils.isExecutableRegistered("ffmpeg")) {
            Message.err("Could not find FFmpeg tool", "The FFmpeg tool could not be found. Movie export for mp4 and mov files will not be available", false);
            return;
        }

        Log.info(">> ExportMovieDialog.exportMovie() > Delete files in temp folder");
        // Delete all files in JHV/temp
        File[] tempFiles = JHVDirectory.TEMP.getFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.contains("frame") && (name.toLowerCase().endsWith(".bmp") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"))) || (name.startsWith("export_") && (name.endsWith(".sub") || name.endsWith(".ttxt")));
            }
        });

        for (File tempFile : tempFiles) {
            tempFile.delete();
        }

        try {
            Log.info(">> ExportMovieDialog.exportMovie() > Wait for view chain initialization");
            initThread.join();
        } catch (InterruptedException e) {
            Log.warn(">> ExportMovieDialog.exportMovie() > " + "Export movie interrupted while waiting for view chain initialization", e);
            return;
        }

        // Create new linked movie manager instance
        linkedMovieManagerInstance = LinkedMovieManager.createNewInstance();
        LinkedMovieManager.setActiveInstance(linkedMovieManagerInstance);

        // Get layered view
        LayeredView layeredView = topmostView.getAdapter(LayeredView.class);

        // Subtitle - all
        subtitleFileAll = new File(JHVDirectory.TEMP.getPath() + "export_all.sub");
        try {
            subtitleWriterAll = openSubtitleWriter(subtitleFileAll);
        } catch (IOException e) {
            Log.error(">> ExportMovieDialog.exportMovie() > Could not create subtitle file: " + subtitleFileAll.getAbsolutePath(), e);
        }
        for (int i = 0; i < layeredView.getNumLayers(); i++) {
            View view = layeredView.getLayer(i);

            // Error count
            JHVJP2View jp2view = view.getAdapter(JHVJP2View.class);
            if (jp2view != null) {
                readerErrorCounter.put(jp2view, 0);
            }

            // Subtitles
            File sub = new File(JHVDirectory.TEMP.getPath() + "export_" + i + ".sub");
            try {
                Writer writer = openSubtitleWriter(sub);
                subtitleFiles.put(view, sub);
                subtitleWriters.put(view, writer);
            } catch (IOException e) {
                Log.error(">> ExportMovieDialog.exportMovie() > Could not create subtitle file: " + sub.getAbsolutePath(), e);
            }

            JHVJP2View jp2View = layeredView.getLayer(i).getAdapter(JHVJP2View.class);
            if (jp2View != null) {
                if (loadFirstCheckBox.isSelected()) {
                    jp2View.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
                    jp2View.setPersistent(true);
                } else {
                    if (!jp2View.getJP2Image().isMultiFrame()) {
                        jp2View.setReaderMode(ReaderMode.SIGNAL_RENDER_ONCE);
                    } else {
                        jp2View.setReaderMode(ReaderMode.NEVERFIRE);
                    }
                }
            }

            // Search all MovieViews
            if (masterMovieView == null && !(layeredView.getLayer(i).getAdapter(MovieView.class) instanceof TimedMovieView)) {
                masterMovieView = layeredView.getLayer(i).getAdapter(MovieView.class);

            } else {
                TimedMovieView timedMovieView = layeredView.getLayer(i).getAdapter(TimedMovieView.class);

                if (timedMovieView != null && timedMovieView.getMaximumFrameNumber() > 0) {

                    MetaData metaData = ((MetaDataView) timedMovieView).getMetaData();
                    if (metaData instanceof ObserverMetaData) {

                        while (timedMovieView.getMaximumAccessibleFrameNumber() <= 0) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }

                        currentViewStatus.put(timedMovieView, new StatusStruct());

                        // Link all movies
                        timedMovieView.linkMovie();
                        if (LinkedMovieManager.getActiveInstance().isMaster(timedMovieView)) {
                            masterMovieView = timedMovieView;
                        }
                    }

                }
            }
        }

        if (masterMovieView == null) {
            Message.warn("Warning", "Nothing to export: No layer contains an image series.");
            finish();
            return;
        }

        currentFrame = 1;

        progressBar.setMaximum(masterMovieView.getMaximumFrameNumber());

        // Wait for all layers to finish decoding their last frame
        while (true) {
            boolean allSet = true;
            for (TimedMovieView movieView : currentViewStatus.keySet()) {
                if (((ObserverMetaData) ((MetaDataView) movieView).getMetaData()).getDateTime().compareTo(movieView.getCurrentFrameDateTime()) != 0) {
                    allSet = false;
                }
            }

            if (allSet) {
                break;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Log.error(">> ExportMovieDialog.exportMovie() > Interrupted while waiting for decoding to finish.");
                return;
            }
        }
        if (useDifferentialRotationTracking.isSelected() && masterMovieView instanceof TimedMovieView) {
            StandardSolarRotationTrackingView trackingView = topmostView.getAdapter(StandardSolarRotationTrackingView.class);
            if (trackingView != null) {
                trackingView.setEnabled(true);
                trackingView.setStartDate(((TimedMovieView) masterMovieView).getFrameDateTime(0).getTime());
            }
        }

        Log.info(">> ExportMovieDialog.exportMovie() > Start movie export");

        // Start playing
        topmostView.addViewListener(this);
        masterMovieView.setCurrentFrame(0, new ChangeEvent(), true);
    }

    /**
     * A dummy to manage viewport, region and meta data during selection.
     * 
     * <p>
     * This class is only used between initializing the view chain and starting
     * the export.
     * 
     * @author Markus Langenberg
     */
    /**
     * File filter for movies, also providing the extension of the selected
     * format.
     * 
     * @author Markus Langenberg
     */
    private abstract class MovieFileFilter extends FileFilter {

        public boolean accept(File file) {
            return file.isDirectory() || acceptFile(file);
        }

        protected abstract boolean acceptFile(File f);

        /**
         * Returns the extension associated with this movie format.
         * 
         * @return Extension associated with this movie format
         */
        public abstract String getExtension();

        /**
         * Returns the extension used for the intermediate images.
         * 
         * @return Extension used for the intermediate images
         */
        public abstract String getIntermediateExtension();

        /**
         * Returns, whether intermediate images should be written to the
         * temporary directory or to the final destination.
         * 
         * @return If true, intermediate images are written to the temporary
         *         directoy
         */
        public abstract boolean useTempDirectoryForIntermediateImages();

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return getExtension();
        }
    }

    /**
     * Implementation of MovieFileFiter for Quicktime movies.
     * 
     * @author Markus Langenberg
     */
    private class MOVFilter extends MovieFileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean acceptFile(File f) {
            return f.getName().toLowerCase().endsWith(".mov");
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Quicktime (.mov)";
        }

        /**
         * {@inheritDoc}
         */
        public String getExtension() {
            return ".mov";
        }

        /**
         * {@inheritDoc}
         */
        public String getIntermediateExtension() {
            return ".bmp";
        }

        /**
         * {@inheritDoc}
         */
        public boolean useTempDirectoryForIntermediateImages() {
            return true;
        }
    }

    /**
     * Implementation of MovieFileFilter for MPEG-4 movies.
     * 
     * @author Markus Langenberg
     */
    private class MP4Filter extends MovieFileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean acceptFile(File f) {
            return f.getName().toLowerCase().endsWith(".mp4");
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "MPEG-4 (.mp4)";
        }

        /**
         * {@inheritDoc}
         */
        public String getExtension() {
            return ".mp4";
        }

        /**
         * {@inheritDoc}
         */
        public String getIntermediateExtension() {
            return ".bmp";
        }

        /**
         * {@inheritDoc}
         */
        public boolean useTempDirectoryForIntermediateImages() {
            return true;
        }
    }

    /**
     * Implementation of MovieFileFilter for a set of JPEG images.
     * 
     * @author Markus Langenberg
     */
    private class JPEGSetFilter extends MovieFileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean acceptFile(File f) {
            return f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg");
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Set of JPG images (.jpg)";
        }

        /**
         * {@inheritDoc}
         */
        public String getExtension() {
            return ".jpg";
        }

        /**
         * {@inheritDoc}
         */
        public String getIntermediateExtension() {
            return ".jpg";
        }

        /**
         * {@inheritDoc}
         */
        public boolean useTempDirectoryForIntermediateImages() {
            return false;
        }
    }

    /**
     * Implementation of MovieFileFilter for a set of PNG images.
     * 
     * @author Markus Langenberg
     */
    private class PNGSetFilter extends MovieFileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean acceptFile(File f) {
            return f.getName().toLowerCase().endsWith(".png");
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Set of PNG images (.png)";
        }

        /**
         * {@inheritDoc}
         */
        public String getExtension() {
            return ".png";
        }

        /**
         * {@inheritDoc}
         */
        public String getIntermediateExtension() {
            return ".png";
        }

        /**
         * {@inheritDoc}
         */
        public boolean useTempDirectoryForIntermediateImages() {
            return false;
        }
    }

    /**
     * Implementation of MovieFileFilter for ZIP archives of JPEG images.
     * 
     * @author Markus Langenberg
     */
    private class JPEGArchiveFilter extends MovieFileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean acceptFile(File f) {
            return f.getName().toLowerCase().endsWith(".zip");
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "ZIP archive of JPG images (.zip)";
        }

        /**
         * {@inheritDoc}
         */
        public String getExtension() {
            return ".zip";
        }

        /**
         * {@inheritDoc}
         */
        public String getIntermediateExtension() {
            return ".jpg";
        }

        /**
         * {@inheritDoc}
         */
        public boolean useTempDirectoryForIntermediateImages() {
            return true;
        }
    }

    /**
     * Implementation of MovieFileFilter for ZIP archives of PNG images.
     * 
     * @author Markus Langenberg
     */
    private class PNGArchiveFilter extends MovieFileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean acceptFile(File f) {
            return f.getName().toLowerCase().endsWith(".zip");
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "ZIP archive of PNG images (.zip)";
        }

        /**
         * {@inheritDoc}
         */
        public String getExtension() {
            return ".zip";
        }

        /**
         * {@inheritDoc}
         */
        public String getIntermediateExtension() {
            return ".png";
        }

        /**
         * {@inheritDoc}
         */
        public boolean useTempDirectoryForIntermediateImages() {
            return true;
        }

    }

    /**
     * Updates either the speed spinner or the zoom level
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getSource() == speedSpinner) {
            int minVal = (Integer) ((SpinnerNumberModel) speedSpinner.getModel()).getMinimum();
            int maxVal = (Integer) ((SpinnerNumberModel) speedSpinner.getModel()).getMaximum();
            int newVal = Math.min(maxVal, Math.max(minVal, -e.getWheelRotation() + (Integer) speedSpinner.getValue()));
            speedSpinner.setValue(newVal);
        } else if (e.getSource() == zoomSpinner) {
            double minVal = (Double) ((SpinnerNumberModel) zoomSpinner.getModel()).getMinimum();
            double newVal = Math.max(minVal, (Double) zoomSpinner.getValue() - (e.getWheelRotation() / 100.0));
            zoomSpinner.setValue(newVal);
        } else {
            int steps = (int) (-Math.max(1, e.getUnitsToScroll() / 3) * Math.signum(e.getUnitsToScroll()));
            double zoomFactor = Math.pow(ZoomController.zoomFactorStep, steps);
            double minVal = (Double) ((SpinnerNumberModel) zoomSpinner.getModel()).getMinimum();
            double newVal = Math.max(minVal, (Double) zoomSpinner.getValue() * zoomFactor);
            zoomSpinner.setValue(newVal);
        }
    }

    /**
     * Class which stores aspect ratio information
     * 
     * @author Andre Dau
     * 
     */
    static class AspectRatio {
        private int width;
        private int height;

        public AspectRatio(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public String toString() {
            if (width == 0 || height == 0) {
                return "Custom";
            } else {
                return width + " : " + height;
            }
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

    }

}