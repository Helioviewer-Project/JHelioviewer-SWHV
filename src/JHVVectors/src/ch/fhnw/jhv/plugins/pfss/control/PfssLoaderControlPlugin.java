package ch.fhnw.jhv.plugins.pfss.control;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;

import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;
import ch.fhnw.jhv.plugins.pfss.data.IncorrectPfssFileException;
import ch.fhnw.jhv.plugins.pfss.data.PfssImporter;
import ch.fhnw.jhv.plugins.pfss.rendering.PfssRenderer;
import ch.fhnw.jhv.plugins.vectors.control.SpringUtilities;

/**
 * Provides a List to load PFSS-Text Files.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         14.08.2011
 */
public class PfssLoaderControlPlugin extends AbstractPlugin implements ControlPlugin, ActionListener {

    /**
     * The Panel that contains all the controls
     */
    JPanel panel;

    /**
     * The Panel that contains all the controls
     */
    JPanel formular;

    /**
     * List for TXT-Files that contain PFSS-exported points
     */
    JList fileList;

    /**
     * File List Model
     */
    DefaultListModel fileListModel;

    /**
     * FileChooser dialog
     */
    JFileChooser chooser;

    /**
     * JSpinner to specifiy the input "Curve Precision" The curve percision is a
     * numeric value. If precision is 3, only every third point is read from the
     * input file.
     */
    JSpinner curvePrecision;

    /**
     * add PFSS-Export File
     */
    JButton addButton;
    /**
     * Remove files from list
     */
    JButton removeButton;

    /**
     * Load selected PFSS Files
     */
    JButton loadButton;

    /**
     * load PFSS Example Button
     */
    JButton loadExample;

    private static Icon addIcon = new ImageIcon(PfssLoaderControlPlugin.class.getResource("/icons/edit_add.png"));
    private static Icon removeIcon = new ImageIcon(PfssLoaderControlPlugin.class.getResource("/icons/edit_remove.png"));

    /**
     * Container-Class for storing filename and full-path in files list.
     * 
     * @author David Hostettler (davidhostettler@gmail.com)
     * 
     *         14.08.2011
     */
    private class FileListItem {
        protected String path;
        protected String name;

        public String toString() {
            return name;
        }
    }

    /**
     * Constructor
     */
    /**
     * Constructor
     */
    public PfssLoaderControlPlugin() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        formular = new JPanel(new SpringLayout());

        // Jspinner to specify curve precision
        formular.add(new JLabel("Curve Precision"));
        curvePrecision = new JSpinner(new SpinnerNumberModel(10, 1, 40, 1));
        curvePrecision.setToolTipText("Define the offset of the used curve points. If you set the value to 10. Every 10th point will be used for the line.");
        curvePrecision.setPreferredSize(new Dimension(70, 25));
        formular.add(curvePrecision);

        // FILE LOADER CONTROLS
        formular.add(new JLabel("Files"));
        JPanel loadFile = new JPanel();
        BoxLayout layout = new BoxLayout(loadFile, BoxLayout.Y_AXIS);
        loadFile.setLayout(layout);

        // list for selected files
        fileListModel = new DefaultListModel();
        fileList = new JList(fileListModel);
        fileList.setToolTipText("List contains the loaded PFSS files. Its possible to order the file by drag & drop.");
        fileList.setLayoutOrientation(JList.VERTICAL);

        // put the jlist inside a scrollpane, looks better.
        JScrollPane listScroller = new JScrollPane(fileList);
        listScroller.setPreferredSize(new Dimension(100, 80));
        loadFile.add(listScroller);

        // container for add and remove buttons
        JPanel fileButtons = new JPanel();

        // add button (for adding PFSS files)
        addButton = new JButton();
        addButton.setToolTipText("Load a PFSS File");
        addButton.setIcon(addIcon);
        addButton.addActionListener(this);

        fileButtons.add(addButton);

        // remove button (for removing selected files from jlist)
        removeButton = new JButton();
        removeButton.setToolTipText("Remove a PFSS file");
        removeButton.setIcon(removeIcon);
        removeButton.addActionListener(this);
        fileButtons.add(removeButton);

        // pfss load button
        loadButton = new JButton("Load Vectors");
        loadButton.setToolTipText("Load the PFSS visualization with the loaded PFSS data");
        panel.add(loadButton);
        loadButton.addActionListener(this);
        fileButtons.add(loadButton);

        // add all the buttons to the loadFile Panel
        loadFile.add(fileButtons);

        // add all the Components for loading files to the formular
        formular.add(loadFile);

        // add input formulars
        panel.add(formular);

        // Lay out the panel
        SpringUtilities.makeCompactGrid(formular, 2, 2, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#getTitle()
     */
    public String getTitle() {
        return "PFSS Export Loader";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#getComponent()
     */
    public JComponent getComponent() {
        return panel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#getType()
     */
    public ControlPluginType getType() {
        return ControlPluginType.PFSS_LOADER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#shouldStartExpanded()
     */
    public boolean shouldStartExpanded() {
        return true;
    }

    /**
     * Handles Add-File and Remove-File Clicks
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addButton) {
            // ADD FILE BUTTON CLICKED
            if (chooser == null) {
                chooser = new JFileChooser();
                chooser.setMultiSelectionEnabled(true);
            }
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                for (File f : chooser.getSelectedFiles()) {
                    // Fetch the date from the fits header
                    FileListItem item = new FileListItem();
                    item.name = f.getName();
                    item.path = f.getAbsolutePath();
                    fileListModel.addElement(item);
                }
            }
        } else if (e.getSource() == removeButton) {
            // REMOVE FILES BUTTON CLICKED
            int indices[] = fileList.getSelectedIndices();

            for (int i = indices.length - 1; i >= 0; i--) {
                fileListModel.remove(indices[i]);
            }
        } else if (e.getSource() == loadButton) {
            // LOAD PFSS Exports
            if (fileListModel.size() > 0) {
                List<String> paths = new LinkedList<String>();
                PfssImporter importer = new PfssImporter();

                for (int i = 0; i < fileListModel.getSize(); i++) {
                    FileListItem itm = (FileListItem) fileListModel.getElementAt(i);
                    paths.add(itm.path);
                }

                RenderPlugin plugin = PluginManager.getInstance().getRenderPluginByType(RenderPluginType.PFSSCURVES);
                PfssRenderer renderer = (PfssRenderer) plugin;
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    renderer.loadData(importer.readPfssExport(paths, (Integer) curvePrecision.getValue()));
                } catch (FileNotFoundException nofile) {
                    JOptionPane.showMessageDialog(panel, "Error while parsing", "Couldn't parse the PFSS file", JOptionPane.ERROR_MESSAGE);
                    nofile.printStackTrace();
                } catch (IncorrectPfssFileException incorretfile) {
                    JOptionPane.showMessageDialog(panel, "Incorrct File", "Unable to read PFSS Points from file, file format is incorrect.", JOptionPane.ERROR_MESSAGE);
                    incorretfile.printStackTrace();
                } catch (IOException ioex) {
                    JOptionPane.showMessageDialog(panel, "File Access", "Unable to read file.", JOptionPane.ERROR_MESSAGE);
                    ioex.printStackTrace();
                } finally {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            } else {
                JOptionPane.showMessageDialog(panel, "Please select at least one file.");
            }
        }
    }

}
