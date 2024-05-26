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
import org.helioviewer.jhv.gui.Interfaces;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public final class SWEKSupplier extends DefaultMutableTreeNode implements Interfaces.JHVCell {

    private final String supplierName;
    private final String name;
    private final String db;
    private final String key;

    private final SWEK.Source source;

    private final JPanel panel;
    private final JCheckBox checkBox;
    private final boolean isCactus;

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    public SWEKSupplier(String _supplierName, String _name, SWEK.Source _source, String _db, SWEKGroup group) {
        supplierName = _supplierName;
        name = _name.intern();
        source = _source;
        db = _db;

        isCactus = name == "CACTus" && "HEK".equals(source.name());

        key = supplierName + source.name() + db;
        suppliers.put(key, this);

        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        checkBox = new JCheckBox(name);
        checkBox.addActionListener(e -> internalActivate(checkBox.isSelected()));
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
        panel.add(checkBox, BorderLayout.LINE_START);

        if (group.containsFilter()) {
            FilterDialog filterDialog = new FilterDialog(group, this);
            JideButton filterButton = getFilterButton(filterDialog);
            panel.setPreferredSize(new Dimension(SWEKGroup.RIGHT_ALIGNMENT, filterButton.getPreferredSize().height)); //!
            panel.add(filterButton, BorderLayout.LINE_END);
        }
    }

    private static JideButton getFilterButton(FilterDialog filterDialog) {
        JideButton filterButton = new JideButton("Filter");
        filterButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point pressedLocation = e.getLocationOnScreen();
                Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
                filterDialog.setLocation(windowLocation);
                filterDialog.setVisible(true);
            }
        });
        return filterButton;
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
        return (SWEKGroup) getParent();
    }

    public SWEK.Source getSource() {
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

    private void internalActivate(boolean b) {
        SWEKDownloader.activateSupplier(this, b);
    }

    public boolean isSelected() {
        return checkBox.isSelected();
    }

    @Override
    public Component getComponent() {
        return panel;
    }

}
