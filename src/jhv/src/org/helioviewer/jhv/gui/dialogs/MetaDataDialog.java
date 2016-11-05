package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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

// Dialog that is used to display meta data for an image.
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
    private final DefaultListModel<String> jhList = new DefaultListModel<String>();
    private final DefaultListModel<String> basicList = new DefaultListModel<String>();

    private Document xmlDoc = null;
    private boolean metaDataOK;
    private String outFileName;

    public MetaDataDialog(View view) {
        super(ImageViewerGui.getMainFrame(), "Image Information");

        setLayout(new BorderLayout());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(exportFitsButton);
        bottomPanel.add(closeButton);

        JList<String> basicBox = new JList<String>(basicList);
        basicBox.setCellRenderer(new WrappedTextCellRenderer());
        JList<String> jhBox = new JList<String>(jhList);
        jhBox.setCellRenderer(new WrappedTextCellRenderer());

        ComponentListener cl = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // force cache invalidation by temporarily setting fixed height
                ((JList) e.getComponent()).setFixedCellHeight(10);
                ((JList) e.getComponent()).setFixedCellHeight(-1);
            }
        };
        jhBox.addComponentListener(cl);

        JTable fTable = new JTable(fitsModel);
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(fitsModel);
        fTable.setRowSorter(sorter);

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

        getRootPane().registerKeyboardAction(e -> closePressed(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(closeButton);
        getRootPane().setFocusable(true);
    }

    private static class WrappedTextCellRenderer extends JTextArea implements ListCellRenderer<Object> {

        public WrappedTextCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(UIGlobals.UIFontMono);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                setText(value.toString().trim());
                int width = list.getWidth();
                if (width > 0)
                    setSize(width, Short.MAX_VALUE);
            }
            return this;
        }

    }

    private void resetData() {
        if (!metaDataOK) {
            exportFitsButton.setEnabled(false);
        } else {
            exportFitsButton.setEnabled(true);
        }
    }

    private void addDataItem(String key, DefaultListModel<String> model) {
        model.addElement(key);
    }

    private void addDataItem(String nodeName, String nodeValue, boolean isFits) {
        if (isFits)
            fitsModel.addRow(new Object[] { nodeName, nodeValue });
        else
            jhList.addElement(nodeName + ": " + nodeValue);
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    private void closePressed() {
        xmlDoc = null;
        resetData();
        dispose();
    }

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
        MetaData metaData = v.getImageLayer().getMetaData();
        if (!(metaData instanceof HelioviewerMetaData)) {
            metaDataOK = false;
            resetData();
            addDataItem("Error: No metadata is available.", basicList);
        } else {
            HelioviewerMetaData m = (HelioviewerMetaData) metaData;
            metaDataOK = true;
            resetData();
            addDataItem("-------------------------------", basicList);
            addDataItem("       Basic Information       ", basicList);
            addDataItem("-------------------------------", basicList);
            addDataItem("Observatory: " + m.getObservatory(), basicList);
            addDataItem("Instrument: " + m.getInstrument(), basicList);
            addDataItem("Detector: " + m.getDetector(), basicList);
            addDataItem("Measurement: " + m.getMeasurement(), basicList);
            addDataItem("Observation Date: " + m.getViewpoint().time, basicList);

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
                    outFileName = JHVDirectory.EXPORTS.getPath() + m.getFullName() + "__" + TimeUtils.filenameDateFormat.format(m.getViewpoint().time.milli) + ".fits.xml";
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

        switch (nodeName) {
            case "fits":
                lastNodeSeen = nodeName;
                break;
            case "helioviewer":
                lastNodeSeen = nodeName;
                addDataItem("-------------------------------", jhList);
                addDataItem("      Helioviewer Header", jhList);
                addDataItem("-------------------------------", jhList);
                break;
            default:
                addDataItem(nodeName, nodeValue, lastNodeSeen.equals("fits"));
                break;
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
    private String getElementValue(Node elem) {
        if (elem != null && elem.hasChildNodes()) {
            for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    return child.getNodeValue();
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
        try (FileOutputStream fos = new FileOutputStream(new File(filename))) {
            StreamResult result = new StreamResult(fos);
            TransformerFactory.newInstance().newTransformer().transform(source, result);
            return true;
        } catch (Exception e) {
            Log.error("Failed to write XML: " + e);
        }
        return false;
    }

    @Override
    public void init() {
    }

}
