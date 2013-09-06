package ch.fhnw.jhv.plugins.vectors.control;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.vecmath.Vector2f;

import nom.tam.fits.FitsException;

import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;
import ch.fhnw.jhv.plugins.vectors.data.importer.InconsistentVectorfieldSizeException;
import ch.fhnw.jhv.plugins.vectors.data.importer.ObservationDateMissingException;

/**
 * Displays a Control Plugin with all the elements to load a Vectorfield
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         14.08.2011
 */
public class VectorsLoaderControlPlugin extends AbstractPlugin implements ControlPlugin, ActionListener {

    /**
     * The Panel that is actually displayed in the Application. It contains all
     * the controls and is returned by getComponent
     */
    private JPanel panel;

    /**
     * List to add Fits-Files
     */
    private JList fileList;

    /**
     * file open dialog
     */
    private JFileChooser chooser;

    /**
     * The ListModel that contains all the files
     */
    private DefaultListModel fileListModel;

    /**
     * Button to load the example
     */
    private JButton loadExample;

    /**
     * Button to load the example
     */
    private JButton loadHugeExample;

    /**
     * Button to load the medium example
     */
    private JButton loadMediumExample;

    /**
     * Input for width of vectorfield
     */
    private JSpinner widthArcsec;

    /**
     * input for height of vectorfield
     */
    private JSpinner heightArcsec;

    /**
     * Input for Position (X-Coordinate) of vectorfield
     */
    private JSpinner posArcsexX;

    /**
     * Input for Position (Y-Coordinate) of vectorfield
     */
    private JSpinner posArcsexY;

    /**
     * Button to add FITS-Files to the list
     */
    private JButton addButton;

    /**
     * Button to remove FITS-Files from the list
     */
    private JButton removeButton;

    /**
     * Button to load VectorField
     */
    private JButton loadButton;

    /**
     * number of columns that are inside formular. Information is necessary for
     * the SpringUtilities class to format the input fields
     */
    private int controlsCount = 3;

    /**
     * The Panel that contains the Input fields formated with
     * SpringUtilities.makeCompactGrid
     */
    private JPanel formular;

    /**
     * Add File Icon
     */
    private static Icon addIcon = new ImageIcon(VectorsLoaderControlPlugin.class.getResource("/icons/edit_add.png"));

    /**
     * Remove File Icon
     */
    private static Icon removeIcon = new ImageIcon(VectorsLoaderControlPlugin.class.getResource("/icons/edit_remove.png"));

