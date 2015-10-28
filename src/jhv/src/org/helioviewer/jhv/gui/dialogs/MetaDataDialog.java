package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.fitsview.FITSView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2View;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Dialog that is used to display meta data for an image.
 *
 * @author Alen Agheksanterian
 * @author Stephan Pagel
 */
@SuppressWarnings("serial")
public class MetaDataDialog extends JDialog implements ActionListener, ShowableDialog {

    private static class LocalTableModel extends DefaultTableModel {

        public LocalTableModel(Object[][] object, Object[] objects) {
            super(object, objects);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    private final JButton closeButton = new JButton("Close");
    private final JButton exportFitsButton = new JButton("Export FITS Header as XML");
    private final DefaultTableModel fitsModel = new LocalTableModel(null, new Object[] { "FITS Key", "value" });
    private final DefaultListModel jhList = new DefaultListModel();
    private final JList jhBox = new JList(jhList);

    private final DefaultListModel basicList = new DefaultListModel();
    private final JList basicBox = new JList(basicList);
    private Document xmlDoc = null;
    private boolean metaDataOK;
    private String outFileName;

    /**
     * The private constructor that sets the fields and the dialog.
     */
    public MetaDataDialog(View view) {
        super(ImageViewerGui.getMainFrame(), "Image Information");

        setLayout(new BorderLayout());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(exportFitsButton);
        bottomPanel.add(closeButton);
        prepareList(basicBox);
        prepareList(jhBox);

        JTable fTable = new JTable(fitsModel);
        prepareTable(fTable);

        JPanel sp = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;

        c.gridx = 0;
        c.gridy = 0;

        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        sp.add((basicBox), c);
        c.weighty = 3;
        c.gridy = 1;
        sp.add(new JScrollPane(fTable), c);
        c.weighty = 1.25;
        c.gridy = 2;
        sp.add(new JScrollPane(jhBox), c);

        add(sp, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.PAGE_END);

        closeButton.addActionListener(this);
        exportFitsButton.addActionListener(this);

        setMetaData(view);

        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closePressed();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void prepareList(JList l) {
        l.setFont(UIGlobals.UIFontMono);

        l.setCellRenderer(new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JTextArea textArea = new JTextArea(value.toString().trim());
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(list.getFont());
                return textArea;
            }
        });
    }

    public void prepareTable(JTable t) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(t.getModel());
        t.setRowSorter(sorter);
        //JTextField tf = new JTextField();
        //t.setRowSelectionAllowed(true);
        //t.setColumnSelectionAllowed(true);
        //t.setCellSelectionEnabled(true);
    }

    /**
     * Resets the list.
     */
    public void resetData() {
        // update the listBox
        // set the status of export button
        if (!metaDataOK) {
            exportFitsButton.setEnabled(false);
        } else {
            exportFitsButton.setEnabled(true);
        }
    }

    public void addDataItem(String key, String value) {
        basicList.add(basicList.getSize(), key + ": " + value + "\n");
    }

    public void addDataItem(String key, DefaultListModel model) {
        model.add(model.getSize(), key + "\n");
    }

