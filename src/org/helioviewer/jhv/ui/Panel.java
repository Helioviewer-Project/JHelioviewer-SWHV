package org.helioviewer.jhv.ui;

import java.util.Arrays;

// Reusable snapshot of a screen's controls, independent of any toolkit. Its arrays stay owned by this panel and are read synchronously on the model thread.
// Each kind is one widget with one callback; containers run from their kind to the matching end kind.
public final class Panel {

    public enum Kind {
        LABEL, SPACE, BUTTON, TOOL, CHECK, RADIO, TOGGLE, COMBO, SWATCH, SLIDER, RANGE, NUMBER_SPINNER, TEXT_SPINNER, TEXT, TEXT_AREA, LINK, IMAGE,
        SPINNER, SEPARATOR, TIME_SLIDER, ITEM, MENU_ITEM,
        ROW, ROW_END, COLUMN, COLUMN_END, GRID, GRID_END, TABLE, TABLE_END, SECTION, SECTION_END, SCROLL, SCROLL_END, MENU, MENU_END, TREE, TREE_END,
        TIME_FIELD, SPLIT_BUTTON, SPLIT, SPLIT_END, PROGRESS_TOGGLE, MESSAGE_ICON
    }
    public enum IntField {
        KIND, ID, OWNER, FLAGS, INDEX, CHOICE, POPUP, WIDTH, HEIGHT, COLUMNS, STRETCH, MAX_ROWS, DECIMALS, LAST_FRAME, ACTION,
        CHOICES_START, CHOICES_COUNT, KEY, MODIFIERS, FOREGROUND,
        LABEL, TEXT, TIP, ICON, FILTER, NAME, DIRECTORY,
        ACTIVATE_COLUMNS, DOUBLE_CLICK_COLUMNS, ICON_SIZE, SECONDARY_ID, BYTES_START, BYTES_COUNT, COLUMN_START,
        COPY_COLUMNS, SEARCH_COLUMNS, ROW_HEIGHT, WIDTH_TEXT, DESCRIPTION, ALIGN_GRID, ALIGN_COLUMN, FLEX_COLUMNS, CELL_SPAN, LARGE, DOUBLE_INDEX, FIT, GAP, MARGIN
    }
    public enum DoubleField {
        VALUE, VALUE2, MIN, MAX, STEP, SCALE(1), REPEAT;

        public final double initial;

        DoubleField() {
            this(0);
        }

        DoubleField(double _initial) {
            initial = _initial;
        }
    }
    // What a button does when clicked.
    public enum Act { COMMAND, FILE, SAVE, URL, PASTE }
    public enum Command { CLICK, DOUBLE_CLICK, PRESS, SELECT_ONLY, SELECT_ADD, SELECT_REMOVE, SCRUB_START, SCRUB_END, PLAY_PAUSE, STEP_BACKWARD, STEP_FORWARD, INCREASE, DECREASE, ENTER, EXIT }
    public enum Key { NONE, COMMA, Q, N, O, S, R, V, DIGIT0, DIGIT9, EQUALS, MINUS, P, LEFT, RIGHT, T, SLASH }
    public enum Notice { INFO, WARNING, ERROR }

    public static final int COMMAND = 1;
    public static final int ALT = 2;
    public static final int SHIFT = 4;
    public static final int CONTROL = 8;
    public static final int INTS = IntField.values().length;
    public static final int DOUBLES = DoubleField.values().length;
    public static final int SPAN_STRIDE = 2;
    public static final int CHOICE_STRIDE = 3;
    public static final int COLUMN_STRIDE = 3;

    public static int choiceIndex(int index, int count) {
        if (index < 0 || index >= count)
            throw new IllegalArgumentException("Choice " + index + " outside [0, " + count + ')');
        return index;
    }

    public static <T> T choice(T[] values, int index) {
        return values[choiceIndex(index, values.length)];
    }

