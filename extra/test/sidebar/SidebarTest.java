package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RepaintManager;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.JHVRangeSlider;
import org.helioviewer.jhv.gui.component.MoviePanel;
import org.helioviewer.jhv.gui.component.SliderValueLabel;
import org.helioviewer.jhv.gui.component.ToolBar;
import org.helioviewer.jhv.gui.time.TimeSelectorPanel;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.layers.TimestampLayer;
import org.helioviewer.jhv.layers.ViewpointLayer;
import org.helioviewer.jhv.layers.ViewpointLayerOptions;
import org.helioviewer.jhv.layers.ViewpointLayerOptionsExpert;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;
import org.helioviewer.jhv.movie.ExportFormat;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.thread.UiThread;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SidebarTest {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.io.tmpdir", System.getProperty("user.home"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        DataSources.initSources();
        try {
            UiThread.postAndWait(SidebarTest::checkSidebar);
            checkFitsOptions();
        } finally {
            UiThread.postAndWait(() -> {
                Layers.getViewpointLayer().setCameraMode(ViewpointLayerOptions.CameraMode.Observer);
                Player.pause();
                ViewState.setRefresh(false);
            });
        }
        System.out.println("SidebarTest passed");
    }

    private static void checkSidebar() {
        DisplayController.setRenderRequestHandler(_ -> {});
        UIGlobals.setLaf();
        checkGridState();
        checkSliderSizes(new GridLayerOptions(new GridLayer(null)), 350);
        checkSliderSizes(new MiniviewLayerOptions(new MiniviewLayer(null)), 350);
        checkSliderSizes(new TimestampLayerOptions(new TimestampLayer(null)), 350);
        checkPopupSliders(new ToolBar(new JPanel()));
        Layers.restore(List.of());
        LayersPanel panel = new LayersPanel();
        JPanel transport = new JPanel();
        transport.setPreferredSize(new Dimension(200, 40));
        panel.setTransportPanel(transport);
        JTabbedPane tabs = find(panel, JTabbedPane.class);
        ViewpointLayerOptionsPanel viewpoint = find(panel, ViewpointLayerOptionsPanel.class);
        JTable images = find(tabs.getComponentAt(0), JTable.class);
        JTable overlays = find(tabs.getComponentAt(1), JTable.class);
        images.setSize(500, 200);
        overlays.setSize(500, 200);
        check(images.getRowCount() == 0 && overlays.getRowCount() == 5, "Default category filtering");
        check(find(viewpoint, JComboBox.class).getSelectedItem() == ViewpointLayerOptions.CameraMode.Observer, "Default mode control");
        check(!findButton(viewpoint, Buttons.chevronRight + "Viewpoint").isEnabled(), "Default Observer disclosure enabled");
        check(!tabs.isEnabledAt(2) && !tabs.isEnabledAt(3), "Uninstalled sidebar tabs enabled");
        JPanel timelineContents = new JPanel();
        JPanel eventContents = new JPanel();
        panel.setTimelinesPanel(timelineContents);
        panel.setEventsPanel(eventContents);
        check(tabs.isEnabledAt(2) && tabs.isEnabledAt(3), "Installed sidebar tabs disabled");
        tabs.setSelectedIndex(3);
        panel.setEventsPanel(null);
        check(tabs.getSelectedIndex() == 0 && !tabs.isEnabledAt(3) && eventContents.getParent() == null, "Events uninstall retained its editor");
        panel.setEventsPanel(eventContents);

        int gridRow = rowNamed(overlays, "Grid");
        overlays.setRowSelectionInterval(gridRow, gridRow);
        Layer grid = (Layer) overlays.getValueAt(gridRow, 0);
        int observerHeight = viewpoint.getPreferredSize().height;
        tabs.setSelectedIndex(1);
        int gridHeight = tabs.getPreferredSize().height;
        tabs.setSelectedIndex(0);
        ImageLayer first = ImageLayer.create();
        ImageLayer second = ImageLayer.create();
        Dimension imageSize = tabs.getPreferredSize();
        tabs.setSelectedIndex(1);
        check(viewpoint.getPreferredSize().height == observerHeight, "Image editor enlarged Viewpoint section");
        check(tabs.getPreferredSize().height < imageSize.height, "Grid reserved image editor height");
        check(tabs.getPreferredSize().width == imageSize.width, "Tab switch changed sidebar width");
        tabs.setSize(imageSize.width, tabs.getPreferredSize().height);
        tabs.doLayout();
        check(tabs.getComponentAt(1).getHeight() == tabs.getComponentAt(1).getPreferredSize().height, "Grid content stretched vertically");
        check(tabs.getPreferredSize().height == gridHeight, "Image editor enlarged Grid tab");
        tabs.setSelectedIndex(0);
        check(tabs.getPreferredSize().equals(imageSize), "Image tab size changed after tab switch");
        check(images.getRowCount() == 2 && overlays.getRowCount() == 5, "Image insertion changed overlay rows");
        check(overlays.getValueAt(overlays.getSelectedRow(), 0) == grid, "Filtered insertion changed overlay selection");
        check(images.getValueAt(images.getSelectedRow(), 0) == second, "New image selection");
        ImageLayerOptions secondOptions = find(tabs.getComponentAt(0), ImageLayerOptions.class);
        first.setEnabled(true);
        click(images, 0, LayersTableModel.NAME_COL);
        check(Layers.getActiveImageLayer() == first, "Image activation");
        ImageLayerOptions firstOptions = find(tabs.getComponentAt(0), ImageLayerOptions.class);
        check(firstOptions != secondOptions, "Image options shared between layers");
        grid.setEnabled(false);
        Layers.fireLayerUpdated(grid);
        check(find(tabs.getComponentAt(1), GridLayerOptions.class).isEnabled(), "Hidden overlay settings disabled");
        click(overlays, rowNamed(overlays, "Grid"), LayersTableModel.ENABLED_COL);
        check(grid.isEnabled(), "Filtered visibility checkbox targeted the wrong layer");
        check(Layers.getActiveImageLayer() == first, "Overlay checkbox changed active image");
        checkAdjustments(panel, tabs, 0, firstOptions, imageSize.width);
        tabs.setSelectedIndex(1);
        GridLayerOptions gridOptions = find(tabs.getComponentAt(1), GridLayerOptions.class);
        panel.setSize(imageSize.width, panel.getPreferredSize().height);
        layoutTree(panel);
        Rectangle visible = gridOptions.getVisibleRect();
        check(visible.height == gridOptions.getPreferredSize().height && visible.width == gridOptions.getWidth(),
                "Grid controls clipped");
        checkLayerTimeRepaints(panel, tabs, images, overlays, first);

        JSONObject camera = DisplayController.cameraToJson();
        UpdateViewpoint update = DisplayController.getViewpointUpdate();
        for (int index = 0; index < tabs.getTabCount(); index++)
            tabs.setSelectedIndex(index);
        tabs.setSelectedIndex(0);
        check(DisplayController.getViewpointUpdate() == update && camera.similar(DisplayController.cameraToJson()), "Tab switch changed camera");
        check(find(tabs.getComponentAt(0), ImageLayerOptions.class) == firstOptions, "Tab switch rebuilt image options");
        ((LayersTableModel) images.getModel()).reorder(0, 2);
        check(images.getValueAt(0, 0) == second && images.getValueAt(1, 0) == first, "Filtered image reorder");
        images.setRowSelectionInterval(1, 1);
        click(images, 1, LayersTableModel.REMOVE_COL);
        check(images.getRowCount() == 1 && images.getValueAt(0, 0) == second, "Filtered image deletion");
        check(overlays.getValueAt(overlays.getSelectedRow(), 0) == grid, "Filtered deletion changed overlay selection");

        PfssPlugin plugin = new PfssPlugin();
        Settings.setProperty("plugins.PfssPlugin.active", "false");
        PluginManager.setGUIEnabled(true);
        PluginManager.addPlugin(plugin);
        PluginManager.setActive(plugin, true);
        int pfssRow = rowNamed(overlays, "PFSS Model");
        overlays.setRowSelectionInterval(pfssRow, pfssRow);
        check(hasLabel(tabs.getComponentAt(1), "Detail") && hasLabel(tabs.getComponentAt(1), "Radius"), "PFSS editor not integrated");

        ViewpointLayer restored = new ViewpointLayer(new JSONObject().put("mode", "ObserverAt1au"));
        restored.setEnabled(true);
        Layers.restore(List.of(restored));
        check(javax.swing.SwingUtilities.isDescendingFrom(transport, panel), "Viewpoint restoration removed transport controls");
        viewpoint = find(panel, ViewpointLayerOptionsPanel.class);
        int headerHeight = viewpoint.getPreferredSize().height;
        JComboBox<?> modeBox = find(viewpoint, JComboBox.class);
        check(modeBox.getSelectedItem() == ViewpointLayerOptions.CameraMode.ObserverAt1au, "Restored viewpoint editor is stale");
        check(!findButton(viewpoint, Buttons.chevronRight + "Viewpoint").isEnabled(), "Observer at 1au disclosure enabled");
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Observer);
        check(!restored.isEnabled() && DisplayController.getViewpointUpdate() == UpdateViewpoint.observer, "Observer selection did not turn off custom viewpoint");
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Location);
        check(restored.isEnabled(), "Location selection");
        ViewpointLayerOptionsExpertPanel locationEditor = visibleViewpointOptions(viewpoint);
        check(locationEditor != null, "Location options not visible");
        JTable objects = find(locationEditor, JTable.class);
        check(modeText(modeBox, -1).equals("Location: Earth"), "Selected Location label");
        check(modeText(modeBox, modeBox.getSelectedIndex()).equals("Location"), "Target leaked into mode menu");
        click(objects, objectRow(objects, "Venus"), 1);
        check(modeText(modeBox, -1).equals("Location: Venus"), "Location name click did not update selected target");
        check(restored.getOptions().getLocationOptions().getContainer().getSelectedElements().getFirst().toString().equals("Venus"), "Location name click did not select target");
        camera = DisplayController.cameraToJson();
        update = DisplayController.getViewpointUpdate();
        AbstractButton expand = findButton(viewpoint, Buttons.chevronDown + "Viewpoint");
        check(expand.isEnabled(), "Location disclosure disabled");
        expand.doClick();
        Layers.fireLayerUpdated(restored);
        check(visibleViewpointOptions(viewpoint) == null && modeBox.isVisible(), "Collapsed viewpoint hid mode or retained visible details");
        check(viewpoint.getPreferredSize().height == headerHeight, "Collapsed viewpoint reserves options height");
        check(modeText(modeBox, -1).equals("Location: Venus"), "Collapsed viewpoint lost target label");
        check(camera.similar(DisplayController.cameraToJson()) && update == DisplayController.getViewpointUpdate(), "Collapsing viewpoint changed camera");
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Location);
        check(visibleViewpointOptions(viewpoint) == locationEditor && expand.isSelected()
                && camera.similar(DisplayController.cameraToJson()) && update == DisplayController.getViewpointUpdate(),
                "Reselecting Location did not expand retained controls without changing the camera");
        expand.doClick();
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Heliosphere);
        ViewpointLayerOptionsExpertPanel equatorialEditor = visibleViewpointOptions(viewpoint);
        check(equatorialEditor != null && equatorialEditor != locationEditor, "Mode selection did not expand its own controls");
        ViewpointLayerOptionsExpert equatorial = restored.getOptions().getEquatorialOptions();
        checkSliderSizes(equatorialEditor, 500);
        JSlider spiralSlider = find(equatorialEditor, JSlider.class);
        AbstractButton spiralCheckBox = findButton(equatorialEditor, "Spiral");
        SliderValueLabel spiralLabel = find(spiralSlider.getParent(), SliderValueLabel.class);
        check(!spiralSlider.isEnabled() && !spiralLabel.isEnabled(), "Unchecked Spiral controls are enabled");
        spiralCheckBox.doClick();
        check(spiralSlider.isEnabled() && spiralLabel.isEnabled() && equatorial.isSpiral(), "Checking Spiral did not enable controls");
        spiralSlider.setValue(1370);
        int savedSpeed = spiralSlider.getValue();
        spiralCheckBox.doClick();
        check(!spiralSlider.isEnabled() && !spiralLabel.isEnabled() && !equatorial.isSpiral(), "Unchecking Spiral did not disable controls");
        spiralSlider.dispatchEvent(new MouseWheelEvent(spiralSlider, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0,
                10, 10, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
        spiralSlider.dispatchEvent(new MouseEvent(spiralSlider, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                10, 10, 2, false, MouseEvent.BUTTON1));
        check(spiralSlider.getValue() == savedSpeed && equatorial.getSpiralSpeedValue() == savedSpeed, "Disabling Spiral reset its speed");
        objects = find(equatorialEditor, JTable.class);
        SpaceObjectElement venus = (SpaceObjectElement) objects.getValueAt(objectRow(objects, "Venus"), 1);
        boolean wasSelected = venus.isSelected();
        click(objects, objectRow(objects, "Venus"), 1);
        check(venus.isSelected() == wasSelected && equatorial.getContainer().getHighlightedElement() == venus, "Heliosphere highlight changed checked objects");
        check(modeText(modeBox, -1).equals("Heliosphere"), "Heliosphere mode label");
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Observer);
        check(visibleViewpointOptions(viewpoint) == null && viewpoint.getPreferredSize().height == headerHeight, "Observer reserves options height");
        check(!expand.isEnabled() && !expand.isSelected(), "Observer disclosure enabled or expanded");
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Location);
        check(visibleViewpointOptions(viewpoint) == locationEditor && modeText(modeBox, -1).equals("Location: Venus"), "Mode switch rebuilt Location options or lost its target");
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Heliosphere);
        check(visibleViewpointOptions(viewpoint) == equatorialEditor, "Mode switch rebuilt Heliosphere options");
        modeBox.setSelectedItem(ViewpointLayerOptions.CameraMode.Observer);
        check(images.getRowCount() == 0 && overlays.getRowCount() == 5, "Restoration did not refresh category lists");
        tabs.setSelectedIndex(2);
        panel.setTimelinesPanel(null);
        check(tabs.getSelectedIndex() == 0 && !tabs.isEnabledAt(2) && timelineContents.getParent() == null, "Timelines uninstall retained its editor");
        PluginManager.setActive(plugin, false);
        checkRestoredViewpointDisclosure(panel);
        checkMovieOptions(panel);
    }

    private static void checkRestoredViewpointDisclosure(LayersPanel panel) {
        for (ViewpointLayerOptions.CameraMode mode : ViewpointLayerOptions.CameraMode.values()) {
            JSONObject empty = new JSONObject().put("objects", new JSONArray());
            ViewpointLayer restored = new ViewpointLayer(new JSONObject().put("mode", mode.name())
                    .put("location", empty).put("equatorial", empty));
            restored.setEnabled(true);
            Layers.restore(List.of(restored));
            ViewpointLayerOptionsPanel viewpoint = find(panel, ViewpointLayerOptionsPanel.class);
            boolean hasOptions = mode == ViewpointLayerOptions.CameraMode.Location || mode == ViewpointLayerOptions.CameraMode.Heliosphere;
            AbstractButton expand = findButton(viewpoint, (hasOptions ? Buttons.chevronDown : Buttons.chevronRight) + "Viewpoint");
            check(expand != null && expand.isEnabled() == hasOptions && expand.isSelected() == hasOptions,
                    "Restored disclosure state: " + mode);
            check((visibleViewpointOptions(viewpoint) != null) == hasOptions, "Restored options visibility: " + mode);
            if (hasOptions) {
                expand.doClick();
                restored.setCameraMode(mode == ViewpointLayerOptions.CameraMode.Location
                        ? ViewpointLayerOptions.CameraMode.Heliosphere : ViewpointLayerOptions.CameraMode.Location);
                check(expand.isSelected() && visibleViewpointOptions(viewpoint) != null, "Model mode change did not expand options");
            }
        }
        Layers.getViewpointLayer().setCameraMode(ViewpointLayerOptions.CameraMode.Observer);
    }

    private static void checkMovieOptions(LayersPanel panel) {
        Settings.setProperty("video.format", ExportFormat.H265HQ.name());
        ViewState.setRecordingMode(ViewState.RecordingMode.SHOT);
        MoviePanel movie = MoviePanel.getInstance();
        panel.setTransportPanel(movie);
        List<Component> components = new ArrayList<>();
        collect(movie, Component.class, components);
        JComboBox<?> formatBox = null;
        for (Component component : components) {
            if (component instanceof JComboBox<?> box && box.getItemAt(0) instanceof ExportFormat)
                formatBox = box;
            if (component instanceof JTextField field && "Program time".equals(field.getToolTipText()))
                checkProgramTime(movie, field);
        }
        check(formatBox != null && !formatBox.isEnabled() && formatBox.getSelectedItem() == ExportFormat.H265HQ,
                "Initial Screenshot format state or saved video format");
        for (ExportFormat format : ExportFormat.values()) {
            findButton(movie, ViewState.RecordingMode.LOOP.toString()).doClick();
            check(formatBox.isEnabled(), "Loop format disabled");
            formatBox.setSelectedItem(format);
            check(Settings.getProperty("video.format").equals(format.name()), "Video format selection not saved");
            findButton(movie, ViewState.RecordingMode.SHOT.toString()).doClick();
            movie.recordingStatusChanged();
            check(!formatBox.isEnabled(), "Screenshot format enabled");
            check(formatBox.getSelectedItem() == format && Settings.getProperty("video.format").equals(format.name()),
                    "Screenshot overwrote video format");
            findButton(movie, ViewState.RecordingMode.FREE.toString()).doClick();
            check(formatBox.isEnabled() && formatBox.getSelectedItem() == format,
                    "Unlimited recording did not retain video format");
        }
        ViewState.setRecordingMode(ViewState.RecordingMode.LOOP);
    }

    private static void checkProgramTime(MoviePanel movie, JTextField clock) {
        String original = clock.getText();
        Font originalFont = clock.getFont();
        Container position = clock.getParent();
        Container transport = position.getParent();
        JComponent frames = (JComponent) position.getComponent(0);
        Font frameFont = frames.getFont();
        RepaintManager repaintManager = RepaintManager.currentManager(clock);
        try {
            for (float scale : new float[]{1, 1.25f, 1.5f}) {
                clock.setFont(originalFont.deriveFont(originalFont.getSize2D() * scale));
                frames.setFont(frameFont.deriveFont(frameFont.getSize2D() * scale));
                Dimension size = clock.getPreferredSize();
                transport.setSize(transport.getPreferredSize());
                layoutTree(transport);
                check(frames.getWidth() >= frames.getFontMetrics(frames.getFont()).stringWidth("10000/10000"), "Full frame indicator clipped");
                check(clock.getWidth() >= size.width, "Transport row clipped timestamp");
                for (String text : List.of("2026-10-21T11:56:23.000", "2026-10-22T00:00:00.123", "2026-10-23T23:59:59.999")) {
                    clock.setText(text);
                    check(clock.getCaretPosition() == 0, "Timestamp update moved the caret");
                    check(clock.getPreferredSize().equals(size), "Timestamp update changed its reserved size");
                    clock.setScrollOffset(0);
                    double right = clock.modelToView2D(text.length()).getX();
                    clock.setScrollOffset(1);
                    check(clock.modelToView2D(text.length()).getX() == right, "Caret scrolling shifted the timestamp");
                    check(clock.modelToView2D(0).getX() >= 0 && right < size.width, "Timestamp clipped");
                }
            }

            int[] repaints = {0};
            RepaintManager.setCurrentManager(new RepaintManager() {
                @Override
                public void addDirtyRegion(JComponent component, int x, int y, int width, int height) {
                    if (component == clock)
                        repaints[0]++;
                }
            });
            movie.lazyRepaint();
            repaints[0] = 0;
            clock.repaint();
            clock.repaint(0, 0, clock.getWidth(), clock.getHeight());
            check(repaints[0] == 0, "Unfocused timestamp repaint bypassed UI timer");
            movie.lazyRepaint();
            check(repaints[0] == 1, "Timestamp repaint requests were not batched");
            movie.lazyRepaint();
            check(repaints[0] == 1, "Clean timestamp repainted again");

            clock.setText("2026-10-30T12:34:56.789");
            movie.lazyRepaint();
            repaints[0] = 0;
            BufferedImage image = new BufferedImage(clock.getWidth(), clock.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            clock.paint(graphics);
            graphics.dispose();
            check(repaints[0] == 0, "Painting queued a redundant timestamp repaint");
            movie.lazyRepaint();
            check(repaints[0] == 1, "Paint-generated timestamp repaint was lost");
            movie.lazyRepaint();
            check(repaints[0] == 1, "Timestamp repaint did not settle while paused");

            Field displayedTime = MoviePanel.class.getDeclaredField("displayedTime");
            displayedTime.setAccessible(true);
            displayedTime.setLong(movie, Long.MIN_VALUE);
            ValidationProbe probe = new ValidationProbe();
            RepaintManager.setCurrentManager(probe);
            clock.select(5, 10);
            movie.lazyRepaint();
            check(!probe.queued, "Timestamp update queued validation");
            check(clock.getSelectionStart() == 5 && clock.getSelectionEnd() == 10, "Timestamp update lost the selection");
        } catch (javax.swing.text.BadLocationException | ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        } finally {
            RepaintManager.setCurrentManager(repaintManager);
            clock.setFont(originalFont);
            frames.setFont(frameFont);
            clock.setText(original);
            clock.select(0, 0);
        }
    }

    private static void checkLayerTimeRepaints(LayersPanel panel, JTabbedPane tabs, JTable images, JTable overlays, ImageLayer image) {
        tabs.setSelectedIndex(0);
        panel.setSize(panel.getWidth(), panel.getPreferredSize().height);
        layoutTree(panel);
        Interfaces.LazyComponent imageUpdates = (Interfaces.LazyComponent) images;
        Interfaces.LazyComponent overlayUpdates = (Interfaces.LazyComponent) overlays;
        imageUpdates.lazyRepaint();
        overlayUpdates.lazyRepaint();
        RepaintManager original = RepaintManager.currentManager(images);
        Map<JComponent, Rectangle> regions = new IdentityHashMap<>();
        RepaintManager.setCurrentManager(new RepaintManager() {
            @Override
            public void addDirtyRegion(JComponent component, int x, int y, int width, int height) {
                if (component == images || component == overlays)
                    regions.merge(component, new Rectangle(x, y, width, height), Rectangle::union);
            }
        });
        try {
            Layers.fireTimeUpdated(Layers.getViewpointLayer());
            imageUpdates.lazyRepaint();
            overlayUpdates.lazyRepaint();
            check(regions.isEmpty(), "Filtered Viewpoint update repainted layer tables");

            Layers.fireTimeUpdated(image);
            check(regions.isEmpty(), "Timestamp repaint bypassed UI timer");
            imageUpdates.lazyRepaint();
            overlayUpdates.lazyRepaint();
            int row = images.convertRowIndexToView(Layers.getLayers().indexOf(image));
            Rectangle expected = images.getCellRect(row, LayersTableModel.TIME_COL, true).intersection(images.getVisibleRect());
            check(!expected.isEmpty(), "Image time cell is outside the test viewport");
            check(expected.equals(regions.get(images)) && !regions.containsKey(overlays), "Image update repainted unrelated time cells");

            tabs.setSelectedIndex(1);
            panel.setSize(panel.getWidth(), panel.getPreferredSize().height);
            layoutTree(panel);
            imageUpdates.lazyRepaint();
            overlayUpdates.lazyRepaint();
            regions.clear();
            Layer connection = Layers.getConnectionLayer();
            Layers.fireTimeUpdated(connection);
            imageUpdates.lazyRepaint();
            overlayUpdates.lazyRepaint();
            row = overlays.convertRowIndexToView(Layers.getLayers().indexOf(connection));
            expected = overlays.getCellRect(row, LayersTableModel.TIME_COL, true).intersection(overlays.getVisibleRect());
            check(!expected.isEmpty(), "Overlay time cell is outside the test viewport");
            check(expected.equals(regions.get(overlays)) && !regions.containsKey(images), "Overlay update repainted unrelated time cells");
        } finally {
            RepaintManager.setCurrentManager(original);
        }
    }

    private static final class ValidationProbe extends RepaintManager {
        private boolean queued;

        @Override
        public void addInvalidComponent(JComponent component) {
            queued = true;
        }
    }

    private static ViewpointLayerOptionsExpertPanel visibleViewpointOptions(Container viewpoint) {
        for (Component child : viewpoint.getComponents()) {
            if (child instanceof ViewpointLayerOptionsExpertPanel editor && editor.isVisible())
                return editor;
        }
        return null;
    }

    private static void checkFitsOptions() throws Exception {
        ImageLayer layer = UiThread.postAndWait(ImageLayer::create);
        try {
            CompletableFuture<Boolean> finished = UiThread.postAndWait(() -> {
                layer.load(List.of(Path.of("extra/test/data/solo_L2_eui-fsi174-image_20251002T150055171_V00.fits").toUri()));
                return layer.whenLoadFinished();
            });
            check(finished.get(30, TimeUnit.SECONDS), "FITS fixture failed to load");
            UiThread.postAndWait(() -> {
                check(layer.frames().hasFITS(), "Fixture is not FITS");
                checkViewpointTimeSync();
                ImageLayerOptions editor = new ImageLayerOptions(layer);
                AbstractButton adjustments = findButton(editor, Buttons.adjustmentsRight);
                AbstractButton fits = findButton(editor, Buttons.fitsRight);
                check(fits != null && fits.isVisible(), "FITS options missing for loaded layer");
                List<Component> controls = List.of(controlNamed(editor, "Difference"), controlNamed(editor, "Opacity"), controlNamed(editor, "Blend"),
                        controlNamed(editor, "Sharpen"), controlNamed(editor, "Levels"),
                        controlNamed(editor, "Color"), controlNamed(editor, "Channels"), controlNamed(editor, "Filter"));
                List<Component> advanced = List.of(controlNamed(editor, "Slit"), controlNamed(editor, "Mask"),
                        controlNamed(editor, "Sector"), controlNamed(editor, "Opening"), controlNamed(editor, "δCROTA"),
                        controlNamed(editor, "δCRVAL1"), controlNamed(editor, "δCRVAL2"));
                for (int width : new int[]{350, 500}) {
                    editor.setSize(width, editor.getPreferredSize().height);
                    layoutTree(editor);
                    List<Rectangle> bounds = controls.stream()
                            .map(control -> javax.swing.SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), editor)).toList();
                    for (int step = 0; step < 8; step++) {
                        (step % 2 == 0 ? adjustments : fits).doClick();
                        editor.setSize(width, editor.getPreferredSize().height);
                        layoutTree(editor);
                        int previousBottom = 0;
                        for (int index = 0; index < controls.size(); index++) {
                            Component control = controls.get(index);
                            Rectangle actual = javax.swing.SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), editor);
                            Rectangle expected = bounds.get(index);
                            check(actual.x == expected.x && actual.width == expected.width,
                                    "Main image control moved when expanding options: " + expected + " -> " + actual);
                            check(actual.height > 0 && actual.y >= previousBottom, "Main image control rows overlap");
                            previousBottom = actual.y + actual.height;
                        }
                        if (adjustments.isSelected()) {
                            for (Component control : advanced) {
                                Rectangle actual = javax.swing.SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), editor);
                                check(actual.height > 0 && actual.y >= previousBottom, "Image adjustment rows overlap");
                                previousBottom = actual.y + actual.height;
                            }
                        }
                    }
                }
                adjustments.doClick();
                fits.doClick();
                List<Component> aligned = new ArrayList<>(controls);
                aligned.addAll(advanced);
                aligned.add(controlNamed(editor, "γ"));
                aligned.add(controlNamed(editor, "β"));
                aligned.add(controlNamed(editor, "α"));
                for (int width : new int[]{350, 500}) {
                    editor.setSize(width, editor.getPreferredSize().height);
                    layoutTree(editor);
                    Rectangle expected = javax.swing.SwingUtilities.convertRectangle(controls.getFirst().getParent(), controls.getFirst().getBounds(), editor);
                    for (Component control : aligned) {
                        Rectangle actual = javax.swing.SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), editor);
                        check(actual.x == expected.x && actual.width == expected.width,
                                "Image option columns do not align: " + expected + " / " + actual);
                    }
                    checkSliderSizes(editor, width);
                }
                checkPopupSliders(editor);
            });
        } finally {
            UiThread.postAndWait(() -> Layers.remove(layer));
        }
    }

    private static void checkViewpointTimeSync() {
        long movieStart = Player.getStartTime();
        long movieEnd = Player.getEndTime();
        check(movieStart % 1000 != 0, "FITS fixture must retain fractional seconds");
        JSONObject empty = new JSONObject().put("objects", new JSONArray());
        JSONObject spiralState = new JSONObject().put("objects", new JSONArray()).put("spiral", true).put("spiralSpeed", 1370);
        ViewpointLayer layer = new ViewpointLayer(new JSONObject().put("location", empty).put("equatorial", spiralState));
        for (ViewpointLayerOptionsExpert options : List.of(layer.getOptions().getLocationOptions(), layer.getOptions().getEquatorialOptions())) {
            ViewpointLayerOptionsExpertPanel editor = new ViewpointLayerOptionsExpertPanel(options, () -> {});
            if (options.hasFrameOptions()) {
                JSlider slider = find(editor, JSlider.class);
                check(slider.isEnabled() && slider.getValue() == 1370, "Restored Spiral controls are not enabled at the saved speed");
            }
            AbstractButton sync = findButton(editor, "Use movie time interval");
            TimeSelectorPanel selector = find(editor, TimeSelectorPanel.class);
            check(sync.isSelected() && !selector.isVisible(), "Synchronized interval editor is visible");
            check(options.getStartTime() == movieStart && options.getEndTime() == movieEnd, "Creating editor changed synchronized timestamps");
            sync.doClick();
            check(!options.isSyncInterval() && selector.isVisible(), "Manual interval editor did not open");
            selector.setTime(movieStart + 10000, movieEnd + 20000);
            check(options.getStartTime() == selector.getStartTime() && options.getEndTime() == selector.getEndTime(), "Manual interval edit did not reach viewpoint");
            sync.doClick();
            check(options.isSyncInterval() && !selector.isVisible(), "Movie time sync did not hide manual interval");
            check(options.getStartTime() == movieStart && options.getEndTime() == movieEnd, "Time sync lost fractional movie timestamps");
            selector.setTime(movieStart + 30000, movieEnd + 40000);
            check(options.getStartTime() == movieStart && options.getEndTime() == movieEnd, "Hidden selector changed synchronized interval");
        }
    }

    private static Component controlNamed(Container parent, String text) {
        Component[] children = parent.getComponents();
        for (int index = 0; index < children.length; index++) {
            Component child = children[index];
            if ((child instanceof JLabel label && label.getText().trim().equals(text))
                    || (child instanceof AbstractButton button && text.equals(button.getText())))
                return children[index + 1];
            if (child instanceof Container container) {
                Component control = controlNamed(container, text);
                if (control != null)
                    return control;
            }
        }
        return null;
    }

    private static void checkPopupSliders(Component component) {
        if (component instanceof JMenu menu) {
            checkSliderSizes(menu.getPopupMenu(), menu.getPopupMenu().getPreferredSize().width);
            checkPopupSliders(menu.getPopupMenu());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents())
                checkPopupSliders(child);
        }
    }

    private static void checkSliderSizes(Container editor, int width) {
        List<JSlider> sliders = new ArrayList<>();
        collect(editor, JSlider.class, sliders);
        if (sliders.isEmpty())
            return;
        List<SliderValueLabel> readouts = new ArrayList<>();
        collect(editor, SliderValueLabel.class, readouts);
        editor.setSize(width, editor.getPreferredSize().height);
        layoutTree(editor);
        Dimension preferred = editor.getPreferredSize();
        List<Rectangle> bounds = sliders.stream().map(Component::getBounds).toList();
        for (JSlider slider : sliders) {
            int initial = slider.getValue();
            int extent = slider.getExtent();
            if (slider instanceof JHVRangeSlider range) {
                range.getModel().setRangeProperties(range.getMinimum(), range.getMaximum() - range.getMinimum(), range.getMinimum(), range.getMaximum(), false);
                for (int value = range.getMinimum(); value <= range.getMaximum(); value++) {
                    range.setLowValue(value);
                    checkSliderGeometry(editor, sliders, bounds, preferred, readouts);
                }
                range.setLowValue(range.getMinimum());
                for (int value = range.getMinimum(); value <= range.getMaximum(); value++) {
                    range.setHighValue(value);
                    checkSliderGeometry(editor, sliders, bounds, preferred, readouts);
                }
                range.getModel().setRangeProperties(initial, extent, range.getMinimum(), range.getMaximum(), false);
            } else {
                for (int value = slider.getMinimum(); value <= slider.getMaximum(); value++) {
                    slider.setValue(value);
                    checkSliderGeometry(editor, sliders, bounds, preferred, readouts);
                }
                slider.setValue(initial);
            }
        }
    }

    private static void checkSliderGeometry(Container editor, List<JSlider> sliders, List<Rectangle> bounds, Dimension preferred, List<SliderValueLabel> readouts) {
        layoutTree(editor);
        check(editor.getPreferredSize().equals(preferred), "Slider value changed editor preferred size");
        for (int index = 0; index < sliders.size(); index++)
            check(sliders.get(index).getBounds().equals(bounds.get(index)), "Slider value changed control bounds: " + bounds.get(index) + " -> " + sliders.get(index).getBounds());
        for (SliderValueLabel label : readouts)
            check(label.getUI().getPreferredSize(label).width <= label.getPreferredSize().width,
                    "Slider readout does not fit reserved width: " + label.getText());
    }

    private static <T> void collect(Component component, Class<T> type, List<T> matches) {
        if (type.isInstance(component))
            matches.add(type.cast(component));
        if (component instanceof Container container) {
            for (Component child : container.getComponents())
                collect(child, type, matches);
        }
    }

    private static void checkGridState() {
        GridLayer hidden = new GridLayer(new JSONObject().put("labelAlpha", 0).put("labelSize", 30));
        check(hidden.getLabelAlpha() == 0 && hidden.getGridLabelSize() == 30, "Hidden label settings not restored");
        hidden.setLabelAlpha(0.4);
        JSONObject saved = new JSONObject();
        hidden.serialize(saved);
        GridLayer restored = new GridLayer(saved);
        check(restored.getLabelAlpha() == 0.4 && restored.getGridLabelSize() == 30, "Label opacity/size round trip");
        restored.setLabelAlpha(0);
        restored.serialize(saved);
        check(new GridLayer(saved).getLabelAlpha() == 0, "Hidden label opacity round trip");
    }

    private static void checkAdjustments(LayersPanel panel, JTabbedPane tabs, int tab, JComponent editor, int sidebarWidth) {
        tabs.setSelectedIndex(tab);
        AbstractButton adjustments = findButton(editor, Buttons.adjustmentsRight);
        check(adjustments != null, "Missing adjustments button");
        int collapsedHeight = editor.getPreferredSize().height;
        adjustments.doClick();
        check(editor.getPreferredSize().height > collapsedHeight, "Adjustments did not expand");
        panel.setSize(sidebarWidth, panel.getPreferredSize().height);
        layoutTree(panel);
        Rectangle visible = editor.getVisibleRect();
        check(visible.height == editor.getHeight() && visible.width == editor.getWidth(),
                editor.getClass().getSimpleName() + " expanded controls clipped: " + visible + " / " + editor.getSize());
        adjustments.doClick();
        check(editor.getPreferredSize().height == collapsedHeight, "Adjustments did not collapse");
    }

    private static AbstractButton findButton(Component component, String text) {
        if (component instanceof AbstractButton button && text.equals(button.getText()))
            return button;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                AbstractButton found = findButton(child, text);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private static <T> String modeText(JComboBox<T> box, int index) {
        T value = box.getItemAt(index == -1 ? box.getSelectedIndex() : index);
        return ((JLabel) box.getRenderer().getListCellRendererComponent(new JList<T>(), value, index, false, false)).getText();
    }

    private static int objectRow(JTable table, String name) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (table.getValueAt(row, 1).toString().equals(name))
                return row;
        }
        throw new AssertionError("Missing object: " + name);
    }

    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents())
                layoutTree(child);
        }
    }

    private static void click(JTable table, int row, int column) {
        table.changeSelection(row, column, false, false);
        Rectangle cell = table.getCellRect(row, column, false);
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                cell.x + cell.width / 2, cell.y + cell.height / 2, 1, false, MouseEvent.BUTTON1));
    }

    private static int rowNamed(JTable table, String name) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (((Layer) table.getValueAt(row, 0)).getName().equals(name))
                return row;
        }
        throw new AssertionError("Missing layer: " + name);
    }

    private static boolean hasLabel(Component component, String text) {
        if (component instanceof JLabel label && label.getText().trim().equals(text))
            return true;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (hasLabel(child, text))
                    return true;
            }
        }
        return false;
    }

    private static <T extends Component> T find(Component component, Class<T> type) {
        T found = findOrNull(component, type);
        if (found == null)
            throw new AssertionError("Missing component: " + type.getName());
        return found;
    }

    private static <T extends Component> T findOrNull(Component component, Class<T> type) {
        if (type.isInstance(component))
            return type.cast(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findOrNull(child, type);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
