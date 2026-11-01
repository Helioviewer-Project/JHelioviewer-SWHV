package org.helioviewer.jhv.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.ImageRequestSettings;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.time.TimeUtils;

// Image dataset selection and loading for both window adapters.
public final class ImageScreen extends DialogScreen implements Layers.Listener {

    private static final int TREE_WIDTH = 250;
    private static final int TREE_HEIGHT = 350;

    private final Panel buffer = new Panel();

    private static final int ADD = 1;
    private static final int CANCEL = 2;
    private static final int AVAILABLE = 3;
    private static final int NODE = 1000;

    private final LinkedHashMap<String, DataSourcesParser> sources = new LinkedHashMap<>();
    private final ArrayList<DataSourcesParser.Item> items = new ArrayList<>();
    private final LinkedHashSet<DataSourcesParser.Item> selected = new LinkedHashSet<>();
    private final LinkedHashMap<String, DataSourcesParser.Item> servers = new LinkedHashMap<>();
    // The layer whose dataset changes, else a new layer.
    @Nullable
    private ImageLayer target;
    private final BiConsumer<ImageLayer, APIRequest> load;
    private final Consumer<String> openUrl;

    public ImageScreen(Consumer<String> _openUrl) {
        this((layerToReplace, request) -> {
            ImageLayer layer = layerToReplace == null ? ImageLayer.create() : layerToReplace;
            layer.load(request);
        }, _openUrl);
        DataSources.setListener(this::setupSources);
        Layers.addListener(this);
    }

    ImageScreen(BiConsumer<ImageLayer, APIRequest> _load, Consumer<String> _openUrl) {
        load = _load;
        openUrl = _openUrl;
        for (String server : DataSources.getServers())
            servers.put(server, new DataSourcesParser.Item(server, DataSources.getServer(server).label()));
    }

    // Datasets of a server arrived; the preferred server's default dataset is selected and sets the request interval, as the Swing dialog does.
    public void setupSources(DataSourcesParser parser) {
        DataSourcesParser previous = sources.put(parser.getServer(), parser);
        selected.removeIf(item -> item instanceof DataSourcesParser.SourceItem source && source.server.equals(parser.getServer())
                || previous != null && previous.getGroups().stream().anyMatch(group -> group.item() == item));
        if (parser.getServer().equals(Settings.getProperty("dataSources.defaultServer"))) {
            DataSourcesParser.SourceItem item = parser.getDefault();
            if (item != null) {
                selected.clear();
                selected.add(item);
                ImageRequestSettings.instance().setInterval(item.end - 2 * TimeUtils.DAY_IN_MILLIS, item.end);
            }
        }
        changed();
    }

    private boolean containsSelected(DataSourcesParser.Group group) {
        for (DataSourcesParser.SourceItem item : group.sources()) {
            if (selected.contains(item))
                return true;
        }
        return false;
    }

    @Override
    public void show() {
        show(null);
    }

    public void show(@Nullable ImageLayer layer) {
        target = layer;
        if (layer != null) {
            APIRequest request = layer.getAPIRequest();
            if (request != null) {
                DataSourcesParser parser = sources.get(request.server());
                if (parser != null) {
                    for (DataSourcesParser.Group group : parser.getGroups()) {
                        for (DataSourcesParser.SourceItem item : group.sources()) {
                            if (item.sourceId == request.sourceId()) {
                                selected.clear();
                                selected.add(item);
                            }
                        }
                    }
                }
            }
            if (selected.size() > 1) {
                DataSourcesParser.Item first = selected.iterator().next();
                selected.clear();
                selected.add(first);
            }
        }
        changed();
    }

    @Nullable
    private String availabilityUrl() {
        List<DataSourcesParser.SourceItem> datasets = selectedSources();
        if (datasets.size() != 1)
            return null;
        DataSourcesParser.SourceItem item = datasets.getFirst();
        DataSources.Server server = DataSources.getServer(item.server);
        String availability = server == null ? null : server.availabilityURL();
        return availability == null ? null : availability + "ID=" + item.sourceId;
    }