    public static String[] labels(Object[] values) {
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++)
            labels[i] = values[i].toString();
        return labels;
    }

    public static final class FileFilter {
        final String name;
        final String[] extensions;

        public FileFilter(String _name, String... _extensions) {
            name = _name;
            extensions = _extensions;
        }
    }

    // Control flags.
    public static final int ENABLED = 1;
    public static final int SELECTED = 2; // also checked
    public static final int STRONG = 4;
    public static final int MONO = 8;
    public static final int SMALL = 16;
    public static final int WRAP = 32;
    public static final int END_ROW = 64;
    public static final int OPEN = 128;
    public static final int DEFAULT = 256; // the dialog's Enter action
    public static final int RIGHT = 512; // a row laid out from the right
    public static final int CENTER = 1024; // a centered column
    public static final int MULTIPLE = 2048; // the file dialog or item list allows multiple selection
    public static final int LIVE = 4096; // text reports each edit, for search fields
    public static final int PASSWORD = 8192;
    public static final int READ_ONLY = 16384; // selectable text that cannot be edited
    public static final int ACTIVATE = 32768; // report Command.PRESS even without a value change
    public static final int REPEAT = 65536; // the button repeats while held
    public static final int TIGHT = 131072; // a row without gaps between its controls
    public static final int DRAGGABLE = 262144; // the table row may be dragged to another row
    public static final int CHECKABLE = 524288; // a toggle button or menu item with a check
    public static final int UNDERLINE = 1048576;
    public static final int HEADER_ROW = 2097152; // the section's first row stays beside its title
    public static final int RESET = 4194304; // the slider resets to its reset value, not the first value seen
    public static final int FLAT = 8388608; // a button framed only while hovered
    public static final int SEARCHABLE = 16777216; // typing selects matching table rows without filtering them
    public static final int INITIAL_FOCUS = 33554432;
    public static final int SELECTABLE = 67108864; // a tree row selected as a whole; a branch expands only by its handle
    public static final int POINT_SIZE = 268435456;
    public static final int CELL_EDITOR = 536870912;
    public static final int FILL = 1073741824; // the viewport takes its share of a resized window's height; a check takes clicks across its column

    private static final Kind[] KINDS = Kind.values();

    private byte[] bytes = new byte[1024];
    private int[] ints = new int[32 * INTS];
    private double[] doubles = new double[32 * DOUBLES];
    private char[] chars = new char[1024];
    private int[] spans = new int[128];
    // Each choice: text span, packed RGB, tip span. File filters use text only.
    private int[] choices = new int[96];
    // Each table column: fixed point width (0 automatic, -1 fixed sample), sample span, font flags.
    private int[] columns = new int[48];
    private int count;
    private int charCount;
    private int spanCount;
    private int choiceCount;
    private int byteCount;
    private int columnCount;
    private int doubleCount;
    private int owner;

    public Panel reset() {
        count = charCount = choiceCount = byteCount = columnCount = doubleCount = owner = 0;
        spanCount = 1; // span zero is always the empty string
        return this;
    }

    public Panel() {
        reset();
    }

    // Owner and control id are separate; asynchronous results retain both.
    public Panel owner(int value) {
        owner = value;
        return this;
    }

    // Persistent identity for a container, independent of its position in the snapshot.
    public Panel identity(int value) {
        if (value == 0)
            throw new IllegalArgumentException("Container identity must be nonzero");
        return field(IntField.ID, value);
    }

    private Panel add(Kind kind, int id, String label) {
        if ((count + 1) * INTS > ints.length) {
            ints = Arrays.copyOf(ints, 2 * ints.length);
        }
        Arrays.fill(ints, count * INTS, (count + 1) * INTS, 0);
        count++;
        field(IntField.KIND, kind.ordinal());
        field(IntField.ID, id);
        field(IntField.OWNER, owner);
        field(IntField.FLAGS, ENABLED);
        field(IntField.DOUBLE_INDEX, -1);
        return string(IntField.LABEL, label);
    }

    private Panel field(IntField field, int value) {
        ints[(count - 1) * INTS + field.ordinal()] = value;
        return this;
    }

    private Panel number(DoubleField field, double value) {
        int offset = (count - 1) * INTS + IntField.DOUBLE_INDEX.ordinal();
        int index = ints[offset];
        if (index == -1) {
            index = doubleCount++;
            if (doubleCount * DOUBLES > doubles.length)
                doubles = Arrays.copyOf(doubles, 2 * doubles.length);
            Arrays.fill(doubles, index * DOUBLES, (index + 1) * DOUBLES, 0);
            doubles[index * DOUBLES + DoubleField.SCALE.ordinal()] = DoubleField.SCALE.initial;
            ints[offset] = index;
        }
        doubles[index * DOUBLES + field.ordinal()] = value;
        return this;
    }

    private Panel flag(int flag) {
        ints[(count - 1) * INTS + IntField.FLAGS.ordinal()] |= flag;
        return this;
    }

    private Panel flag(int flag, boolean on) {
        return on ? flag(flag) : this;
    }

    private int append(String text) {
        if (text == null || text.isEmpty())
            return 0;
        int length = text.length();
        if (charCount + length > chars.length)
            chars = Arrays.copyOf(chars, Math.max(charCount + length, 2 * chars.length));
        if ((spanCount + 1) * SPAN_STRIDE > spans.length)
            spans = Arrays.copyOf(spans, 2 * spans.length);
        text.getChars(0, length, chars, charCount);
        spans[SPAN_STRIDE * spanCount] = charCount;
        spans[SPAN_STRIDE * spanCount + 1] = length;
        charCount += length;
        return spanCount++;
    }

    private Panel string(IntField field, String text) {
        return field(field, append(text));
    }

    private Panel payload(byte[] value) {
        if (byteCount + value.length > bytes.length)
            bytes = Arrays.copyOf(bytes, Math.max(byteCount + value.length, 2 * bytes.length));
        System.arraycopy(value, 0, bytes, byteCount, value.length);
        field(IntField.BYTES_START, byteCount).field(IntField.BYTES_COUNT, value.length);
        byteCount += value.length;
        return this;
    }

    private Panel choices(String[] labels, int[] colors) {
        if ((choiceCount + labels.length) * CHOICE_STRIDE > choices.length)
            choices = Arrays.copyOf(choices, Math.max((choiceCount + labels.length) * CHOICE_STRIDE, 2 * choices.length));
        field(IntField.CHOICES_START, choiceCount);
        field(IntField.CHOICES_COUNT, labels.length);
        for (int i = 0; i < labels.length; i++) {
            choices[CHOICE_STRIDE * choiceCount] = append(labels[i]);
            choices[CHOICE_STRIDE * choiceCount + 1] = colors == null ? 0 : colors[i];
            choices[CHOICE_STRIDE * choiceCount + 2] = 0;
            choiceCount++;
        }
        return this;
    }

    // Tooltips of the last control's choices.
    public Panel choiceTips(String[] tips) {
        int start = ints[(count - 1) * INTS + IntField.CHOICES_START.ordinal()];
        for (int i = 0; i < tips.length; i++)
            choices[CHOICE_STRIDE * (start + i) + 2] = append(tips[i]);
        return this;
    }

    private Panel values(double value, double min, double max, double step) {
        return number(DoubleField.VALUE, value).number(DoubleField.MIN, min).number(DoubleField.MAX, max).number(DoubleField.STEP, step);
    }

    // Leaves.

    public Panel label(String text) {
        return add(Kind.LABEL, 0, text);
    }

    // The label of a form row, against the control after it.
    public Panel caption(String text) {
        return label(text).right();
    }

    // A changing value, right aligned in the width of its widest text.
    public Panel readout(String text, String widest) {
        return label(text).right().widthText(widest);
    }

    // Widths are in text columns and heights in control rows.
    public Panel space(int columns, int rows) {
        return add(Kind.SPACE, 0, "").field(IntField.WIDTH, columns).field(IntField.HEIGHT, rows);
    }

    public Panel button(int id, String label) {
        return add(Kind.BUTTON, id, label);
    }

    public Panel toggleButton(int id, String label, boolean on) {
        return button(id, label).flag(CHECKABLE).flag(SELECTED, on);
    }

    // The referenced host panel opens at this button, with its content owned by Java.
    public Panel popupButton(int id, String label, int panel) {
        return button(id, label).field(IntField.POPUP, panel);
    }

    // A toolbar button: the icon over the caption, or the icon alone.
    public Panel tool(int id, String icon, String caption) {
        return add(Kind.TOOL, id, caption).string(IntField.ICON, icon);
    }

    public Panel check(int id, String label, boolean on) {
        return add(Kind.CHECK, id, label).flag(SELECTED, on);
    }

    // One button of a radio group; the group shares the id and reports the chosen index.
    public Panel radio(int id, String label, int choice, int selected) {
        return add(Kind.RADIO, id, label).field(IntField.CHOICE, choice).flag(SELECTED, choice == selected);
    }

    public Panel radio(int id, String[] labels, int selected) {
        for (int i = 0; i < labels.length; i++)
            radio(id, labels[i], i, selected);
        return this;
    }

    public Panel toggle(int id, String label, boolean on) {
        return add(Kind.TOGGLE, id, label).flag(SELECTED, on);
    }

    public Panel combo(int id, String[] choices, int index) {
        return add(Kind.COMBO, id, "").choices(choices, null).field(IntField.INDEX, index);
    }

    // A picker with a fixed caption: every pick reports, even of the current choice.
    public Panel comboButton(int id, String label, String[] choices, int index) {
        return add(Kind.COMBO, id, label).choices(choices, null).field(IntField.INDEX, index);
    }

    // Choices with a color swatch each, rgb packed as 0xRRGGBB.
    public Panel colors(int id, String[] names, int[] rgb, int index) {
        return add(Kind.COMBO, id, "").choices(names, rgb).field(IntField.INDEX, index);
    }

    // A row of color swatches, the chosen one outlined.
    public Panel swatches(int id, String[] names, int[] rgb, int selected) {
        for (int i = 0; i < names.length; i++)
            add(Kind.SWATCH, id, "").field(IntField.CHOICE, i).field(IntField.FOREGROUND, 0xFF000000 | rgb[i]).flag(SELECTED, i == selected).tip(names[i]);
        return this;
    }

    // Without format() the slider shows no value.
    public Panel slider(int id, double value, double min, double max) {
        return add(Kind.SLIDER, id, "").values(value, min, max, 0).field(IntField.DECIMALS, -1);
    }

    // The value a double-click restores.
    public Panel reset(double value) {
        return number(DoubleField.VALUE2, value).flag(RESET);
    }

    public Panel range(int id, double low, double high, double min, double max) {
        return add(Kind.RANGE, id, "").values(low, min, max, 0).number(DoubleField.VALUE2, high).field(IntField.DECIMALS, -1);
    }

    public Panel numberSpinner(int id, double value, double min, double max, double step) {
        return add(Kind.NUMBER_SPINNER, id, "").values(value, min, max, step);
    }

    // A spinner backed by a textual model, such as GOES class. Steps are commands to its Java owner.
    public Panel textSpinner(int id, String text, int columns) {
        return add(Kind.TEXT_SPINNER, id, "").string(IntField.TEXT, text).field(IntField.WIDTH, columns);
    }

    // The slider snaps to multiples of the step.
    public Panel step(double step) {
        return number(DoubleField.STEP, step);
    }

    // The value shows as value * scale with the decimals and the suffix.
    public Panel format(int decimals, double scale, String suffix) {
        return number(DoubleField.SCALE, scale).field(IntField.DECIMALS, decimals).string(IntField.TEXT, suffix);
    }

    public Panel text(int id, String text, int columns) {
        return add(Kind.TEXT, id, "").string(IntField.TEXT, text).field(IntField.WIDTH, columns);
    }

    // Swing TimeField: one text editor with a trailing calendar button, two typed callbacks.
    public Panel timeField(int id, int dateId, String text, int columns, long millis, long min, long max) {
        return add(Kind.TIME_FIELD, id, "").field(IntField.SECONDARY_ID, dateId).string(IntField.TEXT, text)
                .field(IntField.WIDTH, columns).values(millis, min, max, 0);
    }

    public Panel splitButton(int id, int choiceId, String icon, String[] labels, int selected) {
        return add(Kind.SPLIT_BUTTON, id, "").field(IntField.SECONDARY_ID, choiceId).string(IntField.ICON, icon)
                .choices(labels, null).field(IntField.INDEX, selected);
    }

    public Panel textArea(String text, int columns, int rows) {
        return add(Kind.TEXT_AREA, 0, "").string(IntField.TEXT, text).field(IntField.WIDTH, columns).field(IntField.HEIGHT, rows);
    }

    public Panel link(String url) {
        return link(url, url);
    }

    public Panel link(String label, String url) {
        return add(Kind.LINK, 0, label).string(IntField.TEXT, url);
    }

    public Panel link(int id, String label) {
        return add(Kind.LINK, id, label);
    }

    // Each image owns a slice of the bulk payload, width in points.
    public Panel image(byte[] png, int width) {
        return add(Kind.IMAGE, 0, "").field(IntField.WIDTH, width).payload(png);
    }

    public Panel imageIcon(byte[] png, int points) {
        return payload(png).field(IntField.ICON_SIZE, points);
    }

    public Panel spinner() {
        return add(Kind.SPINNER, 0, "");
    }

    public Panel messageIcon(Notice type) {
        return add(Kind.MESSAGE_ICON, 0, "").field(IntField.INDEX, type.ordinal()).sizePoints(32, 32);
    }

    // Download progress replaces the glyph inside the same toggle.
    public Panel progressToggle(int id, int percent) {
        return add(Kind.PROGRESS_TOGGLE, id, "").flag(SELECTED).number(DoubleField.VALUE, percent);
    }

    public Panel separator() {
        return add(Kind.SEPARATOR, 0, "");
    }

    public Panel timeSlider(int id, int frame, int first, int last, int maximum, byte[] completion) {
        return add(Kind.TIME_SLIDER, id, "").field(IntField.INDEX, frame).values(0, first, last, 0)
                .field(IntField.LAST_FRAME, maximum).payload(completion);
    }

    // A selectable row of a tree.
    public Panel item(int id, String label, boolean selected) {
        return add(Kind.ITEM, id, label).flag(SELECTED, selected);
    }

    public Panel menuItem(int id, String label) {
        return add(Kind.MENU_ITEM, id, label);
    }

    public Panel menuCheck(int id, String label, boolean on) {
        return menuItem(id, label).flag(CHECKABLE).flag(SELECTED, on);
    }

    public Panel shortcut(Key key, int modifiers) {
        return field(IntField.KEY, key.ordinal()).field(IntField.MODIFIERS, modifiers);
    }

    // Containers.

    public Panel row() {
        return add(Kind.ROW, 0, "");
    }

    // A table row, selected as a whole; its cells follow.
    public Panel tableRow(int id, boolean selected) {
        return add(Kind.ROW, id, "").flag(SELECTED, selected);
    }

    public Panel rowEnd() {
        return add(Kind.ROW_END, 0, "");
    }

    // The row reports Command.ENTER and Command.EXIT with this item as the pointer crosses it.
    public Panel hover(int id) {
        return field(IntField.SECONDARY_ID, id);
    }

    public Panel column() {
        return add(Kind.COLUMN, 0, "");
    }

    public Panel columnEnd() {
        return add(Kind.COLUMN_END, 0, "");
    }

    // Cells by rows; each row ends with endRow() on its last cell or after the column count.
    public Panel grid(int columns) {
        return add(Kind.GRID, 0, "").field(IntField.COLUMNS, columns);
    }

    // The column that takes the width left by the others; its cells' first control fills it.
    public Panel stretch(int column) {
        return field(IntField.STRETCH, column + 1);
    }

    // Labels with their controls filling the rest.
    public Panel form() {
        return grid(2).stretch(1);
    }

    public Panel gridEnd() {
        return add(Kind.GRID_END, 0, "");
    }

    // Rows of tableRow(); the name column stretches; scrolls after maxRows rows.
    public Panel table(int columns, int nameColumn, int maxRows) {
        if ((columnCount + columns) * COLUMN_STRIDE > this.columns.length)
            this.columns = Arrays.copyOf(this.columns, Math.max((columnCount + columns) * COLUMN_STRIDE, 2 * this.columns.length));
        int start = columnCount;
        columnCount += columns;
        Arrays.fill(this.columns, start * COLUMN_STRIDE, columnCount * COLUMN_STRIDE, 0);
        return add(Kind.TABLE, 0, "").field(IntField.COLUMNS, columns).field(IntField.COLUMN_START, start)
                .field(IntField.STRETCH, nameColumn + 1).field(IntField.MAX_ROWS, maxRows).field(IntField.SEARCH_COLUMNS, 1)
                .stretchColumns(nameColumn).activateColumns(nameColumn).doubleClickColumns(nameColumn);
    }

    public Panel columnWidth(int column, int points) {
        int start = ints[(count - 1) * INTS + IntField.COLUMN_START.ordinal()];
        columns[COLUMN_STRIDE * (start + column)] = points;
        return this;
    }

    public Panel columnSample(int column, String text, int fontFlags, boolean fixed) {
        int start = ints[(count - 1) * INTS + IntField.COLUMN_START.ordinal()];
        columns[COLUMN_STRIDE * (start + column)] = fixed ? -1 : 0;
        columns[COLUMN_STRIDE * (start + column) + 1] = append(text);
        columns[COLUMN_STRIDE * (start + column) + 2] = fontFlags;
        return this;
    }

    public Panel copyColumns(int... columns) {
        return field(IntField.COPY_COLUMNS, columnMask(columns));
    }

    public Panel stretchColumns(int... columns) {
        return field(IntField.FLEX_COLUMNS, columnMask(columns));
    }

    public Panel searchColumns(int... columns) {
        return field(IntField.SEARCH_COLUMNS, columnMask(columns));
    }

    public Panel rowHeight(int points) {
        return field(IntField.ROW_HEIGHT, points);
    }

    private static int columnMask(int[] columns) {
        int mask = 0;
        for (int column : columns) {
            if (column < 0 || column >= Integer.SIZE)
                throw new IllegalArgumentException("Column mask index " + column);
            mask |= 1 << column;
        }
        return mask;
    }

    public Panel activateColumns(int... columns) {
        return field(IntField.ACTIVATE_COLUMNS, columnMask(columns));
    }

    public Panel doubleClickColumns(int... columns) {
        return field(IntField.DOUBLE_CLICK_COLUMNS, columnMask(columns));
    }

    public Panel searchable() {
        return flag(SEARCHABLE);
    }

    public Panel initialFocus() {
        return flag(INITIAL_FOCUS);
    }

    public Panel tableEnd() {
        return add(Kind.TABLE_END, 0, "");
    }

    public Panel section(String title, boolean open) {
        return add(Kind.SECTION, 0, title).flag(OPEN, open);
    }

    // A section whose state the screen owns: each toggle reports checked() with the new state.
    public Panel section(int id, String title, boolean open) {
        return section(title, open).identity(id).field(IntField.SECONDARY_ID, id);
    }

    // The first row of this section stays beside its title, even when collapsed.
    public Panel headerRow() {
        return flag(HEADER_ROW);
    }

    public Panel alignWithGrid(int gridId, int column) {
        return field(IntField.ALIGN_GRID, gridId).field(IntField.ALIGN_COLUMN, column);
    }

    public Panel columnsFrom(int gridId) {
        return field(IntField.ALIGN_GRID, gridId);
    }

    public Panel sectionEnd() {
        return add(Kind.SECTION_END, 0, "");
    }

    public Panel scroll(int rows) {
        return add(Kind.SCROLL, 0, "").field(IntField.HEIGHT, rows);
    }

    public Panel scrollEnd() {
        return add(Kind.SCROLL_END, 0, "");
    }

    // Vertical split pane. Children declare their initial point heights and retain independent scrolling.
    public Panel split(int id) {
        return add(Kind.SPLIT, id, "");
    }

    public Panel splitEnd() {
        return add(Kind.SPLIT_END, 0, "");
    }

    // A button that pops up the controls up to menuEnd(); the menu bar draws its menus itself.
    public Panel menu(String title) {
        return add(Kind.MENU, 0, title);
    }

    public Panel menuEnd() {
        return add(Kind.MENU_END, 0, "");
    }

    // A collapsible branch of items and branches up to treeEnd().
    public Panel tree(int id, String label, boolean open) {
        return add(Kind.TREE, id, label).flag(OPEN, open);
    }

    public Panel treeEnd() {
        return add(Kind.TREE_END, 0, "");
    }

    // Modifiers of the last control.

    public Panel tip(String tip) {
        return string(IntField.TIP, tip);
    }

    public Panel icon(String icon) {
        return string(IntField.ICON, icon);
    }

    public Panel icon(String icon, int points) {
        return string(IntField.ICON, icon).field(IntField.ICON_SIZE, points);
    }

    public Panel endRow() {
        return flag(END_ROW);
    }

    // The cell takes this many grid columns.
    public Panel cells(int columns) {
        return field(IntField.CELL_SPAN, columns);
    }

    public Panel enabled(boolean enabled) {
        if (!enabled)
            ints[(count - 1) * INTS + IntField.FLAGS.ordinal()] &= ~ENABLED;
        return this;
    }

    public Panel selected(boolean selected) {
        return flag(SELECTED, selected);
    }

    public Panel strong() {
        return flag(STRONG);
    }

    public Panel underline() {
        return flag(UNDERLINE);
    }

    public Panel mono() {
        return flag(MONO);
    }

    public Panel small() {
        return flag(SMALL);
    }

    public Panel large() {
        return field(IntField.LARGE, 1);
    }

    public Panel wrap() {
        return flag(WRAP);
    }

    public Panel right() {
        return flag(RIGHT);
    }

    public Panel tight() {
        return flag(TIGHT);
    }

    // Points between the controls of a row, around them too as FlowLayout with margin().
    public Panel gap(int points) {
        return field(IntField.GAP, points);
    }

    // Points kept clear inside a row, column or grid.
    public Panel margin(int top, int left, int bottom, int right) {
        return field(IntField.MARGIN, top << 24 | left << 16 | bottom << 8 | right);
    }

    // The height of a row in control rows, for a control centered against a taller neighbor.
    public Panel height(int rows) {
        return field(IntField.HEIGHT, rows);
    }

    public Panel selectable() {
        return flag(SELECTABLE);
    }

    public Panel center() {
        return flag(CENTER);
    }

    public Panel width(int columns) {
        return field(IntField.WIDTH, columns);
    }

    public Panel widthText(String text) {
        return string(IntField.WIDTH_TEXT, text);
    }

    public Panel description(String text) {
        return string(IntField.DESCRIPTION, text);
    }

    public Panel cellEditor() {
        return flag(CELL_EDITOR);
    }

    // Explicit Swing logical dimensions, rather than text columns and control rows.
    public Panel sizePoints(int width, int height) {
        return field(IntField.WIDTH, width).field(IntField.HEIGHT, height).flag(POINT_SIZE);
    }

    public Panel foreground(int rgb) {
        return field(IntField.FOREGROUND, 0xFF000000 | rgb); // zero means the default foreground
    }

    public Panel defaultButton() {
        return flag(DEFAULT);
    }

    public Panel flat() {
        return flag(FLAT);
    }

    public Panel repeat(int milliseconds) {
        return number(DoubleField.REPEAT, milliseconds / 1000.).flag(REPEAT);
    }

    public Panel live() {
        return flag(LIVE);
    }

    public Panel password() {
        return flag(PASSWORD);
    }

    public Panel readOnly(boolean readOnly) {
        return flag(READ_ONLY, readOnly);
    }

    public Panel activateOnPress() {
        return flag(ACTIVATE);
    }

    public Panel draggable() {
        return flag(DRAGGABLE);
    }

    public Panel fill() {
        return flag(FILL);
    }

    // A table header button: this column leads the rows' order, INDEX holding 1 ascending or 2 descending.
    public Panel sorted(boolean descending) {
        return field(IntField.INDEX, descending ? 2 : 1);
    }

    // A window fits this snapshot again whenever the revision changes. Stored on the first control; zero keeps the window's size.
    public Panel fit(int revision) {
        ints[IntField.FIT.ordinal()] = revision;
        return this;
    }

    public Panel multiple(boolean multiple) {
        return flag(MULTIPLE, multiple);
    }

    private Panel act(Act act) {
        return field(IntField.ACTION, act.ordinal());
    }

    public Panel file(FileFilter filter, String directory, boolean multiple) {
        act(Act.FILE).string(IntField.DIRECTORY, directory);
        if (filter != null)
            string(IntField.FILTER, filter.name).choices(filter.extensions, null);
        return flag(MULTIPLE, multiple);
    }

    public Panel save(FileFilter filter, String name, String directory) {
        return file(filter, directory, false).act(Act.SAVE).string(IntField.NAME, name);
    }

    public Panel paste() {
        return act(Act.PASTE);
    }

    public Panel url(String url) {
        return act(Act.URL).tip(url);
    }

    // Read access for adapters in the JVM.

    public int size() {
        return count;
    }

    public Kind kind(int control) {
        return KINDS[value(control, IntField.KIND)];
    }

    public int value(int control, IntField field) {
        return ints[control * INTS + field.ordinal()];
    }

    public double value(int control, DoubleField field) {
        int index = value(control, IntField.DOUBLE_INDEX);
        return index < 0 ? field.initial : doubles[index * DOUBLES + field.ordinal()];
    }

    public boolean has(int control, int flag) {
        return (value(control, IntField.FLAGS) & flag) != 0;
    }

    public String string(int control, IntField field) {
        return span(value(control, field));
    }

    private String span(int span) {
        return new String(chars, spans[SPAN_STRIDE * span], spans[SPAN_STRIDE * span + 1]);
    }

    // The declared point width of a table column.
    public int columnPoints(int control, int column) {
        return columns[COLUMN_STRIDE * (value(control, IntField.COLUMN_START) + column)];
    }

    // The control closing the table at this control.
    public int tableEnd(int control) {
        int end = control;
        while (kind(end) != Kind.TABLE_END)
            end++;
        return end;
    }

    // The control closing the scroll at this control.
    public int scrollEnd(int control) {
        int end = control;
        while (kind(end) != Kind.SCROLL_END)
            end++;
        return end;
    }

    public String[] choices(int control) {
        int start = value(control, IntField.CHOICES_START);
        String[] labels = new String[value(control, IntField.CHOICES_COUNT)];
        for (int i = 0; i < labels.length; i++)
            labels[i] = span(choices[CHOICE_STRIDE * (start + i)]);
        return labels;
    }

    // The tooltip of each choice.
    public String[] choiceTips(int control) {
        int start = value(control, IntField.CHOICES_START);
        String[] tips = new String[value(control, IntField.CHOICES_COUNT)];
        for (int i = 0; i < tips.length; i++)
            tips[i] = span(choices[CHOICE_STRIDE * (start + i) + 2]);
        return tips;
    }

    public byte[] payload(int control) {
        int start = value(control, IntField.BYTES_START);
        return Arrays.copyOfRange(bytes, start, start + value(control, IntField.BYTES_COUNT));
    }

    // The arrays of this snapshot, for a reader that does not go control by control. Counts are entries; capacities can exceed them.
    @FunctionalInterface
    public interface Storage<T> {
        T read(int count, int[] ints, int doubleCount, double[] doubles, int charCount, char[] chars, int spanCount, int[] spans,
                  int choiceCount, int[] choices, int byteCount, byte[] bytes, int columnCount, int[] columns);
    }

    public <T> T read(Storage<T> storage) {
        return storage.read(count, ints, doubleCount, doubles, charCount, chars, spanCount, spans, choiceCount, choices, byteCount, bytes, columnCount, columns);
    }

}
