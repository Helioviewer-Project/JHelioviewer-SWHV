package org.helioviewer.jhv.gui.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.text.DefaultCaret;

import org.helioviewer.jhv.base.PngPixels;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.time.TimeField;
import org.helioviewer.jhv.io.ExtensionFileFilter;
import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;
import com.jidesoft.swing.JideToggleButton;

// Renders the supported controls of a screen onto retained Swing components. The screen owns content and domain changes;
// this adapter owns widgets, focus, input and painting, and calls the screen with user input.
@SuppressWarnings("serial")
public final class SwingPanel extends JPanel implements Interfaces.LazyComponent {

    @FunctionalInterface
    private interface Update {
        void apply(Panel p, int control);
    }

    // Children of the control go into content.
    private record Widget(JComponent component, JComponent content, Update update) {

        Widget(JComponent component, Update update) {
            this(component, component, update);
        }
    }

    private record Key(Panel.Kind kind, int owner, int id, int choice, int position) {}

    private static final class Node {
        final Widget widget;
        final HashMap<Key, Node> children = new HashMap<>();

        Node(Widget _widget) {
            widget = _widget;
        }
    }

    private final Screen screen;
    private final HashMap<Key, Node> nodes = new HashMap<>();
    private final HashMap<Key, ButtonGroup> groups = new HashMap<>();
    private final ArrayList<Interfaces.LazyComponent> lazy = new ArrayList<>();
    private final AtomicBoolean refreshQueued = new AtomicBoolean();
    private boolean syncing;
    private volatile boolean acting;
    private volatile boolean dirty;
    private int fitted;
    private JComponent initialFocus;

    public SwingPanel(Screen _screen) {
        screen = _screen;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        screen.listen(this::changed);
        refresh();
    }

    // The control of the snapshot that takes the focus when its window opens.
    public JComponent initialFocus() {
        return initialFocus;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        UITimer.register(this);
    }

    @Override
    public void removeNotify() {
        UITimer.unregister(this);
        super.removeNotify();
    }

