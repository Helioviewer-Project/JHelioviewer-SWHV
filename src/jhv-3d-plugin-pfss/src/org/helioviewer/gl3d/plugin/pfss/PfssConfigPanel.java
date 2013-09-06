package org.helioviewer.gl3d.plugin.pfss;

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
 * The UI configuration component for the pfss plugin
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class PfssConfigPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 8535729209025947878L;

    private PfssPlugin plugin;

    private PfssFileListModel fileModel;

    public PfssConfigPanel(PfssPlugin plugin) {
        this.plugin = plugin;
    }

    protected void init() {
        final JPanel that = this;

        this.setLayout(new BorderLayout());

        this.add(new JLabel("Loaded PFSS Models:"), BorderLayout.NORTH);

        this.fileModel = new PfssFileListModel();
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
                    lastFile = new File("/Users/simon/I4DS/JHelioviewer/ws/jhv-3d-wcs/src/jhv-3d-plugin-pfss/resources/pfss");
                    // lastFile = new File("/Users/simon/Downloads");
                }
                JFileChooser chooser = new JFileChooser(lastFile);
                chooser.setMultiSelectionEnabled(true);
                if (chooser.showOpenDialog(that) == JFileChooser.APPROVE_OPTION) {
                    Log.debug("Loading PFSS Files...");
                    for (File f : chooser.getSelectedFiles()) {
                        PfssFileModel model = new PfssFileModel(f);
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
                PfssFileModel model = fileModel.models.get(modelIndex);
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

    private class PfssFileListModel extends DefaultListModel {

        /**
         * 
         */
        private static final long serialVersionUID = -2994377168813184873L;
        private List<PfssFileModel> models = new ArrayList<PfssFileModel>();

        public PfssFileListModel() {
        }

        public void removeModel(int index) {
            models.remove(index);
            this.fireIntervalRemoved(this, index, index);
        }

        public void addModel(PfssFileModel model) {
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