    /**
     * Container Class for List-Entries in the File-List
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
     * Create the VectorsLoaderControlPlugin with all its controls
     */
    public VectorsLoaderControlPlugin() {

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        formular = new JPanel(new SpringLayout());

        // maximum arcseconds for input in jspinners
        float maxArcsec = 1000.0f;
        float maxSizeArcsec = 2000.0f;

        // WIDTH AND HEIGHT IN ARCSECONDS
        formular.add(new JLabel("Size (in arcsec)"));
        JPanel panelSize = new JPanel();
        widthArcsec = new JSpinner(new SpinnerNumberModel(0.0f, -maxSizeArcsec, maxSizeArcsec, 1.0f));
        widthArcsec.setToolTipText("Define the width of the loaded field (in arc seconds).");
        widthArcsec.setPreferredSize(new Dimension(70, 25));
        panelSize.add(widthArcsec);
        heightArcsec = new JSpinner(new SpinnerNumberModel(0.0f, -maxSizeArcsec, maxSizeArcsec, 1.0));
        heightArcsec.setToolTipText("Define the height of the loaded field (in arc seconds).");
        heightArcsec.setPreferredSize(new Dimension(70, 25));
        panelSize.add(heightArcsec);
        formular.add(panelSize);

        // POSITION X/Y IN ARCSECONDS
        formular.add(new JLabel("Pos  (in arcsec)"));
        JPanel panelPos = new JPanel();
        posArcsexX = new JSpinner(new SpinnerNumberModel(0.0f, -maxArcsec, maxArcsec, 1.0f));
        posArcsexX.setToolTipText("Define the position X on the sun (in arc seconds).");
        posArcsexX.setPreferredSize(new Dimension(70, 25));
        panelPos.add(posArcsexX);
        posArcsexY = new JSpinner(new SpinnerNumberModel(0.0f, -maxArcsec, maxArcsec, 1.0f));
        posArcsexY.setToolTipText("Define the position Y on the sun (in arc seconds).");
        posArcsexY.setPreferredSize(new Dimension(70, 25));
        panelPos.add(posArcsexY);
        formular.add(panelPos);

        // add input formulars
        panel.add(formular);

        // FILE LOADER CONTROLS
        formular.add(new JLabel("Files"));
        JPanel loadFile = new JPanel();
        BoxLayout layout = new BoxLayout(loadFile, BoxLayout.Y_AXIS);
        loadFile.setLayout(layout);

        // list for selected files
        fileListModel = new DefaultListModel();
        fileList = new JList(fileListModel);
        fileList.setToolTipText("All loaded FITS files will be listed here.");
        fileList.setLayoutOrientation(JList.VERTICAL);

        // put the jlist inside a scrollpane, looks better.
        JScrollPane listScroller = new JScrollPane(fileList);
        listScroller.setPreferredSize(new Dimension(100, 80));
        loadFile.add(listScroller);

        // container for add and remove buttons
        JPanel fileButtons = new JPanel();

        // add button (for adding FITS files)
        addButton = new JButton();
        addButton.setIcon(addIcon);
        addButton.setToolTipText("Add a new FITS file to the list.");
        addButton.addActionListener(this);

        fileButtons.add(addButton);

        // remove button (for removing selected files from jlist)
        removeButton = new JButton();
        removeButton.setIcon(removeIcon);
        removeButton.setToolTipText("Remove a FITS file from the list");
        removeButton.addActionListener(this);

        fileButtons.add(removeButton);
        loadFile.add(fileButtons);
        formular.add(loadFile);

        // LOAD VECTOR FIELD BUTTON
        loadButton = new JButton("Load Vectors");
        loadButton.setToolTipText("Load the vectorfield and visualize it.");
        loadButton.addActionListener(this);

        fileButtons.add(loadButton);

        // Lay out the panel
        SpringUtilities.makeCompactGrid(formular, controlsCount, 2, // rows,
                                                                    // cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad

        panel.add(new JSeparator(SwingConstants.HORIZONTAL));

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());

        JLabel lblTitleExample = new JLabel("Vectorfield Examples:");
        container.add(lblTitleExample, BorderLayout.NORTH);

        JPanel innerContainer = new JPanel();
        innerContainer.setLayout(new BoxLayout(innerContainer, BoxLayout.Y_AXIS));

        loadExample = new JButton("Load 51 Dimension (256x30)");
        loadExample.addActionListener(this);

        loadHugeExample = new JButton("Load 1 Dimension (4096x4096)");
        loadHugeExample.addActionListener(this);

        loadMediumExample = new JButton("Load 1 Dimension (1024x1024)");
        loadMediumExample.addActionListener(this);

        innerContainer.add(loadExample, BorderLayout.WEST);
        innerContainer.add(loadMediumExample, BorderLayout.EAST);
        innerContainer.add(loadHugeExample, BorderLayout.SOUTH);

        container.add(innerContainer);

        panel.add(container);

    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#getTitle()
     */
    public String getTitle() {
        return "Vector Field Loader";
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
        return ControlPluginType.VECTOR_LOADER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addButton) {
            // add files was clicked
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
            // remove files button was clicked
            int indices[] = fileList.getSelectedIndices();

            for (int i = indices.length - 1; i >= 0; i--) {
                fileListModel.remove(indices[i]);
            }
        } else if (e.getSource() == loadExample) {
            // load IDL Example
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            VectorFieldManager.getInstance().loadIDLExample();
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            PluginManager.getInstance().getControlPluginByType(ControlPluginType.SETTINGS).setEnabled(true);
        } else if (e.getSource() == loadHugeExample) {
            // load HUGE Example
            try {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                VectorFieldManager.getInstance().loadHugeExample();
            } catch (ObservationDateMissingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (InconsistentVectorfieldSizeException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } finally {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            PluginManager.getInstance().getControlPluginByType(ControlPluginType.SETTINGS).setEnabled(true);
        } else if (e.getSource() == loadMediumExample) {
            // load HUGE Example
            try {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                VectorFieldManager.getInstance().loadMediumExample();
            } catch (ObservationDateMissingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (InconsistentVectorfieldSizeException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } finally {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            PluginManager.getInstance().getControlPluginByType(ControlPluginType.SETTINGS).setEnabled(true);
        } else if (e.getSource() == loadButton) {
            if (fileListModel.size() > 0) {

                // load the vectorfield
                double sizeX, sizeY, posX, posY;
                // values of the JSpinner must first be cast to double
                sizeX = (Double) widthArcsec.getValue();
                sizeY = (Double) heightArcsec.getValue();
                posX = (Double) posArcsexX.getValue();
                posY = (Double) posArcsexY.getValue();

                Vector2f sizeArcsec = new Vector2f((float) sizeX, (float) sizeY);
                Vector2f posArcsec = new Vector2f((float) posX, (float) posY);

                List<String> paths = new LinkedList<String>();

                for (int i = 0; i < fileListModel.getSize(); i++) {
                    FileListItem itm = (FileListItem) fileListModel.getElementAt(i);
                    paths.add(itm.path);
                }

                VectorFieldManager vectorFieldManager = VectorFieldManager.getInstance();

                try {
                    vectorFieldManager.loadVectorField(paths, sizeArcsec, posArcsec);
                } catch (ObservationDateMissingException nodate) {
                    JOptionPane.showMessageDialog(panel, nodate.getMessage(), "Observation Date Missing", JOptionPane.ERROR_MESSAGE);
                } catch (InconsistentVectorfieldSizeException wrongsize) {
                    JOptionPane.showMessageDialog(panel, wrongsize.getMessage(), "Inconsistent Field Size", JOptionPane.ERROR_MESSAGE);
                } catch (FitsException fitsexception) {
                    JOptionPane.showMessageDialog(panel, "FITS File could not be read", fitsexception.getMessage(), JOptionPane.ERROR_MESSAGE);
                }

                PluginManager.getInstance().getControlPluginByType(ControlPluginType.SETTINGS).setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(panel, "Please select at least one file.");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#shouldStartExpanded()
     */
    public boolean shouldStartExpanded() {
        return true;
    }
}