    // Any thread. Coalesces notifications on the event queue without waiting for the repaint timer; a gesture's end shows them itself.
    private void changed() {
        dirty = true;
        if (!acting && refreshQueued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                refreshQueued.set(false);
                lazyRepaint();
            });
        }
    }

    @Override
    public void lazyRepaint() {
        if (dirty)
            refresh();
        lazy.forEach(Interfaces.LazyComponent::lazyRepaint);
    }

    public void refresh() {
        dirty = false;
        syncing = true;
        Panel p;
        try {
            p = screen.build();
            reconcile(p, 0, this, nodes, null, isEnabled());
        } finally {
            syncing = false;
        }
        int revision = p.size() == 0 ? 0 : p.value(0, Panel.IntField.FIT);
        if (revision != fitted) {
            fitted = revision;
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null && window.isDisplayable()) {
                window.pack();
                window.repaint();
            }
        }
    }

    // Applies the controls up to the end kind to the children of parent, in order, and hides what the snapshot left out.
    private int reconcile(Panel p, int start, JComponent parent, HashMap<Key, Node> children, Panel.Kind end, boolean parentEnabled) {
        if (parent instanceof Cells cells)
            cells.start();
        int i = start;
        int index = 0;
        for (; i < p.size() && p.kind(i) != end; index++) {
            Panel.Kind kind = p.kind(i);
            int id = p.value(i, Panel.IntField.ID);
            Key key = new Key(kind, p.value(i, Panel.IntField.OWNER), id, p.value(i, Panel.IntField.CHOICE), id == 0 ? index : 0);
            Node node = children.get(key);
            if (node == null)
                children.put(key, node = new Node(create(p, i, kind, parent.getLayout())));
            JComponent component = node.widget.component;
            if (index >= parent.getComponentCount() || parent.getComponent(index) != component)
                parent.add(component, index);
            component.setVisible(true);
            if (parent instanceof Cells cells)
                cells.place(component, p.value(i, Panel.IntField.CELL_SPAN), p.has(i, Panel.END_ROW), p.has(i, Panel.RIGHT));
            else if (end == Panel.Kind.COLUMN_END || parent == this)
                component.setAlignmentX(LEFT_ALIGNMENT);
            if (p.has(i, Panel.INITIAL_FOCUS))
                initialFocus = node.widget.content;

            boolean enabled = parentEnabled && p.has(i, Panel.ENABLED);
            if (component.isEnabled() != enabled)
                ComponentUtils.setEnabled(component, enabled);
            String tip = p.string(i, Panel.IntField.TIP);
            tip = tip.isEmpty() ? null : tipText(tip);
            if (!Objects.equals(component.getToolTipText(), tip))
                component.setToolTipText(tip);
            node.widget.update.apply(p, i);

            i = switch (kind) {
                case ROW -> reconcile(p, i + 1, node.widget.content, node.children, Panel.Kind.ROW_END, enabled);
                case COLUMN -> reconcile(p, i + 1, node.widget.content, node.children, Panel.Kind.COLUMN_END, enabled);
                case GRID -> reconcile(p, i + 1, node.widget.content, node.children, Panel.Kind.GRID_END, enabled);
                case SECTION -> reconcile(p, i + 1, node.widget.content, node.children, Panel.Kind.SECTION_END, enabled);
                case SCROLL -> reconcile(p, i + 1, node.widget.content, node.children, Panel.Kind.SCROLL_END, enabled);
                case TABLE -> p.tableEnd(i) + 1;
                case LIST -> p.listEnd(i) + 1;
                case TREE_VIEW -> p.treeViewEnd(i) + 1;
                default -> i + 1;
            };
        }
        for (int c = index; c < parent.getComponentCount(); c++)
            parent.getComponent(c).setVisible(false);
        return i + 1;
    }

    private Widget create(Panel p, int i, Panel.Kind kind, LayoutManager parent) {
        boolean inRow = parent instanceof RowLayout;
        int owner = p.value(i, Panel.IntField.OWNER);
        int item = p.value(i, Panel.IntField.ID);
        return switch (kind) {
            case ROW -> row(p, i, owner, inRow);
            case COLUMN -> new Widget(column(p, i), (s, c) -> {});
            case GRID -> grid(p, i);
            case SPACE -> space(p, i);
            case SEPARATOR -> new Widget((JComponent) Box.createRigidArea(new Dimension()), (s, c) -> {});
            case LABEL -> p.string(i, Panel.IntField.WIDTH_TEXT).isEmpty() ? label(parent instanceof RowLayout row && row.gap == 0) : reservedLabel();
            case BUTTON -> button(p, i, owner, item);
            case CHECK -> check(p.has(i, Panel.FILL), owner, item);
            case RADIO -> radio(p, i, owner, item);
            case COMBO -> p.string(i, Panel.IntField.LABEL).isEmpty() ? combo(p, i, owner, item) : picker(p, i, owner, item);
            case SPLIT_BUTTON -> splitButton(p, i, owner, item);
            case NUMBER_SPINNER -> spinner(p, i, owner, item);
            case SLIDER -> slider(p, i, owner, item);
            case TEXT -> !p.string(i, Panel.IntField.WIDTH_TEXT).isEmpty() ? text(p, i)
                    : p.value(i, Panel.DoubleField.MIN) < p.value(i, Panel.DoubleField.MAX) ? numericField(p, i, owner, item) : editor(p, i, owner, item);
            case TIME_FIELD -> timeField(p, i, owner, item);
            case TIME_SLIDER -> timeSlider(p, i, owner, item);
            case IMAGE -> image();
            case SECTION -> section(p, i, owner);
            case SCROLL -> scroll(p, i);
            case TABLE -> table(p, i);
            case LIST -> list(p, i);
            case TREE_VIEW -> treeView(p, i);
            default -> throw new IllegalArgumentException("Swing has no " + kind);
        };
    }

    // Shows the plain x^y and x^(y) of shared tips as superscripts.
    private static String tipText(String tip) {
        return tip.indexOf('^') < 0 ? tip : "<html><body>" + tip.replaceAll("\\^\\((.*?)\\)|\\^(\\S)", "<sup>$1$2</sup>");
    }

    // User input runs its callback, then shows the model it left. Programmatic updates report nothing.
    private void act(Runnable callback) {
        if (!syncing) {
            acting = true;
            try {
                callback.run();
            } finally {
                acting = false;
                refresh();
            }
        }
    }

    private static void applyFont(Panel p, int i, JComponent component) {
        if (!p.has(i, Panel.MONO | Panel.SMALL))
            return;
        boolean small = p.has(i, Panel.SMALL);
        component.setFont(p.has(i, Panel.MONO) ? (small ? UIGlobals.uiFontMonoSmall : UIGlobals.uiFontMono) : UIGlobals.uiFontSmall);
    }

    private Widget row(Panel p, int i, int owner, boolean inRow) {
        Line row = new Line(new RowLayout(p.has(i, Panel.RIGHT), inRow || p.has(i, Panel.FILL), p.value(i, Panel.IntField.GAP)));
        applyMargin(p, i, row);
        int hover = p.value(i, Panel.IntField.SECONDARY_ID);
        if (hover != 0) {
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    act(() -> screen.command(owner, hover, Panel.Command.ENTER));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    act(() -> screen.command(owner, hover, Panel.Command.EXIT));
                }
            });
        }
        return new Widget(row, (s, c) -> {});
    }

    private static void applyMargin(Panel p, int i, JComponent component) {
        int margin = p.value(i, Panel.IntField.MARGIN);
        if (margin != 0)
            component.setBorder(BorderFactory.createEmptyBorder(margin >>> 24, margin >> 16 & 0xFF, margin >> 8 & 0xFF, margin & 0xFF));
    }

    private static JPanel column(Panel p, int i) {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
        applyMargin(p, i, column);
        return column;
    }

    private static Widget grid(Panel p, int i) {
        Cells cells = new Cells(p.value(i, Panel.IntField.COLUMNS), p.value(i, Panel.IntField.STRETCH) - 1, p.value(i, Panel.IntField.GAP), p.has(i, Panel.RIGHT));
        applyMargin(p, i, cells);
        return new Widget(cells, (s, c) -> {});
    }

    private static Widget space(Panel p, int i) {
        if (!p.has(i, Panel.POINT_SIZE) && (p.value(i, Panel.IntField.WIDTH) != 0 || p.value(i, Panel.IntField.HEIGHT) != 0))
            throw new IllegalArgumentException("Swing space needs point sizes");
        JComponent space = (JComponent) Box.createRigidArea(new Dimension(p.value(i, Panel.IntField.WIDTH), p.value(i, Panel.IntField.HEIGHT)));
        return new Widget(space, (s, c) -> {
            int foreground = s.value(c, Panel.IntField.FOREGROUND);
            space.setOpaque(foreground != 0);
            space.setBackground(foreground == 0 ? null : new Color(foreground & 0xFFFFFF));
        });
    }

    private static Widget label(boolean spaced) {
        JLabel label = new JLabel();
        return new Widget(label, (p, i) -> {
            applyFont(p, i, label);
            label.setText(p.string(i, Panel.IntField.LABEL));
            // Separates the words from the control after them.
            int gap = spaced && p.kind(i + 1) != Panel.Kind.ROW_END ? label.getFontMetrics(label.getFont()).charWidth(' ') : 0;
            if (label.getInsets().right != gap)
                label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, gap));
        });
    }

    private static Widget reservedLabel() {
        ReservedLabel label = new ReservedLabel();
        return new Widget(label, (p, i) -> {
            applyFont(p, i, label);
            label.update(p.string(i, Panel.IntField.LABEL), p.string(i, Panel.IntField.WIDTH_TEXT), p.has(i, Panel.RIGHT));
        });
    }

    private Widget button(Panel p, int i, int owner, int item) {
        AbstractButton button = p.has(i, Panel.CHECKABLE) ? new JideToggleButton() : p.has(i, Panel.FLAT) ? new JideButton() : new JButton();
        Color normal = button.getForeground();
        int iconSize = p.value(i, Panel.IntField.ICON_SIZE);
        if (iconSize > 0)
            button.setFont(Buttons.getMaterialFont(iconSize));
        if (p.has(i, Panel.POINT_SIZE))
            button.setPreferredSize(new Dimension(p.value(i, Panel.IntField.WIDTH), p.value(i, Panel.IntField.HEIGHT)));
        if (p.has(i, Panel.REPEAT))
            HoldRepeat.install(button, (int) Math.round(1000 * p.value(i, Panel.DoubleField.REPEAT)));
        Panel.Act action = Panel.Act.values()[p.value(i, Panel.IntField.ACTION)];
        String[] directory = new String[1];
        switch (action) {
            case COMMAND -> button.addActionListener(e -> act(() -> screen.command(owner, item, Panel.Command.CLICK)));
            case FILE -> {
                String[] extensions = p.choices(i);
                boolean multiple = p.has(i, Panel.MULTIPLE);
                button.addActionListener(e -> chooseFiles(directory[0], extensions, multiple, owner, item));
            }
            default -> throw new IllegalArgumentException("Swing button has no " + action);
        }
        return new Widget(button, (s, c) -> {
            directory[0] = s.string(c, Panel.IntField.DIRECTORY);
            String icon = s.string(c, Panel.IntField.ICON);
            String label = s.string(c, Panel.IntField.LABEL);
            button.setText(icon.isEmpty() ? label : icon);
            button.setSelected(s.has(c, Panel.SELECTED));
            JRootPane root = SwingUtilities.getRootPane(button);
            if (root != null && button instanceof JButton defaultButton && s.has(c, Panel.DEFAULT)) root.setDefaultButton(defaultButton);
            int foreground = s.value(c, Panel.IntField.FOREGROUND);
            button.setForeground(foreground == 0 ? normal : new Color(foreground & 0xFFFFFF));
            if (label.isEmpty())
                button.getAccessibleContext().setAccessibleName(s.string(c, Panel.IntField.TIP));
        });
    }

    private void chooseFiles(String directory, String[] extensions, boolean multiple, int owner, int item) {
        FileDialog dialog = new FileDialog(MainFrame.get(), "Choose a file", FileDialog.LOAD);
        dialog.setFilenameFilter(ExtensionFileFilter.of(extensions));
        dialog.setDirectory(directory);
        dialog.setMultipleMode(multiple);
        dialog.setVisible(true);
        File[] files = dialog.getFiles();
        if (files.length > 0) {
            String[] paths = new String[files.length];
            Arrays.setAll(paths, n -> files[n].toString());
            act(() -> screen.files(owner, item, paths));
        }
    }

    private Widget check(boolean fill, int owner, int item) {
        JCheckBox check = fill ? new WideCheck() : new JCheckBox();
        check.addActionListener(e -> act(() -> screen.checked(owner, item, check.isSelected())));
        return new Widget(check, (p, i) -> {
            String description = p.string(i, Panel.IntField.DESCRIPTION);
            String label = p.string(i, Panel.IntField.LABEL);
            check.setText(description.isEmpty() ? label : "<html>" + label + "<br/><small>" + description);
            check.setSelected(p.has(i, Panel.SELECTED));
        });
    }

    private Widget radio(Panel p, int i, int owner, int item) {
        int choice = p.value(i, Panel.IntField.CHOICE);
        JRadioButton radio = new JRadioButton();
        // Keeps the field it leads apart from it.
        if (p.kind(i + 1) == Panel.Kind.TEXT) {
            Insets margin = radio.getMargin();
            radio.setMargin(new Insets(margin.top, margin.left, margin.bottom, margin.right + 5));
        }
        groups.computeIfAbsent(new Key(Panel.Kind.RADIO, owner, item, 0, 0), key -> new ButtonGroup()).add(radio);
        radio.addActionListener(e -> act(() -> screen.choice(owner, item, choice)));
        return new Widget(radio, (s, c) -> {
            radio.setText(s.string(c, Panel.IntField.LABEL));
            radio.setSelected(s.has(c, Panel.SELECTED));
        });
    }

    private Widget combo(Panel p, int i, int owner, int item) {
        JComboBox<String> combo = new JComboBox<>(p.choices(i));
        String widest = p.string(i, Panel.IntField.WIDTH_TEXT);
        if (!widest.isEmpty())
            combo.setPrototypeDisplayValue(widest);
        String[][] tips = {p.choiceTips(i)};
        ListCellRenderer<? super String> plain = combo.getRenderer();
        combo.setRenderer((list, value, index, selected, focused) -> {
            Component cell = plain.getListCellRendererComponent(list, value, index, selected, focused);
            if (cell instanceof JComponent shown)
                shown.setToolTipText(index < 0 || index >= tips[0].length || tips[0][index].isEmpty() ? null : tips[0][index]);
            return cell;
        });
        combo.addActionListener(e -> act(() -> screen.choice(owner, item, combo.getSelectedIndex())));
        return new Widget(combo, (s, c) -> {
            tips[0] = s.choiceTips(c);
            String[] choices = s.choices(c);
            String[] shown = new String[combo.getItemCount()];
            Arrays.setAll(shown, combo::getItemAt);
            if (!Arrays.equals(shown, choices))
                combo.setModel(new DefaultComboBoxModel<>(choices));
            int index = s.value(c, Panel.IntField.INDEX);
            if (combo.getSelectedIndex() != index)
                combo.setSelectedIndex(index);
        });
    }

    private Widget picker(Panel p, int i, int owner, int item) {
        ChoicePicker picker = new ChoicePicker(p.string(i, Panel.IntField.LABEL), index -> act(() -> screen.choice(owner, item, index)));
        return new Widget(picker, (s, c) -> picker.update(s.choices(c), s.choiceTips(c), s.value(c, Panel.IntField.INDEX)));
    }

    private Widget splitButton(Panel p, int i, int owner, int item) {
        JideSplitButton button = new JideSplitButton(Buttons.button(p.string(i, Panel.IntField.ICON)));
        button.setMargin(new Insets(0, 0, 0, 0));
        int choiceItem = p.value(i, Panel.IntField.SECONDARY_ID);
        String[] choices = p.choices(i);
        JRadioButtonMenuItem[] menu = new JRadioButtonMenuItem[choices.length];
        ButtonGroup group = new ButtonGroup();
        for (int choice = 0; choice < choices.length; choice++) {
            int index = choice;
            menu[index] = new JRadioButtonMenuItem(choices[index]);
            menu[index].addActionListener(e -> act(() -> screen.choice(owner, choiceItem, index)));
            group.add(menu[index]);
            button.add(menu[index]);
        }
        button.addActionListener(e -> act(() -> screen.command(owner, item, Panel.Command.CLICK)));
        return new Widget(button, (s, c) -> menu[s.value(c, Panel.IntField.INDEX)].setSelected(true));
    }

    private Widget spinner(Panel p, int i, int owner, int item) {
        if (p.value(i, Panel.IntField.DECIMALS) != 0)
            throw new IllegalArgumentException("Swing spinner needs whole numbers");
        SpinnerNumberModel model = new SpinnerNumberModel((int) p.value(i, Panel.DoubleField.VALUE), (int) p.value(i, Panel.DoubleField.MIN),
                (int) p.value(i, Panel.DoubleField.MAX), (int) p.value(i, Panel.DoubleField.STEP));
        JHVSpinner spinner = new JHVSpinner(model);
        JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        spinner.addChangeListener(e -> act(() -> screen.number(owner, item, ((Number) spinner.getValue()).doubleValue())));
        return new Widget(spinner, (s, c) -> {
            model.setMinimum((int) s.value(c, Panel.DoubleField.MIN));
            model.setMaximum((int) s.value(c, Panel.DoubleField.MAX));
            model.setStepSize((int) s.value(c, Panel.DoubleField.STEP));
            // The model, not getValue(): reading the spinner commits an edit in progress, which stays until the user ends it.
            int value = (int) s.value(c, Panel.DoubleField.VALUE);
            if (model.getNumber().intValue() != value) {
                if (editing(field))
                    dirty = true;
                else
                    spinner.setValue(value);
            }
        });
    }

    private static boolean editing(JFormattedTextField field) {
        try {
            return !field.getText().equals(field.getFormatter().valueToString(field.getValue()));
        } catch (ParseException e) {
            return true;
        }
    }

    private Widget slider(Panel p, int i, int owner, int item) {
        double min = p.value(i, Panel.DoubleField.MIN);
        double max = p.value(i, Panel.DoubleField.MAX);
        int scale = (int) Math.round(1 / p.value(i, Panel.DoubleField.STEP));
        JHVSlider slider = new JHVSlider(min, max, Math.clamp(p.value(i, Panel.DoubleField.VALUE), min, max), scale);
        slider.setPreferredSize(new Dimension(100, slider.getPreferredSize().height));
        slider.addChangeListener(e -> act(() -> screen.number(owner, item, slider.getDoubleValue())));
        if (p.has(i, Panel.ACTIVATE))
            slider.addMouseListener(press(owner, item));
        return new Widget(slider, (s, c) -> slider.setDoubleValue(s.value(c, Panel.DoubleField.VALUE)));
    }

    // Runs after the widget's own handling of the press.
    private MouseAdapter press(int owner, int item) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                act(() -> screen.command(owner, item, Panel.Command.PRESS));
            }
        };
    }

    private Widget timeField(Panel p, int i, int owner, int item) {
        TimeField[] holder = new TimeField[1];
        TimeField field = new TimeField(p.string(i, Panel.IntField.TIP), () -> act(() -> screen.number(owner, item, holder[0].getTime())));
        holder[0] = field;
        field.setColumns(p.value(i, Panel.IntField.WIDTH));
        return new Widget(field, (s, c) -> {
            long time = (long) s.value(c, Panel.DoubleField.VALUE);
            if (field.getTime() != time)
                field.setTime(time);
        });
    }

    private Widget text(Panel p, int i) {
        if (!p.has(i, Panel.READ_ONLY))
            throw new IllegalArgumentException("Swing text needs read-only");
        ReadOnlyText field = new ReadOnlyText(p.string(i, Panel.IntField.WIDTH_TEXT));
        field.setHorizontalAlignment(p.has(i, Panel.RIGHT) ? JTextField.RIGHT : JTextField.LEFT);
        if (p.has(i, Panel.FLAT)) {
            field.setOpaque(false);
            field.setBorder(BorderFactory.createEmptyBorder());
        }
        lazy.add(field);
        return new Widget(field, (s, c) -> {
            applyFont(s, c, field);
            field.update(s.string(c, Panel.IntField.TEXT));
        });
    }

    // Reports each edit.
    private Widget editor(Panel p, int i, int owner, int item) {
        if (!p.has(i, Panel.LIVE))
            throw new IllegalArgumentException("Swing text needs live or numeric");
        int columns = p.value(i, Panel.IntField.WIDTH);
        JTextField field = p.has(i, Panel.PASSWORD) ? new JPasswordField(columns) : new JTextField(columns);
        Runnable typed = () -> act(() -> screen.setText(owner, item, field.getText()));
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                typed.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                typed.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                typed.run();
            }
        });
        return new Widget(field, (s, c) -> {
            field.setEditable(!s.has(c, Panel.READ_ONLY));
            String text = s.string(c, Panel.IntField.TEXT);
            if (!text.equals(field.getText()))
                field.setText(text);
        });
    }

    // Commits on Enter and focus loss; an invalid edit stays through Enter and reverts on focus loss.
    private Widget numericField(Panel p, int i, int owner, int item) {
        JFormattedTextField field = new JFormattedTextField(new TerminatedFormatterFactory("%g", "", p.value(i, Panel.DoubleField.MIN), p.value(i, Panel.DoubleField.MAX)));
        field.setColumns(p.value(i, Panel.IntField.WIDTH));
        field.addPropertyChangeListener("value", e -> act(() -> screen.setText(owner, item, field.getText())));
        if (p.has(i, Panel.ACTIVATE))
            field.addMouseListener(press(owner, item));
        return new Widget(field, (s, c) -> {
            double value = s.value(c, Panel.DoubleField.VALUE);
            if (!(field.getValue() instanceof Number number) || number.doubleValue() != value)
                field.setValue(value);
            field.setEditable(!s.has(c, Panel.READ_ONLY));
        });
    }

    private Widget timeSlider(Panel p, int i, int owner, int item) {
        TimeSlider slider = new TimeSlider(new TimeSlider.Target() {
            @Override
            public void seek(int frame) {
                act(() -> screen.choice(owner, item, frame));
            }

            @Override
            public void range(int first, int last) {
                act(() -> screen.setRange(owner, item, first, last));
            }

            @Override
            public void command(Panel.Command command) {
                act(() -> screen.command(owner, item, command));
            }
        });
        if (p.has(i, Panel.POINT_SIZE)) {
            Dimension size = slider.getPreferredSize();
            size.height = p.value(i, Panel.IntField.HEIGHT);
            slider.setPreferredSize(size);
        }
        lazy.add(slider);
        return new Widget(slider, (s, c) -> slider.update(s.value(c, Panel.IntField.INDEX),
                (int) s.value(c, Panel.DoubleField.MIN), (int) s.value(c, Panel.DoubleField.MAX), s.value(c, Panel.IntField.LAST_FRAME), s.payload(c)));
    }

    private static Widget image() {
        Picture picture = new Picture();
        return new Widget(picture, (p, i) -> picture.update(p.payload(i), p.value(i, Panel.IntField.WIDTH)));
    }

    private Widget section(Panel p, int i, int owner) {
        int item = p.value(i, Panel.IntField.SECONDARY_ID);
        if (item == 0)
            throw new IllegalArgumentException("Swing section needs an id");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        Section section = new Section(p.string(i, Panel.IntField.LABEL), content, p.has(i, Panel.OPEN), owner, item);
        return new Widget(section, content, (s, c) -> section.update(s.string(c, Panel.IntField.LABEL), s.has(c, Panel.OPEN)));
    }

    private Widget scroll(Panel p, int i) {
        if (p.value(i, Panel.IntField.HEIGHT) != 0 && !p.has(i, Panel.POINT_SIZE))
            throw new IllegalArgumentException("Swing scroll needs a free height or point size");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        JScrollPane pane = new JScrollPane(content);
        if (p.has(i, Panel.POINT_SIZE)) pane.setPreferredSize(pointSize(p, i));
        return new Widget(pane, content, (s, c) -> {});
    }

    private Widget table(Panel p, int i) {
        PanelTable table = new PanelTable(p, i, screen, this::act);
        return new Widget(new JScrollPane(table), table::update);
    }

    private Widget list(Panel p, int i) {
        PanelList list = new PanelList(p, i, screen, this::act);
        JScrollPane pane = new JScrollPane(list);
        pane.setPreferredSize(pointSize(p, i));
        return new Widget(pane, list::update);
    }

    private Widget treeView(Panel p, int i) {
        PanelTree tree = new PanelTree(p, i, screen, this::act);
        JScrollPane pane = new JScrollPane(tree);
        pane.setPreferredSize(pointSize(p, i));
        return new Widget(pane, tree, tree::update);
    }

    private static Dimension pointSize(Panel p, int i) {
        return new Dimension(p.value(i, Panel.IntField.WIDTH), p.value(i, Panel.IntField.HEIGHT));
    }

    // Preferred sizes in a line, apart by the gap. A nested row or grid takes the rest of the line at full height and fills its children's height.
    private record RowLayout(boolean right, boolean fill, int gap) implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component component) {}

        @Override
        public void removeLayoutComponent(Component component) {}

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Dimension size = new Dimension();
            int shown = 0;
            for (Component component : parent.getComponents()) {
                if (component.isVisible()) {
                    Dimension preferred = component.getPreferredSize();
                    size.width += preferred.width;
                    size.height = Math.max(size.height, preferred.height);
                    shown++;
                }
            }
            Insets insets = parent.getInsets();
            size.width += Math.max(shown - 1, 0) * gap + insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth() - insets.left - insets.right;
            int height = parent.getHeight() - insets.top - insets.bottom;
            int total = preferredLayoutSize(parent).width - insets.left - insets.right;
            int x = insets.left + (right ? width - total : 0);
            for (Component component : parent.getComponents()) {
                if (component.isVisible()) {
                    Dimension preferred = component.getPreferredSize();
                    boolean nested = component instanceof Cells || component instanceof JPanel row && row.getLayout() instanceof RowLayout;
                    int w = nested ? width - total + preferred.width : preferred.width;
                    int h = nested || fill ? height : preferred.height;
                    component.setBounds(x, insets.top + (height - h) / 2, w, h);
                    x += w + gap;
                }
            }
        }
    }

    // Cells by rows, columns apart by the gap. The stretch column takes the width left by the others, as does a cell across all columns.
    // A right grid aligns its first column to the right. An empty cell after the stretch column gives that column's cell its width.
    private static final class Cells extends JPanel {

        private final GridBagLayout grid = new GridBagLayout();
        private final int columns;
        private final int stretch;
        private final int gap;
        private final boolean right;
        private int column;
        private int row;
        private Component stretched;

        Cells(int _columns, int _stretch, int _gap, boolean _right) {
            columns = _columns;
            stretch = _stretch;
            gap = _gap;
            right = _right;
            setLayout(grid);
        }

        void start() {
            column = row = 0;
        }

        void place(Component component, int span, boolean endRow, boolean rightCell) {
            if (column == stretch)
                stretched = component;
            else if (stretched != null && column == stretch + 1 && component instanceof Box.Filler && component.getPreferredSize().width == 0) {
                GridBagConstraints c = grid.getConstraints(stretched);
                c.gridwidth++;
                grid.setConstraints(stretched, c);
            }
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = column;
            c.gridy = row;
            c.gridwidth = Math.max(span, 1);
            c.anchor = rightCell || right && column == 0 ? GridBagConstraints.LINE_END : GridBagConstraints.LINE_START;
            c.insets.left = column == 0 ? 0 : gap;
            boolean fills = column == stretch || c.gridwidth == columns;
            c.fill = fills ? GridBagConstraints.BOTH : GridBagConstraints.NONE;
            c.weightx = fills ? 1 : 0;
            grid.setConstraints(component, c);
            column += c.gridwidth;
            if (endRow || column >= columns) {
                column = 0;
                row++;
            }
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
        }
    }

    // A row keeps its height when a window gives its column more.
    private static final class Line extends JPanel {

        Line(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
        }
    }

    // Takes clicks across its column.
    private static final class WideCheck extends JCheckBox {

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    // Reports its toggles and shows the state of the snapshot.
    private final class Section extends CollapsiblePane {

        private final int owner;
        private final int item;

        Section(String title, JComponent content, boolean open, int _owner, int _item) {
            super(title, content, open);
            owner = _owner;
            item = _item;
        }

        void update(String title, boolean open) {
            setTitle(title);
            if (isExpanded() != open)
                super.actionPerformed(null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            act(() -> screen.checked(owner, item, isExpanded()));
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension size = super.getMaximumSize();
            return isExpanded() ? size : new Dimension(size.width, getPreferredSize().height);
        }
    }

    private static final class Picture extends JLabel {

        private byte[] png = {};

        void update(byte[] _png, int width) {
            if (Arrays.equals(png, _png))
                return;
            png = _png;
            try {
                BufferedImage image = PngPixels.decode(png).image();
                setIcon(new ImageIcon(image.getWidth() == width ? image : image.getScaledInstance(width, -1, Image.SCALE_SMOOTH)));
            } catch (IOException e) {
                throw new IllegalArgumentException("Swing image needs a decodable file", e);
            }
        }
    }

    // A label reserving the width of its widest text: value updates repaint without relayout.
    private static final class ReservedLabel extends JComponent {

        private final Map<?, ?> desktopHints;
        private String text = "";
        private String reserved = "";
        private boolean right;

        ReservedLabel() {
            Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            desktopHints = hints instanceof Map<?, ?> map ? map : null;
            setForeground(UIGlobals.foreColor);
        }

        void update(String _text, String _reserved, boolean _right) {
            right = _right;
            if (!text.equals(_text)) {
                text = _text;
                repaint();
            }
            if (!reserved.equals(_reserved)) {
                reserved = _reserved;
                revalidate();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(reserved), fm.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g1) {
            Graphics2D g = (Graphics2D) g1.create();
            try {
                if (desktopHints != null)
                    g.addRenderingHints(desktopHints);

                g.setColor(getForeground());
                g.setFont(getFont());

                FontMetrics fm = g.getFontMetrics();
                int x = right ? getWidth() - fm.stringWidth(text) : 0;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                BasicGraphicsUtils.drawString(this, g, text, x, y);
            } finally {
                g.dispose();
            }
        }
    }

    // Selectable text of constant width: updates keep the selection and the caret, need no relayout and repaint on the UI tick unless focused.
    private static final class ReadOnlyText extends JTextField implements Interfaces.LazyComponent {

        private final String widthText;
        private boolean dirty;

        ReadOnlyText(String _widthText) {
            widthText = _widthText;
            setEditable(false);
            ((DefaultCaret) getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }

        void update(String text) {
            if (text.equals(getText()))
                return;
            int selectionStart = getSelectionStart();
            int selectionEnd = getSelectionEnd();
            setText(text);
            if (selectionStart != selectionEnd)
                select(selectionStart, selectionEnd);
        }

        // Keeps Swing's caret margin and one extra pixel to prevent horizontal scrolling.
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (!widthText.isEmpty())
                size.width = getInsets().left + getInsets().right + getFontMetrics(getFont()).stringWidth(widthText) + 2;
            return size;
        }

        @Override
        public void revalidate() {}

        // Swing's scroll model can request another repaint during painting.
        @Override
        public void repaint(long delay, int x, int y, int width, int height) {
            if (isFocusOwner())
                super.repaint(delay, x, y, width, height);
            else
                dirty = true;
        }

        @Override
        public void lazyRepaint() {
            if (dirty) {
                dirty = false;
                super.repaint(0L, 0, 0, getWidth(), getHeight());
            }
        }
    }

}