    private void addDataItem(String nodeName, String nodeValue, boolean isFits) {
        if (isFits)
            fitsModel.addRow(new Object[] { nodeName, nodeValue });
        else
            jhList.add(jhList.getSize(), nodeName + ": " + nodeValue + "\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
        if (!metaDataOK)
            setSize(450, 200);
        else
            setSize(450, 600);

        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        getRootPane().setDefaultButton(closeButton);

        pack();
        setVisible(true);
    }

    private void closePressed() {
        xmlDoc = null;
        resetData();
        dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent _a) {
        if (_a.getSource() == closeButton) {
            closePressed();
        } else if (_a.getSource() == exportFitsButton) {
            DOMSource source = new DOMSource(xmlDoc.getDocumentElement().getElementsByTagName("fits").item(0));

            boolean saveSuccessful = saveXMLDocument(source, outFileName);
            if (saveSuccessful)
                JOptionPane.showMessageDialog(this, "Fits data saved to " + outFileName);
        }
    }

    private void setMetaData(View v) {
        MetaData metaData = v.getImageLayer().getImageData().getMetaData();
        if (!(metaData instanceof HelioviewerMetaData)) {
            metaDataOK = false;
            resetData();
            addDataItem("error", "No metadata is available.");
        } else {
            HelioviewerMetaData m = (HelioviewerMetaData) metaData;
            metaDataOK = true;
            resetData();
            addDataItem("-------------------------------", basicList);
            addDataItem("       Basic Information       ", basicList);
            addDataItem("-------------------------------", basicList);
            addDataItem("Observatory", m.getObservatory());
            addDataItem("Instrument", m.getInstrument());
            addDataItem("Detector", m.getDetector());
            addDataItem("Measurement", m.getMeasurement());
            addDataItem("Observation Date", m.getDateObs().toString());

            String xmlText = null;
            if (v instanceof JP2View) {
                xmlText = ((JP2View) v).getXMLMetaData();
            } else if (v instanceof FITSView) {
                xmlText = ((FITSView) v).getHeaderAsXML();
            }

            if (xmlText != null) {
                try {
                    InputStream in = new ByteArrayInputStream(xmlText.trim().replace("&", "&amp;").getBytes("UTF-8"));
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = builder.parse(in);

                    // Send xml data to meta data dialog box
                    Node root = doc.getDocumentElement().getElementsByTagName("fits").item(0);
                    writeXMLData(root, 0);
                    root = doc.getDocumentElement().getElementsByTagName("helioviewer").item(0);
                    if (root != null) {
                        writeXMLData(root, 0);
                    }

                    // set the xml data for the MetaDataDialog
                    xmlDoc = doc;
                    // export file name
                    outFileName = JHVDirectory.EXPORTS.getPath() + m.getFullName() + "__" + TimeUtils.filenameDateFormat.format(m.getDateObs().getDate()) + ".fits.xml";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * A method that writes the xml box specified by its root node to the list
     * box in image info dialog box.
     *
     * @param node
     *            Node to write
     * @param indent
     *            Number of tabstops to insert
     */
    private String lastNodeSeen = null;

    private void writeXMLData(Node node, int indent) {
        // get element name and value
        String nodeName = node.getNodeName();
        String nodeValue = getElementValue(node);

        if (nodeName.equals("fits")) {
            lastNodeSeen = nodeName;
        } else if (nodeName.equals("helioviewer")) {
            lastNodeSeen = nodeName;
            addDataItem("-------------------------------", jhList);
            addDataItem("      Helioviewer Header", jhList);
            addDataItem("-------------------------------", jhList);
        } else {
            String tab = "";
            for (int i = 0; i < indent; i++) {
                tab = tab + "\t";
            }
            addDataItem(nodeName, nodeValue, lastNodeSeen.equals("fits"));
        }

        // write the child nodes recursively
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                writeXMLData(child, indent + 1);
            }
        }
    }

    /**
     * A method that gets the value of a node element.
     *
     * If the node itself has children and no text value, an empty string is
     * returned. This is maybe an overkill for our purposes now, but takes into
     * account the possibility of nested tags.
     *
     * @param elem
     *            Node to read
     * @return value of the node
     */
    private final String getElementValue(Node elem) {
        Node child;
        if (elem != null) {
            if (elem.hasChildNodes()) {
                for (child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        return child.getNodeValue();
                    }
                }
            }
        }
        return "";
    }

    /**
     * This routine saves the fits data into an XML file.
     *
     * @param source
     *            XML document to save
     * @param filename
     *            XML file name
     */
    private boolean saveXMLDocument(DOMSource source, String filename) {
        // open the output stream where XML Document will be saved
        File xmlOutFile = new File(filename);
        FileOutputStream fos;
        Transformer transformer;
        try {
            fos = new FileOutputStream(xmlOutFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        // Use a Transformer for the purpose of output
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            try {
                fos.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return false;
        }

        // The source is the fits header

        // The destination for output
        StreamResult result = new StreamResult(fos);

        // transform source into result will do a file save
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        try {
            fos.close();
        } catch (IOException e) {
            Log.error("Fail at closing file." + e);
        }

        return true;
    }

    @Override
    public void init() {
    }

}
