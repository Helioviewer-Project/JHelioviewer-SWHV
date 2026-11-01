package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.ui.Panel;

public final class FilterScreenTest {

    private static final SWEK.Parameter NUMBER = new SWEK.Parameter("n", "Number", new SWEK.ParameterFilter("doublemaxfilter", 0, 10, 1, 1, ""), true);
    private static final SWEK.Parameter FLARE = new SWEK.Parameter("f", "Flux", new SWEK.ParameterFilter("flarefilter", 1e-8, 1e-3, 1e-5, 0.2, ""), true);

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        EventQueue.invokeAndWait(() -> {
            try {
                swingEvents();
                draftsAndValidation();
                decimals();
                shippedNumericFilter();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("FilterScreenTest passed");
    }

    private static SWEKSupplier supplier(String id, List<SWEK.Parameter> parameters) {
        return new SWEKSupplier(new SWEKGroup("test", ""), id, id,
                new SWEK.Source("test", List.of(), null, Map.of("n", SWEK.NumericType.DECIMAL, "f", SWEK.NumericType.DECIMAL, "d", SWEK.NumericType.DECIMAL)), id, parameters);
    }

    private static SWEKSession session(SWEKSupplier... suppliers) {
        return new SWEKSession(new SWEKCatalog(0, List.of(suppliers), List.of()));
    }

    private static Object filters(SWEKSession session, SWEKSupplier supplier) throws Exception {
        Method method = SWEKSession.class.getDeclaredMethod("getFilters", SWEKSupplier.class);
        method.setAccessible(true);
        return method.invoke(session, supplier);
    }

    private static void swingEvents() throws Exception {
        SWEKSupplier supplier = supplier("first", List.of(NUMBER, FLARE));
        SWEKSession session = session(supplier);
        FilterScreen screen = new FilterScreen();
        screen.show(session, supplier);
        SwingPanel panel = new SwingPanel(screen);
        List<JCheckBox> checks = components(panel, JCheckBox.class);
        List<JSpinner> spinners = components(panel, JSpinner.class);
        JButton apply = components(panel, JButton.class).stream().filter(button -> button.getText().equals("Apply")).findFirst().orElseThrow();
        check(apply.isEnabled(), "Initial Apply must be enabled");
        check(checks.stream().noneMatch(JCheckBox::isSelected), "Initial filters must be unchecked");
        check(spinners.stream().noneMatch(JSpinner::isEnabled), "Unchecked values must be disabled");
        check(components(panel, JLabel.class).stream().filter(label -> label.getText().contains(">=")).noneMatch(JLabel::isEnabled), "Unchecked labels must be disabled");
        check(!field(spinners.get(1)).isEditable(), "Swing GOES editor changed its focus/edit role");
        panel.setSize(panel.getPreferredSize());
        layout(panel);
        if (Platform.isMacOS()) {
            check(panel.getWidth() == 138 && panel.getHeight() == 75, "Original content geometry changed: " + panel.getSize());
            check(spinners.getFirst().getBounds().equals(new Rectangle(94, 2, 44, 20)), "Original number bounds changed");
            check(spinners.get(1).getBounds().equals(new Rectangle(83, 2, 51, 20)), "Original GOES bounds changed");
            System.out.println("Current geometry: " + panel.getSize() + ", number " + spinners.getFirst().getBounds() + ", GOES " + spinners.get(1).getBounds());
        }

        apply.doClick();
        check(filters(session, supplier).equals(List.of()), "Unchecked filters were applied");
        check(!apply.isEnabled(), "Apply did not clear dirty state");
        checks.getFirst().doClick();
        check(spinners.getFirst().isEnabled() && apply.isEnabled(), "Checkbox did not enable value and Apply");
        field(spinners.getFirst()).setText("7");
        check(filters(session, supplier).equals(List.of()), "Typing silently applied filters");
        // Hide/reopen of retained content does not commit or apply. Apply explicitly reads the editor.
        panel.setVisible(false);
        check(filters(session, supplier).equals(List.of()), "Dismiss silently applied filters");
        panel.setVisible(true);
        apply.doClick();
        check(filters(session, supplier).equals(List.of(new SWEK.Param("n", 7, SWEK.Operand.BIGGER_OR_EQUAL))), "Apply ignored an uncommitted editor value");
        field(spinners.getFirst()).setText("7.5");
        spinners.getFirst().commitEdit();
        Panel snapshot = screen.build();
        for (int i = 0; i < snapshot.size(); i++)
            if (snapshot.kind(i) == Panel.Kind.NUMBER_SPINNER)
                check(snapshot.value(i, Panel.DoubleField.VALUE) == 7.5, "Whole-number bounds truncated a decimal filter edit");
        spinners.getFirst().commitEdit();
        spinners.getFirst().setValue(7.0);
        field(spinners.getFirst()).setText("invalid");
        checks.get(1).doClick();
        components(spinners.get(1), JButton.class).stream().filter(button -> "Spinner.nextButton".equals(button.getName())).findFirst().orElseThrow().doClick();
        apply.doClick();
        check(filters(session, supplier).equals(List.of(new SWEK.Param("n", 7, SWEK.Operand.BIGGER_OR_EQUAL), new SWEK.Param("f", 1.2e-5, SWEK.Operand.BIGGER_OR_EQUAL))), "Invalid number or GOES stepping changed Apply");
        screen.clear();
        session.close();
    }

    private static void shippedNumericFilter() throws Exception {
        SWEK.Parameter parameter = new SWEK.Parameter("n", "CME speed", new SWEK.ParameterFilter("doubleminmaxfilter", 0, 3000, 1000, 100, ""), true);
        SWEKSupplier supplier = supplier("cme", List.of(parameter));
        SWEKSession session = session(supplier);
        FilterScreen screen = new FilterScreen();
        screen.show(session, supplier);
        SwingPanel panel = new SwingPanel(screen);
        components(panel, JCheckBox.class).getFirst().doClick();
        JSpinner spinner = components(panel, JSpinner.class).getFirst();
        JButton apply = components(panel, JButton.class).stream().filter(button -> button.getText().equals("Apply")).findFirst().orElseThrow();
        apply.doClick();
        check(filters(session, supplier).equals(List.of(new SWEK.Param("n", 1000, SWEK.Operand.BIGGER_OR_EQUAL))), "Untouched CME value was reformatted before Apply");
        components(spinner, JButton.class).stream().filter(button -> "Spinner.nextButton".equals(button.getName())).findFirst().orElseThrow().doClick();
        apply.doClick();
        check(filters(session, supplier).equals(List.of(new SWEK.Param("n", 1100, SWEK.Operand.BIGGER_OR_EQUAL))), "CME up-arrow did not add its declared step");
        screen.clear();
        session.close();
    }

    private static void draftsAndValidation() throws Exception {
        SWEKSupplier first = supplier("a", List.of(NUMBER));
        SWEKSupplier second = supplier("b", List.of(FLARE));
        SWEKSession session = session(first, second);
        FilterScreen screen = new FilterScreen();
        screen.show(session, first);
        int number = owner(screen.build(), Panel.Kind.NUMBER_SPINNER);
        screen.checked(number, 0, true);
        screen.number(number, 1, 4);
        screen.show(session, second);
        int flare = owner(screen.build(), Panel.Kind.TEXT_SPINNER);
        check(number != flare, "Supplier widget identities overlap");
        screen.checked(flare, 0, true);
        screen.number(number, 1, 5);
        screen.setText(flare, 1, "C9.8");
        screen.command(flare, 1, Panel.Command.INCREASE);
        check(text(screen).equals("M1.0"), "GOES upward class boundary changed");
        screen.command(flare, 1, Panel.Command.DECREASE);
        check(text(screen).equals("C9.8"), "GOES downward class boundary changed");
        for (String invalid : List.of("invalid", "XNaN", "XInfinity", "X-1", "A0.1", "X11")) {
            screen.setText(flare, 1, invalid);
            check(text(screen).equals("C9.8"), "Invalid GOES replaced accepted value: " + invalid);
        }
        screen.setText(flare, 1, "A1.0");
        screen.command(flare, 1, Panel.Command.DECREASE);
        check(text(screen).equals("A1.0"), "GOES minimum clamp changed");
        screen.setText(flare, 1, "X10.0");
        screen.command(flare, 1, Panel.Command.INCREASE);
        check(text(screen).equals("X10.0"), "GOES maximum clamp changed");
        check(filters(session, second).equals(List.of()), "Draft edits silently applied");
        screen.show(session, first);
        screen.command(0, -2, Panel.Command.CLICK);
        check(filters(session, first).equals(List.of(new SWEK.Param("n", 5, SWEK.Operand.BIGGER_OR_EQUAL))), "Supplier draft or delayed edit was lost");
        screen.show(session, second);
        check(text(screen).equals("X10.0"), "Second supplier draft was lost");
        SWEKSession replacement = session(first, second);
        screen.show(replacement, first);
        int replacementOwner = owner(screen.build(), Panel.Kind.NUMBER_SPINNER);
        screen.number(number, 1, 9);
        screen.command(0, -2, Panel.Command.CLICK);
        check(replacementOwner != number && filters(replacement, first).equals(List.of()), "Session replacement kept old draft/callback");
        screen.clear();
        screen.checked(replacementOwner, 0, true);
        screen.command(0, -2, Panel.Command.CLICK);
        check(filters(replacement, first).equals(List.of()), "Deactivated screen applied stale callbacks");
        session.close();
        replacement.close();
    }

    private static void decimals() throws Exception {
        SWEK.Parameter decimal = new SWEK.Parameter("d", "Decimal", new SWEK.ParameterFilter("doubleminmaxfilter", 0.1, 9.9, 0.5, 0.1, ""), true);
        SWEKSupplier supplier = supplier("decimal", List.of(decimal));
        SWEKSession session = session(supplier);
        FilterScreen screen = new FilterScreen();
        screen.show(session, supplier);
        SwingPanel panel = new SwingPanel(screen);
        List<JSpinner> spinners = components(panel, JSpinner.class);
        check(spinners.size() == 2, "Min/max operand rows lost");
        components(panel, JCheckBox.class).getFirst().doClick();
        JFormattedTextField field = field(spinners.getFirst());
        field.setText("2.3");
        field.postActionEvent();
        components(panel, JButton.class).stream().filter(button -> button.getText().equals("Apply")).findFirst().orElseThrow().doClick();
        check(filters(session, supplier).equals(List.of(new SWEK.Param("d", 2.3, SWEK.Operand.BIGGER_OR_EQUAL))), "Decimal edit was truncated");
        screen.clear();
        session.close();
    }

    private static String text(FilterScreen screen) {
        Panel panel = screen.build();
        for (int i = 0; i < panel.size(); i++)
            if (panel.kind(i) == Panel.Kind.TEXT_SPINNER)
                return panel.string(i, Panel.IntField.TEXT);
        throw new AssertionError("No text spinner");
    }

    private static int owner(Panel panel, Panel.Kind kind) {
        for (int i = 0; i < panel.size(); i++)
            if (panel.kind(i) == kind)
                return panel.value(i, Panel.IntField.OWNER);
        throw new AssertionError("Missing " + kind);
    }

    private static JFormattedTextField field(JSpinner spinner) {
        return ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
    }

    private static <T> List<T> components(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child))
                result.add(type.cast(child));
            else if (child instanceof Container container)
                result.addAll(components(container, type));
        }
        return result;
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents())
            if (child instanceof Container container)
                layout(container);
    }

    private static void check(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }
}
