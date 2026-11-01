package org.helioviewer.jhv.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.band.BandDataset;
import org.helioviewer.jhv.timelines.band.BandType;

// The timeline catalogs as a tree of groups and datasets. Selected nodes add their datasets; a catalog is only a heading.
public final class TimelineScreen extends DialogScreen {

    private static final class Node {
        final String label;
        final List<BandType> types;
        final List<Node> children = new ArrayList<>();
        int id;

        Node(String _label, List<BandType> _types) {
            label = _label;
            types = _types;
        }

        // Children before their parent, as a depth-first enumeration of the tree.
        void collect(LinkedHashSet<BandType> into) {
            for (Node child : children)
                child.collect(into);
            into.addAll(types);
        }
    }

    private static final int WIDTH = 350;
    private static final int HEIGHT = 350;
    private static final int TREE = 1;
    private static final int ADD = 1;
    private static final int CANCEL = 2;
    private static final int FULL_RESOLUTION = 3;
    private static final int FIRST_NODE = 1000;

    private final Panel buffer = new Panel();
    private final BiConsumer<List<BandType>, Boolean> load;
    // What an empty tree says, for a host without a chart to show the catalogs' state.
    @Nullable
    private final String empty;
    private final ArrayList<Node> catalogs = new ArrayList<>();
    private final ArrayList<Node> nodes = new ArrayList<>();
    private final LinkedHashSet<Node> selected = new LinkedHashSet<>();
    private boolean fullResolution;

    public TimelineScreen(@Nullable String _empty) {
        this(_empty, TimelineLayers::addBands);
    }

    TimelineScreen(@Nullable String _empty, BiConsumer<List<BandType>, Boolean> _load) {
        empty = _empty;
        load = _load;
    }

    @Override
    public String title() {
        return "New Timeline Layer";
    }

    public void setCatalogs(Map<String, BandDataset[]> datasets) {
        catalogs.clear();
        selected.clear();
        for (Map.Entry<String, BandDataset[]> entry : datasets.entrySet()) {
            Node catalog = new Node(entry.getKey(), List.of());
            catalogs.add(catalog);
            LinkedHashMap<String, Node> groups = new LinkedHashMap<>();
            for (BandDataset dataset : entry.getValue()) {
                LinkedHashMap<String, List<BandType>> groupedTypes = new LinkedHashMap<>();
                List<BandType> ungroupedTypes = new ArrayList<>();
                for (BandType type : dataset.bandTypes()) {
                    if (type.getGroups().isEmpty()) {
                        ungroupedTypes.add(type);
                    } else {
                        for (String group : type.getGroups())
                            groupedTypes.computeIfAbsent(group, ignored -> new ArrayList<>()).add(type);
                    }
                }
                groupedTypes.forEach((group, types) -> groups.computeIfAbsent(group, name -> {
                    Node node = new Node(name, List.of());
                    catalog.children.add(node);
                    return node;
                }).children.add(new Node(dataset.title(), types)));
                if (!ungroupedTypes.isEmpty())
                    catalog.children.add(new Node(dataset.title(), ungroupedTypes));
            }
        }
        nodes.clear();
        catalogs.forEach(this::register);
        changed();
    }

    private void register(Node node) {
        node.id = FIRST_NODE + nodes.size();
        nodes.add(node);
        node.children.forEach(this::register);
    }

    private void node(Panel p, Node node, boolean open) {
        if (node.children.isEmpty()) {
            p.item(node.id, node.label, selected.contains(node)).selectable().multiple(true);
        } else {
            p.tree(node.id, node.label, open).selectable().multiple(true).selected(selected.contains(node));
            for (Node child : node.children)
                node(p, child, false);
            p.treeEnd();
        }
    }

    @Override
    public Panel build() {
        Panel p = buffer.reset().column().margin(3, 3, 3, 3).treeView(WIDTH, HEIGHT).identity(TREE).initialFocus();
        if (catalogs.isEmpty() && empty != null)
            p.item(0, empty, false).enabled(false);
        for (int i = 0; i < catalogs.size(); i++)
            node(p, catalogs.get(i), i == 0);
        return p.treeViewEnd().check(FULL_RESOLUTION, "Full time resolution", fullResolution).fill().tip("Load original HAPI samples without one-minute rebinning.")
                .columnEnd().row().right().gap(5).button(CANCEL, "Cancel").button(ADD, "Add").defaultButton().rowEnd();
    }

    private void add() {
        LinkedHashSet<BandType> types = new LinkedHashSet<>();
        for (Node node : selected) {
            if (!catalogs.contains(node))
                node.collect(types);
        }
        if (types.isEmpty())
            return;
        load.accept(List.copyOf(types), fullResolution);
        close();
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (item < FIRST_NODE) {
            switch (item) {
                case ADD -> add();
                case CANCEL -> close();
                default -> throw new IllegalArgumentException("Timeline item " + item);
            }
            return;
        }
        Node node = nodes.get(item - FIRST_NODE);
        switch (command) {
            case SELECT_ONLY, CLICK -> {
                selected.clear();
                selected.add(node);
            }
            case SELECT_ADD -> selected.add(node);
            case SELECT_REMOVE -> selected.remove(node);
            case DOUBLE_CLICK -> add();
            default -> {}
        }
        changed();
    }

    @Override
    public void checked(int owner, int item, boolean value) {
        fullResolution = value;
        changed();
    }

}
