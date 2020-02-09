package org.helioviewer.jhv.events;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.events.filter.FilterDialog;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.JHVButton;
import org.helioviewer.jhv.gui.interfaces.JHVTreeNode;

@SuppressWarnings("serial")
public class SWEKSupplier extends DefaultMutableTreeNode implements JHVTreeNode {

    private final String supplierName;
    private final String name;
    private final String db;
    private final String key;

    private final SWEKGroup group;
    private final SWEKSource source;

    private final JPanel panel;
    private final JCheckBox checkBox;
    private final boolean isCactus;

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    public SWEKSupplier(String _supplierName, String _name, SWEKGroup _group, SWEKSource _source, String _db) {
        supplierName = _supplierName;
        name = _name.intern();

        group = _group;
        source = _source;
        db = _db;

        isCactus = name == "CACTus" && source.getName() == "HEK"; // interned

        key = supplierName + source.getName() + db;
        suppliers.put(key, this);

        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        checkBox = new JCheckBox(name);
        checkBox.addActionListener(e -> internalActivate(checkBox.isSelected()));
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
        panel.add(checkBox, BorderLayout.LINE_START);

        if (group.containsFilter()) {
            FilterDialog filterDialog = new FilterDialog(this);
            JHVButton filterButton = new JHVButton("Filter");
            filterButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point pressedLocation = e.getLocationOnScreen();
                    Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
                    filterDialog.setLocation(windowLocation);
                    filterDialog.setVisible(true);
                }
            });
            panel.setPreferredSize(new Dimension(250, filterButton.getPreferredSize().height)); //!
            panel.add(filterButton, BorderLayout.LINE_END);
        }
        ComponentUtils.smallVariant(panel);
    }

    public static SWEKSupplier getSupplier(String name) {
        return suppliers.get(name);
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getDatabaseName() {
        return db;
    }

    public String getName() {
        return name;
    }

    public SWEKGroup getGroup() {
        return group;
    }

    public SWEKSource getSource() {
        return source;
    }

    public String getKey() {
        return key;
    }

    public boolean isCactus() {
        return isCactus;
    }

    public void activate(boolean b) {
        checkBox.setSelected(b);
    }

    public void internalActivate(boolean b) {
        SWEKDownloadManager.activateSupplier(this, b);
    }

    public boolean isSelected() {
        return checkBox.isSelected();
    }

    @Override
    public Component getComponent() {
        return panel;
    }

}
