package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.fitsview.JHVFITSView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Dialog that is used to display meta data for an image.
 * 
 * @author Alen Agheksanterian
 * @author Stephan Pagel
 */
public class MetaDataDialog extends JDialog implements ActionListener, ShowableDialog {

    private static final long serialVersionUID = 1L;

    private final JButton closeButton = new JButton("Close");
    private final JButton exportFitsButton = new JButton("Export FITS Header as XML");

    private Vector<String> infoList = new Vector<String>();
    private JList listBox = new JList();
    private Document xmlDoc = null;
    private boolean metaDataOK;
    private String outFileName;

    /**
     * The private constructor that sets the fields and the dialog.
     */
    public MetaDataDialog() {
        super(ImageViewerGui.getMainFrame(), "Image Information");
        setAlwaysOnTop(true);
        setLayout(new BorderLayout());
        setResizable(false);

        listBox.setFont(new Font("Courier", Font.PLAIN, 12));

        listBox.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JTextArea textArea = new JTextArea(value.toString().trim());
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(list.getFont());
                return textArea;
            }
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(exportFitsButton);
        // bottomPanel.add(exportButton);
        bottomPanel.add(closeButton);

        JScrollPane listScroller = new JScrollPane(listBox);
        add(listScroller, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.PAGE_END);

        // add action listeners to the buttons
        closeButton.addActionListener(this);
        exportFitsButton.addActionListener(this);
        // exportButton.addActionListener(this);
    }

    /**
     * Resets the list.
     */
    public void resetData() {
        infoList.removeAllElements();

        // update the listBox
        listBox.setListData(this.infoList);

        // set the status of export button
        if (!metaDataOK) {
            exportFitsButton.setEnabled(false);
        } else {
            exportFitsButton.setEnabled(true);
        }
    }

    /**
     * Adds a data item to the list
     * 
     * @param _item
     *            New item to add
     * @see #setMetaData(MetaDataView)
     */
    public void addDataItem(String _item) {
        infoList.add(_item);

        // update the listBox
        listBox.setListData(this.infoList);
    }

    /**
     * {@inheritDoc}
     */
    public void showDialog() {
        pack();
        if (!metaDataOK)
            setSize(450, 200);
        else
            setSize(450, 600);

        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent _a) {
        if (_a.getSource() == closeButton) {
            xmlDoc = null;
            infoList.removeAllElements();
            resetData();
            dispose();

        } else if (_a.getSource() == exportFitsButton) {
            DOMSource source = new DOMSource(xmlDoc.getDocumentElement().getElementsByTagName("fits").item(0));

            boolean saveSuccessful = saveXMLDocument(source, outFileName);
            if (saveSuccessful)
                JOptionPane.showMessageDialog(this, "Fits data saved to " + outFileName);
        }
    }

    /**
     * Sets the full document which can be found reading the given MetaDataView.
     * 
     * @param metaDataView
     *            Source to read
     * @see #addDataItem(String)
     */
    public void setMetaData(MetaDataView metaDataView) {

        if (metaDataView == null)
            return;

        MetaData metaData = metaDataView.getMetaData();

        if (!(metaData instanceof HelioviewerMetaData)) {
            metaDataOK = false;
            resetData();
            addDataItem("No metadata is available.");
        } else {
            HelioviewerMetaData m = (HelioviewerMetaData) metaData;
            metaDataOK = true;
            resetData();
            addDataItem("-------------------------------");
            addDataItem("       Basic Information       ");
            addDataItem("-------------------------------");
            addDataItem("Observatory : " + m.getObservatory());
            addDataItem("Instrument  : " + m.getInstrument());
            addDataItem("Detector    : " + m.getDetector());
            addDataItem("Measurement : " + m.getMeasurement());
            addDataItem("Date        : " + m.getDateTime().getFormattedDate());
            addDataItem("Time        : " + m.getDateTime().getFormattedTime());

            String xmlText = null;
            if (metaDataView instanceof JHVJP2View) {
                JP2Image img = ((JHVJP2View) metaDataView).getJP2Image();

                int boxNumber = 1;
                if (metaDataView instanceof JHVJPXView) {
                    if (((JHVJPXView) metaDataView).getMaximumAccessibleFrameNumber() < 0) {
                        return;
                    }
                    boxNumber = ((JHVJPXView) metaDataView).getCurrentFrameNumber() + 1;
                }

                try {
                    xmlText = KakaduUtils.getXml(img.getFamilySrc(), boxNumber);
                } catch (JHV_KduException e) {
                    e.printStackTrace();
                }
            } else if (metaDataView instanceof JHVFITSView) {
                xmlText = ((JHVFITSView) metaDataView).getHeaderAsXML();
            }

            if (xmlText != null) {
                String xml = xmlText.trim().replace("&", "&amp;").replace("$OBS", "");

                InputStream in = null;
                try {
                    in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
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

                    // set the export file name for
                    // MetaDataDialog
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                    outFileName = JHVDirectory.EXPORTS.getPath() + m.getFullName() + " " + dateFormat.format(m.getDateTime().getTime()) + ".fits.xml";

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
    private void writeXMLData(Node node, int indent) {
        // get element name and value
        String nodeName = node.getNodeName();
        String nodeValue = getElementValue(node);

        if (nodeName.equals("fits")) {
            addDataItem("-------------------------------");
            addDataItem("          FITS Header");
            addDataItem("-------------------------------");
        } else if (nodeName.equals("helioviewer")) {
            addDataItem("-------------------------------");
            addDataItem("      Helioviewer Header");
            addDataItem("-------------------------------");
        } else {

            String tab = "";
            for (int i = 0; i < indent; i++) {
                tab = tab + "\t";
            }

            addDataItem(tab + nodeName + ": " + nodeValue);
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
        return true;
    }
}
