package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.helioviewer.jhv.gui.components.base.TableValue;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;

// This component allows to select a date. There are 3 different views:
// 1. All days of a month are displayed corresponding to the weekdays,
// 2. All month of a year are displayed,
// 3. A period of 12 years is displayed.
@SuppressWarnings("serial")
class CalendarPicker extends JPanel {

    private enum DisplayMode {
        DAYS, MONTHS, YEARS
    }

    private final ArrayList<CalendarListener> listeners = new ArrayList<>();
    private final NavigationPanel navigationPanel = new NavigationPanel();
    private final SelectionPanel selectionPanel = new SelectionPanel();

    private DisplayMode displayMode = DisplayMode.DAYS;
    private CalendarViewController calendarViewController = new DayViewController();

    CalendarPicker() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(250, 200));
        // add sub components
        add(navigationPanel, BorderLayout.PAGE_START);
        add(selectionPanel, BorderLayout.CENTER);
        add(new BottomPanel(), BorderLayout.PAGE_END);
        updateDateDisplay();
    }

    void resizeSelectionPanel() {
        selectionPanel.resizeTableSpace();
        selectionPanel.resizeTableRowHeight();
        selectionPanel.resizeTableColumnWidth();
    }

    // Changes the view and the corresponding controller
    private void changeDisplayMode(DisplayMode newMode) {
        // memorize the selected time
        long time = calendarViewController.getTime();
        // change the view controller
        calendarViewController = switch (newMode) {
            case DAYS -> new DayViewController();
            case MONTHS -> new MonthViewController();
            case YEARS -> new YearViewController();
        };
        // set memorized time
        calendarViewController.setTime(time);
        // memorize current view mode
        displayMode = newMode;
    }

    // Updates the data which has to be displayed by the visual components
    private void updateDateDisplay() {
        // fill grid with data
        selectionPanel.fillGrid(calendarViewController.getGridData(), calendarViewController.getGridColumnHeader(), calendarViewController.getCorrespondingCellOfCurrentTime(), displayMode == DisplayMode.DAYS);
        // enable or disable buttons
        navigationPanel.updateButtonsVisibility();
        // refresh button text
        navigationPanel.setSelectButtonText(calendarViewController.getSelectionButtonText());
    }

    // Sets the current time to the calendar component
    void setTime(long milli) {
        calendarViewController.setTime(milli);
        updateDateDisplay();
    }

    long getTime() {
        return calendarViewController.getTime();
    }

    void addListener(CalendarListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    // Informs all listeners of this class by passing the corresponding event
    private void informCalendarListeners() {
        listeners.forEach(CalendarListener::calendarAction);
    }

    // Panel which acts as a container for the navigation buttons on the top of
    // the calendar component
    private class NavigationPanel extends JPanel implements ActionListener {

        private final JideButton quickForwardButton = new JideButton(">>");
        private final JideButton quickBackButton = new JideButton("<<");
        private final JideButton forwardButton = new JideButton(">");
        private final JideButton backButton = new JideButton("<");
        private final JideButton selectButton = new JideButton();

        NavigationPanel() {
            setLayout(new BorderLayout());

            JPanel forwardButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 2));
            forwardButtonPanel.add(forwardButton);
            forwardButtonPanel.add(quickForwardButton);

            JPanel selectionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
            selectionButtonPanel.add(selectButton);

            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 2, 2));
            backButtonPanel.add(quickBackButton);
            backButtonPanel.add(backButton);

            add(forwardButtonPanel, BorderLayout.LINE_END);
            add(selectionButtonPanel, BorderLayout.CENTER);
            add(backButtonPanel, BorderLayout.LINE_START);

            quickForwardButton.addActionListener(this);
            quickBackButton.addActionListener(this);
            forwardButton.addActionListener(this);
            backButton.addActionListener(this);
            selectButton.addActionListener(this);
        }

        // Sets the text of the button which changes the view
        void setSelectButtonText(String text) {
            selectButton.setText(text);
        }

        // Action event for all the buttons of this component
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == forwardButton) {
                // depending on the current view controller the next date period will be shown
                calendarViewController.moveForward();
                updateDateDisplay();
            } else if (e.getSource() == backButton) {
                // depending on the current view controller the previous date period will be shown
                calendarViewController.moveBack();
                updateDateDisplay();
            } else if (e.getSource() == selectButton) {
                // change the view mode and the corresponding controller
                switch (displayMode) {
                    case DAYS -> {
                        changeDisplayMode(DisplayMode.MONTHS);
                        updateDateDisplay();
                    }
                    case MONTHS -> {
                        changeDisplayMode(DisplayMode.YEARS);
                        updateDateDisplay();
                    }
                    default -> {
                    }
                }
            } else if (e.getSource() == quickForwardButton) {
                // increase current date by using the view controller of the next higher period control
                CalendarViewController cvc = switch (displayMode) {
                    case DAYS -> new MonthViewController();
                    case MONTHS -> new YearViewController();
                    default -> null;
                };

                if (cvc != null) {
                    cvc.setTime(calendarViewController.getTime());
                    calendarViewController.setTime(cvc.moveForward());
                    updateDateDisplay();
                }
            } else if (e.getSource() == quickBackButton) {
                // reduce current date by using the view controller of the next higher period control
                CalendarViewController cvc = switch (displayMode) {
                    case DAYS -> new MonthViewController();
                    case MONTHS -> new YearViewController();
                    default -> null;
                };

                if (cvc != null) {
                    cvc.setTime(calendarViewController.getTime());
                    calendarViewController.setTime(cvc.moveBack());
                    updateDateDisplay();
                }
            }
        }

        // Sets the quick forward button and the quick back button visible or
        // not depending on the current display mode.
        void updateButtonsVisibility() {
            quickForwardButton.setVisible(displayMode != DisplayMode.YEARS);
            quickBackButton.setVisible(displayMode != DisplayMode.YEARS);
        }

    }

    // Panel which acts as a container of the grid which displays the period of the current view controller
    private class SelectionPanel extends JPanel {

        final JTable table;
        private final JPanel contentPane;

        SelectionPanel() {
            setLayout(new BorderLayout());

            table = new JTable();
            // allow for individual cell selection and turn off grid lines
            table.setCellSelectionEnabled(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setShowGrid(false);
            table.setRequestFocusEnabled(false); // allow calendarButton to keep focus
            // avoid reordering and resizing of columns
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setResizingAllowed(false);

            table.addMouseListener(new MouseAdapter() {
                // when a cell which contains valid data was clicked, the view controller and view mode will be changed
                @Override
                public void mouseClicked(MouseEvent e) {
                    TableValue v = TableValue.tableValueAtPoint(table, e.getPoint());
                    if (v == null || v.value == null)
                        return;
                    calendarViewController.setTimeOfCellValue(v.value);

                    switch (displayMode) {
                        case YEARS -> {
                            changeDisplayMode(DisplayMode.MONTHS);
                            updateDateDisplay();
                        }
                        case MONTHS -> {
                            changeDisplayMode(DisplayMode.DAYS);
                            updateDateDisplay();
                        }
                        case DAYS -> informCalendarListeners();
                    }
                }
            });

            // place table on form
            JPanel headerPane = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 0));
            headerPane.add(table.getTableHeader());

            JPanel tablePane = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 0));
            tablePane.add(table);

            contentPane = new JPanel();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

            contentPane.add(headerPane);
            contentPane.add(tablePane);
            add(contentPane);
        }

        /**
         * Displays the passed data in the grid.
         *
         * @param data         Data to display in the table. If this parameter is null
         *                     the method will do nothing.
         * @param columnNames  Column names which will be displayed in the header, too.
         *                     If this parameter is null the method will do nothing.
         * @param selectedCell Defines the cell which has to be selected.
         * @param showHeader   true if the header should be displayed; false if not.
         */
        void fillGrid(Object[][] data, String[] columnNames, Point selectedCell, boolean showHeader) {
            // check if valid data is available
            if (data == null || columnNames == null)
                return;
            // change model of table
            table.setModel(new DefaultTableModel(data, columnNames));
            // set header visible or not
            table.getTableHeader().setVisible(showHeader);
            // add a cell renderer to all cells which shows cell content centered
            TableCellRenderer cellRenderer = new CenterTableCellRenderer();
            int columnCount = table.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
            }

            // set the size of rows and columns
            resizeTableRowHeight();
            resizeTableColumnWidth();

            // select cell
            if (selectedCell != null)
                table.changeSelection(selectedCell.x, selectedCell.y, false, false);
        }

        // Sets the size of the table to the size of the available space
        void resizeTableSpace() {
            table.setSize(new Dimension(contentPane.getWidth() - 4, contentPane.getHeight()));
            table.setPreferredSize(new Dimension(contentPane.getWidth() - 4, contentPane.getHeight()));
        }

        // Computes the height of the rows, so they will fit to the whole height of the table
        void resizeTableRowHeight() {
            int headerHeight = 0;
            if (displayMode == DisplayMode.DAYS)
                headerHeight = table.getTableHeader().getHeight();

            int rowCount = table.getRowCount();
            int rowHeight;
            if (rowCount > 0 && (rowHeight = (table.getHeight() - headerHeight) / rowCount) > 0)
                table.setRowHeight(rowHeight);
        }

        // Sets the size of the columns so all columns will have the same size
        // and all columns will fit into the space of the table component.
        void resizeTableColumnWidth() {
            JTableHeader tableHeader = table.getTableHeader();
            int columnCount = tableHeader.getColumnModel().getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                tableHeader.getColumnModel().getColumn(i).setWidth(table.getColumnModel().getColumn(i).getWidth());
            }
        }

    }

    // Table cell renderer for the used JTable. This renderer displays all cell entries centered.
    private static class CenterTableCellRenderer extends DefaultTableCellRenderer {

        CenterTableCellRenderer() {
            setHorizontalAlignment(CENTER);
        }

    }

    // Panel which acts as a container of the button which displays the current
    // date and allows to set the current date to the calendar component.
    private class BottomPanel extends JPanel {

        BottomPanel() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));

            JideButton dateButton = new JideButton("Today is " + TimeUtils.formatDate(System.currentTimeMillis()));
            // set the calendar component to the current date
            dateButton.addActionListener(e -> {
                changeDisplayMode(DisplayMode.DAYS);
                setTime(System.currentTimeMillis());
            });
            add(dateButton);
        }

    }

}
