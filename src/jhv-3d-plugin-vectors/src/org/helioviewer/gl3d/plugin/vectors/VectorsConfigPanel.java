package org.helioviewer.gl3d.plugin.vectors;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.helioviewer.base.logging.Log;

/**
 * UI Configuration Panel for the Vectors Plugin. It allows reading file based
 * vector fields.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class VectorsConfigPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = -4872841345586612164L;

    private VectorsPlugin plugin;

    private VectorsFileListModel fileModel;

    public VectorsConfigPanel(VectorsPlugin plugin) {
        this.plugin = plugin;
    }

    protected void init() {
        final JPanel that = this;

        this.setLayout(new BorderLayout());

        this.add(new JLabel("Loaded PFSS Models:"), BorderLayout.NORTH);

        this.fileModel = new VectorsFileListModel();
        final JList fileList = new JList(this.fileModel);
        JScrollPane scroll = new JScrollPane(fileList);
        this.add(scroll, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            File lastFile = null;

            public void actionPerformed(ActionEvent e) {
                if (lastFile == null) {
                    // lastFile = new File(System.getProperty("user.dir"));
                    lastFile = new File("/Users/simon/I4DS/JHelioviewer/ws/jhv-3d-wcs/src/jhv-3d-plugin-pfss/resources/fits");
                }
                JFileChooser chooser = new JFileChooser(lastFile);
                chooser.setMultiSelectionEnabled(true);
                if (chooser.showOpenDialog(that) == JFileChooser.APPROVE_OPTION) {
                    Log.debug("Loading PFSS Files...");
                    for (File f : chooser.getSelectedFiles()) {
                        VectorsFileModel model = new VectorsFileModel(f);
                        if (model.load()) {
                            fileModel.addModel(model);
                            plugin.fireModelLoaded(model.getRoot());
                        }
                        lastFile = f;
                    }
                    that.revalidate();
                    that.repaint();
                }
            }
        });
        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                int modelIndex = fileList.getSelectedIndex();
                VectorsFileModel model = fileModel.models.get(modelIndex);
                plugin.fireModelUnloaded(model.getRoot());
                fileModel.removeModel(modelIndex);
            }
        });

        fileList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (fileList.isSelectionEmpty()) {
                    removeButton.setEnabled(false);
                } else {
                    removeButton.setEnabled(true);
                }
            }
        });

        controls.add(addButton);
        controls.add(removeButton);

        this.add(controls, BorderLayout.SOUTH);
    }

    private class VectorsFileListModel extends DefaultListModel {

        /**
         * 
         */
        private static final long serialVersionUID = 5883625984602595143L;
        private List<VectorsFileModel> models = new ArrayList<VectorsFileModel>();

        public VectorsFileListModel() {
        }

        public void removeModel(int index) {
            models.remove(index);
            this.fireIntervalRemoved(this, index, index);
        }

        public void addModel(VectorsFileModel model) {
            this.models.add(model);
            this.fireIntervalAdded(this, this.models.size() - 1, this.models.size());
        }

        public Object getElementAt(int index) {
            return models.get(index);
        }

        public int getSize() {
            return models.size();
        }
    }
}
