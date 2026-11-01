package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.ui.Panel;

// Real PFSS field edits and availability routing, without a window.
public final class PfssOptionsTest {

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
                pfss();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("PfssOptionsTest passed");
        System.exit(0);
    }

    public static PfssLayer pfssLayer() throws Exception {
        return new PfssLayer(null, null);
    }

    private static void pfss() throws Exception {
        PfssLayer layer = pfssLayer();
        ArrayList<String> opened = new ArrayList<>();
        PfssOptions screen = new PfssOptions(layer, opened::add);
        SwingPanel pane = new SwingPanel(screen);
        pane.setSize(600, 85);
        layout(pane);
        if (Platform.isMacOS())
            check(pane.getPreferredSize().equals(new Dimension(318, 54)), "PFSS preferred size");
        List<JSpinner> spinners = find(pane, JSpinner.class);
        check(spinners.size() == 2, "PFSS spinners");
        spinners.get(0).setValue(3);
        spinners.get(1).setValue(1.234);
        check(((JSpinner.DefaultEditor) spinners.get(1).getEditor()).getTextField().getText().equals("1.234"), "PFSS toolkit decimal formatting");
        spinners.get(1).setValue(1.2);
        check(layer.getDetail() == 3 && layer.getRadius() == 1.2, "PFSS Swing model writes");
        for (int i = 0; i < spinners.size(); i++) {
            JSpinner spinner = spinners.get(i);
            JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            field.setText(i == 0 ? "1,5" : "2,0");
            try {
                spinner.commitEdit();
                throw new AssertionError("Comma input must retain the original PFSS rejection");
            } catch (java.text.ParseException expected) {}
            field.setValue(spinner.getModel().getValue());
        }
        check(layer.getDetail() == 3 && layer.getRadius() == 1.2, "Rejected text changed PFSS values");
        AbstractButton fixed = find(pane, AbstractButton.class).stream().filter(b -> b.getText().equals("Fixed colors")).findFirst().orElseThrow();
        fixed.doClick();
        check(layer.getFixedColor(), "Fixed colors callback");
        Panel snapshot = screen.build();
        check(snapshot.value(control(snapshot, 1), Panel.DoubleField.MIN) == 1.099999999999999, "Radius minimum");
        check(snapshot.value(control(snapshot, 1), Panel.IntField.DECIMALS) == 1, "Native radius precision");
        AbstractButton available = find(pane, AbstractButton.class).stream().filter(b -> b.getText().equals("Available data")).findFirst().orElseThrow();
        available.doClick();
        check(opened.equals(List.of(PfssSettings.AVAILABILITY_URL)), "Availability URL");
        check(available.getToolTipText().equals("Click here to check the PFSS data availability"), "Availability tooltip");
        check(available.getY() > fixed.getY(), "Availability must occupy second row");
        screen.number(0, 0, 2);
        screen.number(0, 1, 2.3);
        screen.checked(0, 2, false);
        pane.refresh();
        check(layer.getDetail() == 2 && layer.getRadius() == 2.3 && !fixed.isSelected(), "Shared PFSS controls");
    }

    private static int control(Panel panel, int id) {
        for (int i = 0; i < panel.size(); i++)
            if (panel.kind(i) == Panel.Kind.NUMBER_SPINNER && panel.value(i, Panel.IntField.ID) == id)
                return i;
        throw new AssertionError("Missing spinner " + id);
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
