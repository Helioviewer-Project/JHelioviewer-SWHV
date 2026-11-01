package org.helioviewer.jhv.layers.fov;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.io.Directories;

// Real model changes and editable renderer cells, without a host or a window.
public final class FOVOptionsTest {

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        SwingUtilities.invokeAndWait(() -> {
            try {
                com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme.setup();
                com.jidesoft.plaf.LookAndFeelFactory.installJideExtension();
                UIManager.put("defaultFont", UIManager.getFont(Platform.isLinux() ? "small.font" : "medium.font"));
                fov();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("FOVOptionsTest passed");
        System.exit(0);
    }

    private static void fov() throws Exception {
        AtomicInteger displays = new AtomicInteger();
        DisplayController.setRenderRequestHandler(_ -> displays.incrementAndGet());
        FOVCatalog catalog = new FOVCatalog(null);
        FOVOptions screen = new FOVOptions(catalog);
        FOVTreePane pane = new FOVTreePane(catalog);
        pane.setSize(600, 400);
        layout(pane);
        JTree tree = find(pane, JTree.class).getFirst();
        check(tree.isEditable() && tree.getSelectionCount() == 0 && tree.getRowCount() == 3, "Initial tree contract");
        if (Platform.isMacOS())
            check(pane.getPreferredSize().equals(new Dimension(311, 78)), "Collapsed production preferred size");
        for (int row = 0; row < tree.getRowCount(); row++)
            tree.expandRow(row);
        check(tree.getRowCount() == 17, "Platform/instrument count");
        int[] widths = {291, 84, 82, 71, 90, 89, 76, 68, 291, 68, 73, 74, 291, 97, 77, 69, 88};
        int y = 1;
        for (int row = 0; row < 17; row++) {
            boolean platform = row == 0 || row == 8 || row == 12;
            Rectangle expected = new Rectangle(platform ? 19 : 37, y, widths[row], platform ? 25 : 19);
            if (Platform.isMacOS())
                check(tree.getRowBounds(row).equals(expected), "Renderer bounds row " + row + ": " + tree.getRowBounds(row));
            Component cell = cell(tree, row);
            cell.setBounds(tree.getRowBounds(row));
            layout(cell);
            if (platform) {
                List<JSpinner> spinners = find(cell, JSpinner.class);
                check(spinners.size() == 2, "Pointing controls missing");
                JSpinner spinner = spinners.getFirst();
                if (Platform.isMacOS())
                    check(spinner.getPreferredSize().equals(new Dimension(68, 25)), "Pointing editor width");
                JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                check("0.00′".equals(field.getText()), "Arcminute formatting");
                check(((Number) spinner.getNextValue()).doubleValue() == 0.1, "Pointing step");
            }
            y += expected.height;
        }
        Component first = cell(tree, 0);
        JSpinner x = find(first, JSpinner.class).getFirst();
        JFormattedTextField field = ((JSpinner.DefaultEditor) x.getEditor()).getTextField();
        int before = displays.get();
        field.setText("7.5′");
        x.commitEdit();
        check(catalog.platforms().getFirst().getCenterX() == 7.5, "Fractional pointing changed");
        before = displays.get();
        field.setText("99′");
        x.commitEdit();
        check(catalog.platforms().getFirst().getCenterX() == 60 && displays.get() == before + 1, "Typed pointing clamp/display");
        field.setText("invalid");
        try { x.commitEdit(); } catch (java.text.ParseException expected) {}
        check(catalog.platforms().getFirst().getCenterX() == 60, "Invalid edit changed pointing");
        AbstractButton instrument = find(cell(tree, 9), AbstractButton.class).getFirst();
        boolean old = instrument.isSelected();
        instrument.doClick();
        check(catalog.platforms().get(1).instruments().getFirst().isEnabled() != old, "Swing checkbox target");
        check(tree.isExpanded(0) && tree.isExpanded(8) && tree.getSelectionCount() == 0, "Gesture changed expansion/selection");
        tree.startEditingAtPath(tree.getPathForRow(0));
        check(tree.isEditing(), "Actual renderer row cannot be edited");
        ComponentUtils.setEnabled(pane, false);
        check(!x.isEnabled() && !instrument.isEnabled(), "Disabled renderer controls remain enabled");
        ComponentUtils.setEnabled(pane, true);
        check(x.isEnabled() && instrument.isEnabled(), "Re-enable did not restore renderer controls");
        check(tree.stopEditing(), "Tree editor rejected a valid component");
        tree.startEditingAtPath(tree.getPathForRow(0));
        check(tree.isEditing() && find(cell(tree, 0), JSpinner.class).size() == 2, "Finishing an edit replaced the renderer controls");
        tree.cancelEditing();
        int row = 0;
        for (FOVPlatform platform : catalog.platforms()) {
            screen.number(0, 2 * row, -100);
            screen.number(0, 2 * row + 1, 12.25);
            check(platform.getCenterX() == -60 && platform.getCenterY() == 12.25, "Shared pointing target " + row);
            for (int i = 0; i < platform.instruments().size(); i++) {
                screen.checked(0, 1001 + row + i, true);
                check(platform.instruments().get(i).isEnabled(), "Shared instrument target " + row);
            }
            row += 1 + platform.instruments().size();
        }
        try { screen.number(0, 2, 0); throw new AssertionError("Instrument row accepted as pointing"); }
        catch (IllegalArgumentException expected) {}
        BufferedImage rendered = new BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB);
        pane.paint(rendered.getGraphics());
    }

    private static Component cell(JTree tree, int row) {
        TreePath path = tree.getPathForRow(row);
        Object node = path.getLastPathComponent();
        return tree.getCellRenderer().getTreeCellRendererComponent(tree, node, false, tree.isExpanded(row), tree.getModel().isLeaf(node), row, false);
    }

    private static void layout(Component component) {
        if (component instanceof Container parent) {
            parent.doLayout();
            for (Component child : parent.getComponents())
                layout(child);
        }
    }

    private static <T> List<T> find(Component component, Class<T> type) {
        ArrayList<T> found = new ArrayList<>();
        if (type.isInstance(component))
            found.add(type.cast(component));
        if (component instanceof Container parent)
            for (Component child : parent.getComponents())
                found.addAll(find(child, type));
        return found;
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