    @Override
    public String title() {
        return target == null ? "New Image Layer" : "Change Dataset";
    }

    @Override
    public Panel build() {
        Panel p = buffer.reset().column().swingMargin(0, 0, 0, 0).column().margin(3, 3, 3, 3).treeView(TREE_WIDTH, TREE_HEIGHT).multiple(target == null).doubleClickColumns(0).initialFocus();
        items.clear();
        boolean multiple = target == null;
        for (String server : DataSources.getServers()) {
            DataSourcesParser parser = sources.get(server);
            DataSources.Server description = DataSources.getServer(server);
            p.tree(NODE + items.size(), server, parser != null && parser.getGroups().stream().anyMatch(this::containsSelected));
            if (description != null)
                p.tip(description.label());
            p.selected(selected.contains(servers.get(server)));
            items.add(servers.get(server));
            if (parser == null) {
                p.treeEnd();
                continue;
            }
            for (DataSourcesParser.Group group : parser.getGroups()) {
                p.tree(NODE + items.size(), group.item().name, containsSelected(group)).tip(group.item().description);
                p.selected(selected.contains(group.item()));
                items.add(group.item());
                for (DataSourcesParser.SourceItem item : group.sources()) {
                    p.item(NODE + items.size(), item.name, selected.contains(item)).multiple(multiple).tip(item.description);
                    items.add(item);
                }
                p.treeEnd();
            }
            p.treeEnd();
        }
        String extent = "";
        List<DataSourcesParser.SourceItem> datasets = selectedSources();
        if (datasets.size() == 1) {
            DataSourcesParser.SourceItem item = datasets.getFirst();
            extent = "Extent: " + TimeUtils.formatShort(item.start) + " - " + TimeUtils.formatShort(item.end);
        } else if (!datasets.isEmpty()) {
            extent = datasets.size() + " datasets selected";
        }
        return p.treeViewEnd().label(extent).selectable().center().wrap().columnEnd()
                .row().gap(5)
                .button(AVAILABLE, "Available data").enabled(availabilityUrl() != null)
                .row().right().gap(5)
                .button(CANCEL, "Cancel").widthText("Available data")
                .button(ADD, target == null ? "Add" : "Change").widthText("Available data").defaultButton().enabled(!datasets.isEmpty())
                .rowEnd().rowEnd().columnEnd();
    }

    @Override
    public void close() {
        target = null;
        super.close();
    }

    @Override
    public void layerRemoved(int index, Layer layer) {
        if (target == layer)
            close();
    }

    @Override
    public void layersCleared() {
        if (target != null)
            close();
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (item >= NODE) {
            int i = item - NODE;
            if (i < items.size()) {
                DataSourcesParser.Item source = items.get(i);
                if (command == Panel.Command.DOUBLE_CLICK) {
                    if (source instanceof DataSourcesParser.SourceItem) add();
                    return;
                }
                if (command == Panel.Command.SELECT_REMOVE) {
                    selected.remove(source);
                } else {
                    if (command != Panel.Command.SELECT_ADD || target != null)
                        selected.clear();
                    if (source != null)
                        selected.add(source);
                }
                changed();
            }
            return;
        }
        switch (item) {
            case ADD -> add();
            case AVAILABLE -> {
                String url = availabilityUrl();
                if (url != null)
                    openUrl.accept(url);
            }
            case CANCEL -> close();
            default -> throw new IllegalArgumentException("New layer item " + item);
        }
    }

    private List<DataSourcesParser.SourceItem> selectedSources() {
        return selected.stream().filter(DataSourcesParser.SourceItem.class::isInstance).map(DataSourcesParser.SourceItem.class::cast).toList();
    }

    // Loads each selected dataset with the request settings, as the Swing dialog does.
    private void add() {
        List<DataSourcesParser.SourceItem> datasets = selectedSources();
        if (datasets.isEmpty())
            return;
        ImageLayer current = target;
        close();
        for (DataSourcesParser.SourceItem item : datasets) {
            load.accept(current, ImageRequestSettings.instance().request(item.server, item.sourceId));
        }
    }

}
