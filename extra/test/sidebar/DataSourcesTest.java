package org.helioviewer.jhv.io;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.component.DataSourcesTree;
import org.helioviewer.jhv.gui.dialog.ImageDialog;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

public final class DataSourcesTest {

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        DataSources.initSources();
        Settings.setProperty("dataSources.defaultServer", "ROB");
        DataSourcesParser parser = new DataSourcesParser("ROB", catalog());
        checkParser(parser);
        UiThread.postAndWait(() -> {
            checkTree(parser);
            checkDialog(parser);
        });
        System.out.println("DataSourcesTest passed");
    }

    private static JSONObject leaf(String name, int id, boolean selectedDefault) {
        return new JSONObject().put("name", name).put("description", "Description " + name).put("sourceId", id)
                .put("start", "2020-01-02 03:04:05").put("end", "2021-02-03 04:05:06").put("default", selectedDefault);
    }

    private static JSONObject group(String name, JSONObject children) {
        return new JSONObject().put("name", name).put("description", "Description " + name).put("children", children);
    }

    private static JSONObject catalog() {
        JSONObject missingStart = leaf("missing", 102, true);
        missingStart.remove("start");
        JSONObject children = new JSONObject()
                .put("channel10", leaf("304", 10, true))
                .put("channel2", leaf("171", 2, true))
                .put("repeat", group("Same", new JSONObject().put("x", group("Same",
                        new JSONObject().put("x", leaf("Same", 33, false))))))
                .put("space", leaf("X\u205fY_under", 34, false))
                .put("skip1", leaf("empty", 101, true).put("end", JSONObject.NULL))
                .put("skip2", missingStart);
        return new JSONObject().put("group10", group("Empty_10", new JSONObject()))
                .put("group2", group("Group_2\u205fA", children))
                .put("group20", group("Last", new JSONObject().put("x", leaf("Final", 50, true))));
    }

    private static void checkParser(DataSourcesParser parser) {
        List<DataSourcesParser.Group> groups = parser.getGroups();
        check(groups.size() == 3 && groups.get(0).item().name.equals("Group-2 A")
                && groups.get(1).item().name.equals("Empty-10") && groups.get(2).item().name.equals("Last"),
                "natural group order and normalized group names");
        check(groups.get(1).sources().isEmpty(), "empty groups are retained");
        List<DataSourcesParser.SourceItem> sources = groups.getFirst().sources();
        check(ids(sources).equals(List.of(2, 10, 33, 34)), "natural source order and skipped empty datasets");
        check(sources.get(2).name.equals("Same") && sources.get(3).name.equals("X Y_under"),
                "nested names merge without repetition and source underscores remain unchanged");
        DataSourcesParser.SourceItem first = sources.getFirst();
        check(first.server.equals("ROB") && first.description.equals("Description 171")
                && first.start == Instant.parse("2020-01-02T03:04:05Z").toEpochMilli()
                && first.end == Instant.parse("2021-02-03T04:05:06Z").toEpochMilli(), "source fields and UTC extent");
        check(parser.getDefault().sourceId == 50, "last default in traversal order wins");
        check(DataSources.selectDataset("ROB", "Group-2 A", "171") == 2, "flattened source registration");
        check(DataSources.selectDataset("ROB", "Group-2 A", "empty") == -1, "empty source is not registered");

        DataSourcesParser empty = new DataSourcesParser("empty", new JSONObject());
        check(empty.getGroups().isEmpty() && empty.getDefault() == null, "empty catalog");
        DataSourcesParser skipped = new DataSourcesParser("skipped", new JSONObject().put("x", group("G",
                new JSONObject().put("x", leaf("skip", 1, true).put("start", JSONObject.NULL)))));
        check(skipped.getGroups().getFirst().sources().isEmpty() && skipped.getDefault() == null, "skipped default is ignored");
        DataSourcesParser duplicate = new DataSourcesParser("duplicate", new JSONObject()
                .put("group2", group("Earlier", new JSONObject().put("x", leaf("old", 7, false))))
                .put("group10", group("Later", new JSONObject().put("x", leaf("new", 7, false)))));
        check(duplicate.getGroups().size() == 2 && DataSources.selectDataset("duplicate", "Earlier", "old") == -1
                && DataSources.selectDataset("duplicate", "Later", "new") == 7, "later registration replaces a duplicate ID");
        try {
            new DataSourcesParser("invalid", new JSONObject().put("x", leaf("root", 1, false)));
            throw new AssertionError("top-level source was accepted as a group");
        } catch (JSONException expected) {
        }
    }

    private static void checkTree(DataSourcesParser parser) {
        DataSourcesTree tree = new DataSourcesTree(() -> {});
        try {
            tree.setParsedData(parser);
            check(tree.getSelectedItems().isEmpty(), "installing a catalog does not select its default");
            tree.setSelectedItem("ROB", 10);
            TreePath sourcePath = tree.getSelectionPath();
            check(ids(tree.getSelectedItems()).equals(List.of(10)), "selection by server and source ID");
            DefaultMutableTreeNode source = (DefaultMutableTreeNode) sourcePath.getLastPathComponent();
            DefaultMutableTreeNode group = (DefaultMutableTreeNode) source.getParent();
            check(!source.getAllowsChildren() && group.getAllowsChildren() && group.getChildCount() == 4,
                    "tree has group nodes with flat source leaves");
            tree.setSize(1000, 1000);
            tree.expandPath(sourcePath.getParentPath());
            Rectangle bounds = tree.getPathBounds(sourcePath);
            check(bounds != null && tree.getToolTipText(new MouseEvent(tree, MouseEvent.MOUSE_MOVED, 0, 0,
                    bounds.x + 1, bounds.y + 1, 0, false)).equals("Description 304"), "source tooltip");
            check(tree.getToolTipText(null) == null, "null tooltip event");

            TreePath firstPath = new TreePath(((DefaultMutableTreeNode) group.getChildAt(0)).getPath());
            tree.setSelectionPaths(new TreePath[]{firstPath, sourcePath});
            check(ids(tree.getSelectedItems()).equals(List.of(2, 10)), "multiple source selection");
            tree.setSelectionPath(sourcePath.getParentPath());
            check(tree.getSelectedItems().isEmpty(), "group selection contains no source");
            tree.setSelectedItem("ROB", 10);
            DataSourcesParser other = new DataSourcesParser("GSFC", catalog());
            tree.setParsedData(other);
            check(tree.getSelectedItems().getFirst().server.equals("ROB"), "another catalog preserves the existing selection");
            tree.setSelectedItem("GSFC", 10);
            check(tree.getSelectedItems().getFirst().server.equals("GSFC") && ids(tree.getSelectedItems()).equals(List.of(10)),
                    "source IDs are selected within the requested server");
            tree.setSelectedItem("missing", 10);
            tree.setSelectedItem("ROB", -1);
            check(tree.getSelectedItems().getFirst().server.equals("GSFC") && ids(tree.getSelectedItems()).equals(List.of(10)),
                    "missing server or source preserves the existing selection");
            tree.setSelectedItem("ROB", 10);
            tree.setParsedData(new DataSourcesParser("ROB", new JSONObject()));
            check(tree.getSelectedItems().isEmpty(), "catalog replacement removes stale selection");
        } finally {
            ToolTipManager.sharedInstance().unregisterComponent(tree);
        }
    }

    private static List<Integer> ids(List<DataSourcesParser.SourceItem> sources) {
        List<Integer> ids = new ArrayList<>();
        for (DataSourcesParser.SourceItem source : sources)
            ids.add(source.sourceId);
        return ids;
    }

    private static void checkDialog(DataSourcesParser parser) {
        ImageDialog dialog = new ImageDialog();
        JComponent content = dialog.createContentPanel();
        DataSourcesTree tree = (DataSourcesTree) ((JScrollPane) content.getComponent(0)).getViewport().getView();
        try {
            ImageRequestSettings settings = ImageRequestSettings.instance();
            long start = Instant.parse("2018-01-01T00:00:00Z").toEpochMilli();
            long end = start + TimeUtils.DAY_IN_MILLIS;
            settings.setInterval(start, end);
            dialog.setupSources(parser);
            long defaultEnd = Instant.parse("2021-02-03T04:05:06Z").toEpochMilli();
            check(ids(tree.getSelectedItems()).equals(List.of(50)) && settings.end() == defaultEnd
                    && settings.start() == defaultEnd - 2 * TimeUtils.DAY_IN_MILLIS,
                    "dialog selects the preferred server default and its last two days");

            settings.setInterval(start, end);
            dialog.setupSources(new DataSourcesParser("GSFC", catalog()));
            check(tree.getSelectedItems().getFirst().server.equals("ROB") && settings.start() == start && settings.end() == end,
                    "nonpreferred catalog preserves selection and interval");
            dialog.setupSources(new DataSourcesParser("ROB", new JSONObject()));
            check(tree.getSelectedItems().isEmpty() && settings.start() == start && settings.end() == end,
                    "catalog without a default preserves the interval");
        } finally {
            ToolTipManager.sharedInstance().unregisterComponent(tree);
            dialog.dispose();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
