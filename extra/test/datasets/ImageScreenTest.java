package org.helioviewer.jhv.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.gui.time.TimeField;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.ImageRequestSettings;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

public final class ImageScreenTest {
    private static final long START = 1735732800000L;
    private static final long DAY = TimeUtils.DAY_IN_MILLIS;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static <T> List<T> widgets(Container parent, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            else if (child instanceof Container container) found.addAll(widgets(container, type));
        }
        return found;
    }

    private static DataSourcesParser catalog(String server) throws Exception {
        Constructor<DataSourcesParser> constructor = DataSourcesParser.class.getDeclaredConstructor(String.class, JSONObject.class);
        constructor.setAccessible(true);
        return constructor.newInstance(server, new JSONObject("""
            {"SDO":{"name":"SDO","description":"group tip","children":{
            "a":{"name":"171","description":"leaf tip","sourceId":10,"default":true,"start":"2025-01-01 00:00:00","end":"2025-01-15 00:00:00"},
            "b":{"name":"304","description":"other tip","sourceId":20,"start":"2025-01-01 00:00:00","end":"2025-01-15 00:00:00"}}}}
            """));
    }

    private static DataSourcesParser catalog(String server, int groups, int leaves) throws Exception {
        JSONObject data = new JSONObject();
        for (int group = 0; group < groups; group++) {
            JSONObject children = new JSONObject();
            for (int leaf = 0; leaf < leaves; leaf++) {
                JSONObject item = new JSONObject().put("name", "L" + (100 + leaf)).put("description", "dataset").put("sourceId", group * leaves + leaf + 1)
                        .put("start", "2025-01-01 00:00:00").put("end", "2025-01-15 00:00:00");
                if (group == groups - 1 && leaf == leaves - 1)
                    item.put("default", true);
                children.put("L" + (100 + leaf), item);
            }
            data.put("G" + group, new JSONObject().put("name", "G" + group).put("description", "group").put("children", children));
        }
        Constructor<DataSourcesParser> constructor = DataSourcesParser.class.getDeclaredConstructor(String.class, JSONObject.class);
        constructor.setAccessible(true);
        return constructor.newInstance(server, data);
    }

    private static void treeRetention() throws Exception {
        ArrayList<Integer> loaded = new ArrayList<>();
        ImageScreen screen = new ImageScreen((target, request) -> loaded.add(request.sourceId()), url -> {});
        screen.setupSources(catalog("ROB", 3, 40));
        SwingPanel panel = new SwingPanel(screen);
        JTree tree = widgets(panel, JTree.class).getFirst();
        panel.setSize(panel.getPreferredSize());
        layout(panel);
        check(((JViewport) tree.getParent()).getViewPosition().y == 0, "Default selection must not scroll the catalog");
        TreePath group = leaf(tree, "ROB", 0).getParentPath();
        tree.expandPath(group);
        int last = tree.getRowForPath(leaf(tree, "ROB", 3));
        tree.setSelectionRow(last);
        tree.setSelectionInterval(last - 2, last);
        screen.command(0, 1, Panel.Command.CLICK);
        check(loaded.equals(List.of(2, 3, 4)), "Upward range changed layer load order: " + loaded);
        tree.clearSelection();
        screen.setupSources(catalog("GSFC", 2, 3));
        panel.refresh();
        check(tree.isExpanded(leaf(tree, "ROB", 0).getParentPath()), "A second catalog collapsed an open branch");
        tree.collapsePath(leaf(tree, "ROB", 0).getParentPath());
        screen.setupSources(catalog("GSFC", 2, 4));
        panel.refresh();
        check(!tree.isExpanded(leaf(tree, "ROB", 0).getParentPath()), "Catalog replacement reopened a collapsed branch");
    }

    private static int item(Panel p, String label) {
        for (int i = 0; i < p.size(); i++)
            if (p.kind(i) == Panel.Kind.ITEM && p.string(i, Panel.IntField.LABEL).equals(label)) return p.value(i, Panel.IntField.ID);
        throw new AssertionError(label);
    }

    private static TreePath leaf(JTree tree, String server, int index) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
            if (node.toString().equals(server)) {
                DefaultMutableTreeNode group = (DefaultMutableTreeNode) node.getChildAt(0);
                return new TreePath(((DefaultMutableTreeNode) group.getChildAt(index)).getPath());
            }
        }
        throw new AssertionError(server);
    }

    private static void sources() throws Exception {
        List<String> trace = new ArrayList<>();
        List<ImageLayer> targets = new ArrayList<>();
        ImageScreen screen = new ImageScreen((target, request) -> {
            targets.add(target);
            trace.add("load " + request.server() + " " + request.sourceId());
            check(request.startTime() == ImageRequestSettings.instance().start(), "load uses accepted request settings");
        }, trace::add);
        screen.onDismiss(() -> trace.add("dismiss"));
        Settings.setProperty("dataSources.defaultServer", "ROB");
        DataSourcesParser rob = catalog("ROB");
        screen.setupSources(rob);
        ImageRequestSettings settings = ImageRequestSettings.instance();
        check(settings.end() == rob.getDefault().end && settings.start() == rob.getDefault().end - 2 * DAY, "preferred default interval");
        SwingPanel swing = new SwingPanel(screen);
        JTree tree = widgets(swing, JTree.class).getFirst();
        check(swing.initialFocus() == tree, "source tree initial focus");
        check(tree.getSelectionModel().getSelectionMode() == TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, "add permits multiple selection");
        TreePath first = leaf(tree, "ROB", 0);
        TreePath second = leaf(tree, "ROB", 1);
        check(tree.isPathSelected(first), "preferred source selected");
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) first.getLastPathComponent();
        check(!node.getAllowsChildren() && ((DefaultMutableTreeNode) node.getParent()).getAllowsChildren(), "datasets are leaves under groups");
        tree.setSize(500, 500);
        Rectangle bounds = tree.getPathBounds(first);
        check(bounds != null && "leaf tip".equals(tree.getToolTipText(new MouseEvent(tree, MouseEvent.MOUSE_MOVED, 0, 0, bounds.x + 1, bounds.y + 1, 0, false))), "dataset tooltip");
        check(tree.getToolTipText(null) == null, "null tooltip event");
        tree.setSelectionPath(first.getParentPath());
        check(tree.isPathSelected(first.getParentPath()), "group selection stays visible");
        check(widgets(swing, AbstractButton.class).stream().filter(button -> button.getText().equals("Add")).noneMatch(Component::isEnabled), "group cannot load");
        tree.setSelectionPaths(new TreePath[]{first, second});
        screen.command(0, 3, Panel.Command.CLICK);
        check(trace.isEmpty(), "multiple sources have no availability route");
        screen.command(0, 1, Panel.Command.CLICK);
        check(trace.equals(List.of("dismiss", "load ROB 10", "load ROB 20")), "dismiss before real injected loads in selection order " + trace);
        trace.clear();
        tree.setSelectionPath(first);
        screen.command(0, 3, Panel.Command.CLICK);
        check(trace.equals(List.of(DataSources.getServer("ROB").availabilityURL() + "ID=10")), "single source availability route");
        trace.clear();
        long start = settings.start();
        screen.setupSources(catalog("GSFC"));
        swing.refresh();
        check(tree.isPathSelected(leaf(tree, "ROB", 0)) && settings.start() == start, "nonpreferred source preserves selection and interval");
        ImageLayer local = ImageLayer.create();
        tree.setSelectionPaths(new TreePath[]{leaf(tree, "ROB", 0), leaf(tree, "ROB", 1)});
        screen.show(local);
        swing.refresh();
        check(tree.getSelectionCount() == 1 && tree.getSelectionModel().getSelectionMode() == TreeSelectionModel.SINGLE_TREE_SELECTION, "custom/local change preserves first selected dataset");
        screen.command(0, 1, Panel.Command.CLICK);
        check(targets.getLast() == local && trace.equals(List.of("dismiss", "load ROB 10")), "change uses the original local layer");
        Field request = ImageLayer.class.getDeclaredField("request");
        request.setAccessible(true);
        request.set(local, settings.request("GSFC", 20));
        screen.show(local);
        swing.refresh();
        check(tree.isPathSelected(leaf(tree, "GSFC", 1)), "change selects the request's server and ID");
        request.set(local, settings.request("GSFC", -1));
        screen.show(local);
        swing.refresh();
        check(tree.isPathSelected(leaf(tree, "GSFC", 1)), "missing source preserves selection");
        request.set(local, settings.request("missing", 20));
        screen.show(local);
        swing.refresh();
        check(tree.isPathSelected(leaf(tree, "GSFC", 1)), "missing server preserves selection");
        screen.show();
        screen.setupSources(catalog("ROB"));
        swing.refresh();
        first = leaf(tree, "ROB", 0);
        trace.clear();
        bounds = tree.getPathBounds(first);
        MouseEvent doubleClick = new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, 0, 0, bounds.x + 1, bounds.y + 1, 2, false);
        for (MouseListener listener : tree.getMouseListeners()) listener.mouseClicked(doubleClick);
        check(trace.equals(List.of("dismiss", "load ROB 10")), "real leaf mouse listener activates loading");
        tree.clearSelection();
        screen.command(0, 1, Panel.Command.CLICK);
        check(trace.size() == 2, "cleared selection cannot load");
        screen.setupSources(catalog("GSFC"));
        screen.command(0, item(screen.build(), "304"), Panel.Command.SELECT_ONLY);
        Constructor<DataSourcesParser> constructor = DataSourcesParser.class.getDeclaredConstructor(String.class, JSONObject.class);
        constructor.setAccessible(true);
        long acceptedStart = settings.start();
        long acceptedEnd = settings.end();
        screen.setupSources(constructor.newInstance("ROB", new JSONObject()));
        check(settings.start() == acceptedStart && settings.end() == acceptedEnd, "A catalog without a default changed the interval");
        swing.refresh();
        check(tree.getSelectionCount() == 0, "catalog replacement clears stale source selection");
        screen.show(local);
        trace.clear();
        screen.layerRemoved(0, local);
        check(trace.equals(List.of("dismiss")) && screen.title().equals("New Image Layer"), "Removing the change target did not dismiss its dialog");
        Layers.remove(local);
        System.out.println("PASS image defaults, groups, leaves, tooltips, add/change/local routing, callback order, availability and selection clearing");
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents())
            if (child instanceof Container container) layout(container);
    }

    private static void bounds(Container parent, Container root, StringBuilder trace) {
        for (Component child : parent.getComponents()) {
            if (child instanceof TimeField || child instanceof JSpinner || child instanceof JComboBox || child instanceof JRadioButton
                    || child instanceof com.jidesoft.swing.JideSplitButton || child instanceof org.helioviewer.jhv.gui.component.ChoicePicker) {
                Rectangle rectangle = SwingUtilities.convertRectangle(parent, child.getBounds(), root);
                trace.append(child.getClass().getSimpleName()).append(' ').append(rectangle.x).append(' ').append(rectangle.y).append(' ')
                        .append(rectangle.width).append(' ').append(rectangle.height).append(' ').append(child.isEnabled()).append('\n');
            } else if (child instanceof Container container) bounds(container, root, trace);
        }
    }

    private static void timeAndSampling() throws Exception {
        ImageRequestSettings settings = ImageRequestSettings.instance();
        settings.setInterval(START, START + 2 * DAY);
        settings.setSampling(new ImageRequestSettings.TimeStep(1800));
        ImageRequestScreen screen = new ImageRequestScreen();
        SwingPanel swing = new SwingPanel(screen);
        swing.setSize(swing.getPreferredSize());
        layout(swing);
        if (org.helioviewer.jhv.app.Platform.isMacOS()) {
            StringBuilder trace = new StringBuilder("size " + swing.getWidth() + " " + swing.getHeight() + "\n");
            bounds(swing, swing, trace);
            String reference = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("jhv.image.reference")));
            check(trace.toString().equals(reference), "original Swing request selector bounds and enabled roles\n" + trace);
        }
        List<TimeField> fields = widgets(swing, TimeField.class);
        settings.setInterval(START - DAY, START);
        swing.refresh();
        check(fields.getFirst().getTime() == START - DAY && fields.get(1).getTime() == START, "Sidebar missed an external request interval change");
        settings.setInterval(START, START + 2 * DAY);
        swing.refresh();
        TimeField start = fields.getFirst();
        start.setText("2025-01-02T12:00:00");
        for (FocusListener listener : start.getFocusListeners()) listener.focusLost(new FocusEvent(start, FocusEvent.FOCUS_LOST));
        check(settings.start() == START + DAY && fields.get(1).getTime() == START + 2 * DAY, "real field focus loss commits and preserves end");
        screen.choice(0, 10, 1);
        screen.command(0, 3, Panel.Command.CLICK);
        screen.command(0, 2, Panel.Command.CLICK);
        check(settings.start() == START + 7 * DAY, "independent forward week and backward day units");
        screen.number(0, 11, START + 10 * DAY - 12 * (60 * TimeUtils.MINUTE_IN_MILLIS));
        check(Math.floorMod(settings.start(), DAY) == 12 * (60 * TimeUtils.MINUTE_IN_MILLIS), "picked date preserves field time of day");
        int rotation = TimeSelection.rotation(START);
        screen.choice(0, 5, rotation);
        check(settings.start() == TimeUtils.floorSec(Carrington.CR_start[rotation]) && settings.end() == TimeUtils.floorSec(TimeSelection.rotationEnd(rotation)), "Carrington span");
        screen.setText(0, 0, "9999-01-01T00:00:00");
        check(settings.start() == TimeUtils.floorSec(Carrington.CR_start[rotation]), "out of range field preserves start");
        swing.refresh();
        List<JSpinner> spinners = widgets(swing, JSpinner.class);
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinners.getFirst().getEditor();
        editor.getTextField().setText("31");
        check(settings.sampling().equals(new ImageRequestSettings.TimeStep(1860)), "typed cadence commits immediately");
        editor.getTextField().setText("10001");
        check(settings.sampling().equals(new ImageRequestSettings.TimeStep(1860)), "invalid cadence keeps accepted value");
        JComboBox<?> units = widgets(swing, JComboBox.class).getFirst();
        units.setSelectedIndex(0);
        spinners.getFirst().setValue(60);
        check(units.getSelectedIndex() == 0 && settings.cadence() == 60, "equivalent cadence preserves chosen seconds unit");
        units.setSelectedIndex(4);
        check(settings.sampling() instanceof ImageRequestSettings.All && !spinners.getFirst().isEnabled(), "get all disables cadence");
        widgets(swing, JRadioButton.class).get(1).doClick();
        ((JSpinner.DefaultEditor) spinners.get(1).getEditor()).getTextField().setText("123");
        check(settings.sampling().equals(new ImageRequestSettings.FrameCount(123)) && !units.isEnabled(), "frame count typed event and enabled roles");
        widgets(swing, JRadioButton.class).getFirst().doClick();
        check(settings.sampling() instanceof ImageRequestSettings.All, "switch restores remembered get all preset");
        System.out.println("PASS time focus events, separate shifts, picked date, Carrington spans and real sampling widget events");
    }

    private static void imageBounds() throws Exception {
        DialogScreenTest.theme();
        ImageScreen screen = new ImageScreen((target, request) -> {}, url -> {});
        screen.setupSources(catalog("ROB"));
        SwingPanel swing = new SwingPanel(screen);
        swing.setSize(swing.getPreferredSize());
        layout(swing);
        List<org.helioviewer.jhv.gui.component.HTMLPane> labels = widgets(swing, org.helioviewer.jhv.gui.component.HTMLPane.class);
        check(labels.size() == 1 && labels.getFirst().isFocusable() && !labels.getFirst().isEditable(), "selectable extent retains its read-only focus role");
        List<javax.swing.JButton> buttons = widgets(swing, javax.swing.JButton.class).stream()
                .filter(button -> List.of("Available data", "Cancel", "Add").contains(button.getText())).toList();
        check(buttons.size() == 3 && buttons.get(0).getWidth() == buttons.get(1).getWidth() && buttons.get(1).getWidth() == buttons.get(2).getWidth(), "image footer keeps equal button widths");
        if (org.helioviewer.jhv.app.Platform.isMacOS()) {
            check(swing.getSize().equals(new java.awt.Dimension(352, 400)), "original image dialog content size " + swing.getSize());
            Rectangle extent = SwingUtilities.convertRectangle(labels.getFirst().getParent(), labels.getFirst().getBounds(), swing);
            check(extent.equals(new Rectangle(3, 353, 346, 19)), "original extent bounds " + extent);
            javax.swing.JScrollPane scroll = widgets(swing, javax.swing.JScrollPane.class).getFirst();
            check(SwingUtilities.convertRectangle(scroll.getParent(), scroll.getBounds(), swing).equals(new Rectangle(3, 3, 346, 350)), "original source viewport bounds");
            for (int i = 0; i < buttons.size(); i++)
                check(SwingUtilities.convertRectangle(buttons.get(i).getParent(), buttons.get(i).getBounds(), swing).equals(new Rectangle(i * 119, 375, 114, 25)), "original image footer button bounds");
        }
        System.out.println("PASS original image content bounds, equal footer widths and selectable extent focus role");
    }

    public static void main(String[] args) throws Exception {
        org.helioviewer.jhv.app.Platform.init();
        org.helioviewer.jhv.io.Directories.createPersistentDirs();
        org.helioviewer.jhv.app.Log.init();
        org.helioviewer.jhv.io.Directories.createCacheDirs();
        org.helioviewer.jhv.app.AppInit.loadSpice();
        DataSources.initSources();
        com.formdev.flatlaf.FlatLightLaf.setup();
        SwingUtilities.invokeAndWait(() -> {
            try { sources(); treeRetention(); timeAndSampling(); imageBounds(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("ImageScreenTest passed");
    }
}
