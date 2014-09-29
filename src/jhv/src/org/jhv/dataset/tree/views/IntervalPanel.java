package org.jhv.dataset.tree.views;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.jhv.dataset.tree.models.DatasetInterval;
import org.jhv.dataset.tree.models.DatasetLayer;
import org.jhv.dataset.tree.models.DatasetType;
import org.jhv.dataset.tree.models.LayersToDatasetLayers;

public class IntervalPanel extends DatasetPanel {
    private static final long serialVersionUID = 4342443227686604174L;
    DatasetInterval model;

    public IntervalPanel(final DatasetInterval model) {
        super();
        this.model = model;
        this.setPreferredSize(new Dimension(250, 19));
        this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JLabel label = new JLabel(model.getTitle());
        label.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                TreePath path = new TreePath(model.getModel().getPathToRoot(model));
                if (!model.getModel().getTree().isCollapsed(path)) {
                    for (int j = 0; j < model.datasetTypes.size(); j++) {
                        DatasetType type = model.datasetTypes.get(j);
                        for (int i = 0; i < type.datasetLayers.size(); i++) {
                            DatasetLayer layer = type.datasetLayers.get(i);

                            LayersToDatasetLayers layersModel = LayersToDatasetLayers.getSingletonInstance();
                            View view = layersModel.getView(layer.getDescriptor());
                            int index = layersModel.getIndex(layer.getDescriptor());
                            LayersModel.getSingletonInstance().setVisible(view, false);
                        }
                    }
                    model.getModel().getTree().collapsePath(path);
                } else {
                    for (int j = 0; j < model.datasetTypes.size(); j++) {
                        DatasetType type = model.datasetTypes.get(j);
                        for (int i = 0; i < type.datasetLayers.size(); i++) {
                            DatasetLayer layer = type.datasetLayers.get(i);

                            LayersToDatasetLayers layersModel = LayersToDatasetLayers.getSingletonInstance();
                            View view = layersModel.getView(layer.getDescriptor());
                            int index = layersModel.getIndex(layer.getDescriptor());
                            LayersModel.getSingletonInstance().setVisible(view, true);
                        }
                    }
                    model.getModel().getTree().expandPath(path);
                }
            }
        });
        add(label);

    }

}
