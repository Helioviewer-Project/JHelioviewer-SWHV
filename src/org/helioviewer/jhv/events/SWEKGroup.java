package org.helioviewer.jhv.events;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.interfaces.JHVCell;

@SuppressWarnings("serial")
public class SWEKGroup extends DefaultMutableTreeNode implements JHVCell {

    static final int RIGHT_ALIGNMENT = 300;

    private static List<SWEKRelatedEvents> swekrelEvents;

    private final String name;
    private final List<SWEKParameter> parameterList;
    private final ImageIcon icon;
    private final DefaultTreeModel treeModel;

    private final boolean containsParameterFilter;

    private final JPanel panel;
    private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);
    private final JLabel loadingLabel = new JLabel("    ");
    private final Timer loadingTimer; // handles the loading animation

    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEKParameter> _parameterList, ImageIcon _icon, DefaultTreeModel _treeModel) {
        name = _name.intern();
        parameterList = _parameterList;
        icon = _icon;
        treeModel = _treeModel;

        loadingTimer = new Timer(500, e -> {
            over.repaint();
            treeModel.nodeChanged(this); // notify to repaint
        });
        containsParameterFilter = checkFilters(parameterList);

        JLabel label = new JLabel(name);
        int size = label.getPreferredSize().height;
        label.setIcon(new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH)));

        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.LINE_START);
        panel.setPreferredSize(new Dimension(RIGHT_ALIGNMENT, size)); //!
        panel.add(over, BorderLayout.LINE_END);
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

    void startedDownload() {
        if (!loadingTimer.isRunning()) {
            over.setView(loadingLabel);
            loadingTimer.start();
            treeModel.nodeChanged(this); // notify to repaint
        }
    }

    void stoppedDownload() {
        if (loadingTimer.isRunning()) {
            loadingTimer.stop();
            over.setView(null);
            treeModel.nodeChanged(this); // notify to repaint
        }
    }

}
