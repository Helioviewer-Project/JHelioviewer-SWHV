package org.helioviewer.jhv.gui.dialogs;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.XMLUtils;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.HTMLPane;
import org.helioviewer.jhv.gui.components.base.WrappedTable;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.time.TimeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.swing.JideSplitPane;

@SuppressWarnings("serial")
public class MetaDataDialog extends StandardDialog implements ShowableDialog {

    private final JideSplitPane content = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
    private final JButton exportFitsButton = new JButton("Export FITS Header as XML");

    private final FitsModel fitsModel = new FitsModel();
    private final HTMLPane basicArea = new HTMLPane();
    private final HTMLPane hvArea = new HTMLPane();

    public MetaDataDialog() {
        super(JHVFrame.getFrame(), "Image Information");

        WrappedTable fitsTable = new WrappedTable();
        fitsTable.setModel(fitsModel);
        fitsTable.setRowSorter(new TableRowSorter<>(fitsModel));
        fitsTable.setIntercellSpacing(new Dimension(0, 0));
        int keywordWidth = new JLabel("MMMMMMMM").getPreferredSize().width;
        fitsTable.getColumnModel().getColumn(0).setMinWidth(keywordWidth);
        fitsTable.getColumnModel().getColumn(0).setMaxWidth(keywordWidth);
        fitsTable.getColumnModel().getColumn(1).setCellRenderer(new WrappedTable.WrappedTextRenderer());
        fitsTable.getColumnModel().getColumn(2).setCellRenderer(new WrappedTable.WrappedTextRenderer());

        setInitFocusedComponent(fitsTable);
        com.jidesoft.swing.SearchableUtils.installSearchable(fitsTable);

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

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    public void setMetaData(ImageLayer layer) {
        fitsModel.setRowCount(0);
        hvArea.setText("");
        hvArea.setPreferredSize(new Dimension(400, 100));
        lastNodeSeen = null;
        exportFitsButton.setEnabled(false);

        if (!(layer.getMetaData() instanceof HelioviewerMetaData m)) {
            basicArea.setText("No Helioviewer metadata available");
            return;
        }

        basicArea.setText("Observatory: " + m.getObservatory() + "<br/>" +
                "Instrument: " + m.getInstrument() + "<br/>" +
                "Detector: " + m.getDetector() + "<br/>" +
                "Measurement: " + m.getMeasurement() + "<br/>" +
                "Observation Date: " + m.getViewpoint().time);

        try {
            String xml = layer.getView().getXMLMetaData();
            Document doc = XMLUtils.parse(xml);

            // Send xml data to meta data dialog box
            StringBuilder hvSB = new StringBuilder();
            Node root = doc.getDocumentElement().getElementsByTagName("fits").item(0);
            if (root != null)
                readXMLData(hvSB, root);
            root = doc.getDocumentElement().getElementsByTagName("helioviewer").item(0);
            if (root != null)
                readXMLData(hvSB, root);
            hvArea.setText(hvSB.toString().trim());

            Path path = Path.of(JHVDirectory.EXPORTS.getPath(),
                    m.getDisplayName().replace(' ', '_') + "__" + TimeUtils.formatFilename(m.getViewpoint().time.milli) + ".fits.xml");
            exportFitsButton.setEnabled(true);
            exportFitsButton.addActionListener(e -> new Thread(() -> {
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write(xml, 0, xml.length());
                    EventQueue.invokeLater(() -> JHVGlobals.displayNotification(path.toString()));
                } catch (Exception ex) {
                    Log.error("Failed to write metadata", ex);
                }
            }).start());
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private static class FitsModel extends DefaultTableModel {

        FitsModel() {
            super(new String[0][0][0], new String[]{"Keyword", "Value", "Comment"});
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

    }

    private String lastNodeSeen;

    private void readXMLData(StringBuilder hvSB, Node node) {
        // get element name and value
        String nodeName = node.getNodeName();
        String nodeValue = getElementValue(node);

        Node attrNode;
        NamedNodeMap attributes = node.getAttributes();
        String nodeComment = attributes != null && (attrNode = attributes.getNamedItem("comment")) != null ? attrNode.getNodeValue().trim() : "&nbsp;";

        switch (nodeName) {
            case "fits":
            case "helioviewer":
                lastNodeSeen = nodeName;
                break;
            default:
                if ("fits".equals(lastNodeSeen))
                    fitsModel.addRow(new String[]{nodeName, nodeValue, nodeComment});
                else
                    hvSB.append(nodeName).append(": ").append(nodeValue).append("<br/>");
                break;
        }

        // write the child nodes recursively
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                readXMLData(hvSB, child);
            }
        }
    }

    /**
     * A method that gets the value of a node element.
     * <p>
     * If the node itself has children and no text value, an empty string is
     * returned. This is maybe an overkill for our purposes now, but takes into
     * account the possibility of nested tags.
     *
     * @param elem Node to read
     * @return value of the node
     */
    private static String getElementValue(Node elem) {
        if (elem != null && elem.hasChildNodes()) {
            for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    String value = child.getNodeValue().trim();
                    return value.isEmpty() ? "&nbsp;" : value; //! avoid row crush by WrappedTable
                }
            }
        }
        return "&nbsp;";
    }

}
