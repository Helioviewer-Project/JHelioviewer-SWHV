package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.parsers.DocumentBuilderFactory;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.fitsview.FITSView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2View;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.SearchableUtils;

@SuppressWarnings("serial")
public class MetaDataDialog extends StandardDialog implements ShowableDialog {

    private final JideSplitPane content = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
    private final JButton exportFitsButton = new JButton("Export FITS Header as XML");

    private final DefaultTableModel fitsModel = new DefaultTableModel(new Object[0][0], new Object[] { "FITS Keyword", "Value" }) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final StringBuilder basicSB = new StringBuilder();
    private final StringBuilder hvSB = new StringBuilder();

    public MetaDataDialog(View view) {
        super(ImageViewerGui.getMainFrame(), "Image Information");

        exportFitsButton.setEnabled(false);
        setMetaData(view);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(fitsModel);
        JTable fitsTable = new JTable(fitsModel);
        fitsTable.setRowSorter(sorter);
        setInitFocusedComponent(fitsTable);
        SearchableUtils.installSearchable(fitsTable);

        JTextArea basicArea = new JTextArea(basicSB.toString().trim());
        basicArea.setEditable(false);

        JTextArea hvArea = new JTextArea(hvSB.toString().trim());
        hvArea.setEditable(false);
        hvArea.setLineWrap(true);
        hvArea.setWrapStyleWord(true);

        content.add(new JScrollPane(basicArea));
        content.add(new JScrollPane(fitsTable));
        content.add(new JScrollPane(hvArea));
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultAction(close);
        setDefaultCancelAction(close);

        JButton button = new JButton(close);
        button.setText("Close");

        ButtonPanel panel = new ButtonPanel();
        panel.add(button, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(exportFitsButton, ButtonPanel.OTHER_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        return content;
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        content.setDividerLocation(1, 600);
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    private void setMetaData(View v) {
        MetaData metaData = v.getImageLayer().getMetaData();
        if (!(metaData instanceof HelioviewerMetaData)) {
            basicSB.append("Error: No metadata is available");
            return;
        }

        HelioviewerMetaData m = (HelioviewerMetaData) metaData;
        basicSB.append("Observatory: ").append(m.getObservatory()).append('\n');
        basicSB.append("Instrument: ").append(m.getInstrument()).append('\n');
        basicSB.append("Detector: ").append(m.getDetector()).append('\n');
        basicSB.append("Measurement: ").append(m.getMeasurement()).append('\n');
        basicSB.append("Observation Date: ").append(m.getViewpoint().time).append('\n');

        try {
            String xmlText;
            if (v instanceof JP2View)
                xmlText = ((JP2View) v).getXMLMetaData();
            else if (v instanceof FITSView)
                xmlText = ((FITSView) v).getHeaderAsXML();
            else
                return;

            InputStream in = new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8));
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);

            // Send xml data to meta data dialog box
            Node root = doc.getDocumentElement().getElementsByTagName("fits").item(0);
            writeXMLData(root);
            root = doc.getDocumentElement().getElementsByTagName("helioviewer").item(0);
            if (root != null) {
                writeXMLData(root);
            }

            final String xml = xmlText;
            String outFileName = JHVDirectory.EXPORTS.getPath() + m.getFullName().replace(' ', '_') + "__" + TimeUtils.filenameDateFormat.format(m.getViewpoint().time.milli) + ".fits.xml";
            exportFitsButton.setEnabled(true);
            exportFitsButton.addActionListener(e -> {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileName), StandardCharsets.UTF_8))) {
                    writer.write(xml, 0, xml.length());
                } catch (Exception ex) {
                    Log.error("Failed to write XML: " + ex);
                    return; // try with resources
                }
                JHVGlobals.displayNotification(outFileName);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDataItem(String nodeName, String nodeValue, boolean isFits) {
        if (isFits)
            fitsModel.addRow(new Object[] { nodeName, nodeValue });
        else
            hvSB.append(nodeName).append(": ").append(nodeValue).append('\n');
    }

    /**
     * A method that writes the xml box specified by its root node to the list
     * box in image info dialog box.
     *
     * @param node
     *            Node to write
     */
    private String lastNodeSeen = null;

    private void writeXMLData(Node node) {
        // get element name and value
        String nodeName = node.getNodeName();
        String nodeValue = getElementValue(node);

        switch (nodeName) {
            case "fits":
            case "helioviewer":
                lastNodeSeen = nodeName;
                break;
            default:
                addDataItem(nodeName, nodeValue, "fits".equals(lastNodeSeen));
                break;
        }

        // write the child nodes recursively
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                writeXMLData(child);
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
    private static String getElementValue(Node elem) {
        if (elem != null && elem.hasChildNodes()) {
            for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    return child.getNodeValue();
                }
            }
        }
        return "";
    }

}
