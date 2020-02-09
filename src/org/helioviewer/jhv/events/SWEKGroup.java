package org.helioviewer.jhv.events;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.interfaces.JHVTreeNode;

@SuppressWarnings("serial")
public class SWEKGroup extends DefaultMutableTreeNode implements JHVTreeNode {

    private static List<SWEKRelatedEvents> swekrelEvents;

    private final String name;
    private final List<SWEKParameter> parameterList;

    private final ImageIcon icon;
    private final JPanel panel;
    private final JLabel loadingLabel = new JLabel("    ");
    private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);
    // The timer handling the loading animation
    private final Timer loadingTimer = new Timer(500, e -> over.repaint());

    private final boolean containsParameterFilter;

    private List<SWEKSupplier> suppliers = new ArrayList<>();
    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEKParameter> _parameterList, ImageIcon _icon) {
        name = _name.intern();
        parameterList = _parameterList;
        icon = _icon;
        containsParameterFilter = checkFilters(parameterList);

        JLabel label = new JLabel(name);
        ComponentUtils.smallVariant(label);
        int size = label.getPreferredSize().height;
        label.setIcon(new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH)));

        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.LINE_START);
        panel.setPreferredSize(new Dimension(250, size)); //!
        panel.add(over, BorderLayout.LINE_END);
        ComponentUtils.smallVariant(panel);
    }

    public HashMap<String, String> getAllDatabaseFields() {
        if (databaseFields == null) {
            createAllDatabaseFields();
        }
        return databaseFields;
    }

    private void createAllDatabaseFields() {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEKParameter p : parameterList) {
            SWEKParameterFilter pf = p.getParameterFilter();
            if (pf != null) {
                fields.put(p.getParameterName().intern(), pf.getDbType());
            }
        }
        for (SWEKRelatedEvents re : swekrelEvents) {
            if (re.getGroup() == this) {
                re.getRelatedOnList().forEach(swon -> fields.put(swon.parameterFrom.getParameterName().intern(), swon.dbType));
            }
            if (re.getRelatedWith() == this) {
                re.getRelatedOnList().forEach(swon -> fields.put(swon.parameterWith.getParameterName().intern(), swon.dbType));
            }
        }
        databaseFields = fields;
    }

    public static void setSwekRelatedEvents(List<SWEKRelatedEvents> _relatedEvents) {
        swekrelEvents = _relatedEvents;
    }

    public static List<SWEKRelatedEvents> getSWEKRelatedEvents() {
        return swekrelEvents;
    }

    public List<SWEKSupplier> getSuppliers() {
        return suppliers;
    }

    void add(SWEKSupplier supplier) {
        super.add(supplier);
        suppliers.add(supplier);
    }

    public String getName() {
        return name;
    }

    public List<SWEKParameter> getParameterList() {
        return parameterList;
    }

    @Nullable
    public SWEKParameter getParameter(String _name) {
        for (SWEKParameter parameter : parameterList) {
            if (parameter.getParameterName().equalsIgnoreCase(_name)) {
                return parameter;
            }
        }
        return null;
    }

    boolean containsFilter() {
        return containsParameterFilter;
    }

    private static boolean checkFilters(List<SWEKParameter> parameters) {
        for (SWEKParameter parameter : parameters) {
            if (parameter.getParameterFilter() != null) {
                return true;
            }
        }
        return false;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    @Override
    public Component getComponent() {
        return panel;
    }

    private JTree tree;

    void setTree(JTree _tree) {
        tree = _tree;
    }

    void startedDownload() {
        if (!loadingTimer.isRunning()) {
            over.setView(loadingLabel);
            loadingTimer.start();
            tree.treeDidChange(); // notify to repaint
        }
    }

    void stoppedDownload() {
        if (loadingTimer.isRunning()) {
            loadingTimer.stop();
            over.setView(null);
            tree.treeDidChange(); // notify to repaint
        }
    }

}
