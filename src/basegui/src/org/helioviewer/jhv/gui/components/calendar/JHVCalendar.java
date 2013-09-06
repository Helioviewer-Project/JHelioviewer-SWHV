package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * This component allows to select a date. There are 3 different views:<br>
 * 1. All days of a month are displayed corresponding to the weekdays,<br>
 * 2. All month of a year are displayed,<br>
 * 3. A period of 12 years is displayed.
 * <p>
 * To use this component create an instance of the class, set the default date
 * format by calling the method {@link #setDateFormat(String)} to display the
 * date at the bottom like the user prefers and set the date which should be
 * selected by calling the method {@link #setDate(Date)}. When the user has
 * selected a date a {@link JHVCalendarEvent} will be fired. To get the selected
 * date call {@link #getDate()}.
 * <p>
 * This component acts as a subcomponent of the {@link JHVCalendarDatePicker}
 * too. It represents the content of the popup window of the
 * JHVCalendarDatePicker.
 * 
 * @author Stephan Pagel
 */
public class JHVCalendar extends JPanel implements ComponentListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private enum DisplayMode {
        DAYS, MONTHS, YEARS
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    private DisplayMode displayMode;
    private CalendarViewController calendarViewController = null;
    private AbstractList<JHVCalendarListener> listeners = new LinkedList<JHVCalendarListener>();

    private NavigationPanel navigationPanel = new NavigationPanel();
    private SelectionPanel selectionPanel = new SelectionPanel();
    private BottomPanel bottomPanel = new BottomPanel();

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     */
    public JHVCalendar() {

        this(true);
    }

    /**
     * Constructor where to choose if the current date should be displayed at
     * the bottom or not.
     * 
     * @param showToday
     *            True if the date of the current date should be displayed at
     *            the bottom; false if not.
     */
    public JHVCalendar(boolean showToday) {

        // load day selection view
        changeDisplayMode(DisplayMode.DAYS);

        // initialize visual components
        initVisualComponents(showToday);
    }

    /**
     * Initialize the visual parts of the component.
     * 
     * @param showToday
     *            True if the date of the current date should be displayed at
     *            the bottom; false if not.
     */
    private void initVisualComponents(boolean showToday) {

        // set basic layout
        setLayout(new BorderLayout());

        // add listener
        addComponentListener(this);

        setMinimumSize(new Dimension(250, 200));

        // add sub components
        add(navigationPanel, BorderLayout.NORTH);
        add(selectionPanel, BorderLayout.CENTER);

        if (showToday)
            add(bottomPanel, BorderLayout.SOUTH);

        // show data in visual components
        updateDateDisplay();
    }

    /**
     * Changes the view and the corresponding controller.
     * 
     * @param newMode
     *            Defines which view has to be displayed.
     */
    private void changeDisplayMode(DisplayMode newMode) {

        // memorize the selected date if a date is available
        // at the current view controller
        Date date = null;

        if (calendarViewController != null)
            date = calendarViewController.getDate();

        // change the view controller
        switch (newMode) {
        case DAYS:
            calendarViewController = new DayViewController();
            break;
        case MONTHS:
            calendarViewController = new MonthViewController();
            break;
        case YEARS:
            calendarViewController = new YearViewController();
            break;
        }

        // set memorized date if available
        if (date != null)
            calendarViewController.setDate(date);

        // memorize current view mode
        displayMode = newMode;
    }

    /**
     * Updates the data which has to be displayed at the visual components.
     */
    private void updateDateDisplay() {

        // fill grid with data
        selectionPanel.fillGrid(calendarViewController.getGridData(), calendarViewController.getGridColumnHeader(), calendarViewController.getCorrespondingCellOfCurrentDate(), displayMode == DisplayMode.DAYS);

        // enable or disable buttons
        navigationPanel.updateButtonsVisibility();

        // refresh button text
        navigationPanel.setSelectButtonText(calendarViewController.getSelectionButtonText());
    }

    /**
     * Set the date pattern. The date will be displayed in defined format.
     * 
     * @param pattern
     *            pattern how date should be displayed.
     * @return boolean value if pattern is valid.
     */
    public boolean setDateFormat(String pattern) {

        try {
            dateFormat.applyPattern(pattern);
            return true;

        } catch (NullPointerException e1) {
        } catch (IllegalArgumentException e2) {
        }

        return false;
    }

    /**
     * Sets the current date to the calendar component.
     * 
     * @param date
     *            Selected date of the calendar component.
     */
    public void setDate(Date date) {

        // set date
        calendarViewController.setDate(date);

        // update visual components
        updateDateDisplay();
    }

    /**
     * Returns the selected date of the calendar component.
     * 
     * @return selected date.
     */
    public Date getDate() {
        return calendarViewController.getDate();
    }

    /**
     * Adds a listener which will be informed when a date has been selected.
     * 
     * @param l
     *            listener which has to be informed.
     */
    public void addJHVCalendarListener(JHVCalendarListener l) {

        if (l != null)
            listeners.add(l);
    }

    /**
     * Removes a listener which should not be informed anymore when a date has
     * been selected.
     * 
     * @param l
     *            listener which should not be informed anymore.
     */
    public void removeJHVCalendarListener(JHVCalendarListener l) {

        if (l != null)
            listeners.remove(l);
    }

    /**
     * Informs all listener of this class by passing the corresponding event.
     * 
     * @param e
     *            event
     */
    private void informAllJHVCalendarListeners(JHVCalendarEvent e) {

        for (JHVCalendarListener l : listeners) {
            l.actionPerformed(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void componentHidden(ComponentEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void componentMoved(ComponentEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void componentResized(ComponentEvent arg0) {

        if (selectionPanel != null) {
            selectionPanel.resizeTableSpace();
            selectionPanel.resizeTableRowHeight();
            selectionPanel.resizeTableColumnWidth();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void componentShown(ComponentEvent arg0) {
    }

    /**
     * Panel which acts as a container for the navigation buttons on the top of
     * the calendar component.
     * 
     * @author Stephan Pagel
     */
    private class NavigationPanel extends JPanel implements ActionListener {

        // ////////////////////////////////////////////////////////////
        // Definitions
        // ////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        private JButton quickForwardButton = new JButton(IconBank.getIcon(JHVIcon.SIMPLE_DOUBLEARROW_RIGHT));
        private JButton quickBackButton = new JButton(IconBank.getIcon(JHVIcon.SIMPLE_DOUBLEARROW_LEFT));
        private JButton forwardButton = new JButton(IconBank.getIcon(JHVIcon.SIMPLE_ARROW_RIGHT));
        private JButton backButton = new JButton(IconBank.getIcon(JHVIcon.SIMPLE_ARROW_LEFT));
        private JButton selectButton = new JButton("");

        // ////////////////////////////////////////////////////////////
        // Methods
        // ////////////////////////////////////////////////////////////

        /**
         * Default constructor.
         */
        public NavigationPanel() {

            initVisualComponents();
            addActionListeners();
        }

        /**
         * Initialize the visual parts of the component.
         */
        private void initVisualComponents() {

            // set basic layout
            setLayout(new BorderLayout());

            quickForwardButton.setPreferredSize(new Dimension(24, quickForwardButton.getPreferredSize().height));
            quickBackButton.setPreferredSize(new Dimension(24, quickBackButton.getPreferredSize().height));

            forwardButton.setPreferredSize(new Dimension(24, forwardButton.getPreferredSize().height));
            backButton.setPreferredSize(new Dimension(24, backButton.getPreferredSize().height));

            // add navigation buttons
            JPanel forwardButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            forwardButtonPanel.add(forwardButton);
            forwardButtonPanel.add(quickForwardButton);

            JPanel selectionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
            selectionButtonPanel.add(selectButton);

            JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
            backButtonPanel.add(quickBackButton);
            backButtonPanel.add(backButton);

            add(forwardButtonPanel, BorderLayout.EAST);
            add(selectionButtonPanel, BorderLayout.CENTER);
            add(backButtonPanel, BorderLayout.WEST);
        }

        /**
         * Sets the needed action listeners to the visual components.
         */
        private void addActionListeners() {

            quickForwardButton.addActionListener(this);
            forwardButton.addActionListener(this);
            backButton.addActionListener(this);
            quickBackButton.addActionListener(this);
            selectButton.addActionListener(this);
        }

        /**
         * Sets the text of the button which changes the view.
         * 
         * @param text
         *            text to display on the button.
         */
        public void setSelectButtonText(String text) {

            selectButton.setText(text);
        }

        /**
         * Action event for all the buttons of this component.
         */
        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == forwardButton) {

                // depending on the current view controller the next date period
                // will be shown
                calendarViewController.moveForward();
                updateDateDisplay();

            } else if (e.getSource() == backButton) {

                // depending on the current view controller the previous date
                // period
                // will be shown
                calendarViewController.moveBack();
                updateDateDisplay();

            } else if (e.getSource() == selectButton) {

                // change the view mode and the corresponding controller
                switch (displayMode) {
                case DAYS:
                    changeDisplayMode(DisplayMode.MONTHS);
                    updateDateDisplay();
                    break;
                case MONTHS:
                    changeDisplayMode(DisplayMode.YEARS);
                    updateDateDisplay();
                    break;
                }

            } else if (e.getSource() == quickForwardButton) {

                // increase current date by using the view controller of the
                // next higher period control.
                CalendarViewController cvc = null;

                if (displayMode == DisplayMode.DAYS) {
                    cvc = new MonthViewController();
                } else if (displayMode == DisplayMode.MONTHS) {
                    cvc = new YearViewController();
                }

                if (cvc != null) {
                    cvc.setDate(calendarViewController.getDate());
                    calendarViewController.setDate(cvc.moveForward());
                    updateDateDisplay();
                }

            } else if (e.getSource() == quickBackButton) {

                // reduce current date by using the view controller of the next
                // higher period control.
                CalendarViewController cvc = null;

                if (displayMode == DisplayMode.DAYS) {
                    cvc = new MonthViewController();
                } else if (displayMode == DisplayMode.MONTHS) {
                    cvc = new YearViewController();
                }

                if (cvc != null) {
                    cvc.setDate(calendarViewController.getDate());
                    calendarViewController.setDate(cvc.moveBack());
                    updateDateDisplay();
                }
            }
        }

        /**
         * Sets the quick forward button and the quick back button visible or
         * not depending on the current display mode.
         */
        public void updateButtonsVisibility() {

            quickForwardButton.setVisible(displayMode != DisplayMode.YEARS);
            quickBackButton.setVisible(displayMode != DisplayMode.YEARS);
        }
    }

    /**
     * Panel which acts as a container of the grid which displays the period of
     * the current view controller.
     * 
     * @author Stephan Pagel
     */
    private class SelectionPanel extends JPanel implements MouseListener {

        // ////////////////////////////////////////////////////////////
        // Definitions
        // ////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        private JTable table;
        private JPanel contentPane;

        // ////////////////////////////////////////////////////////////
        // Methods
        // ////////////////////////////////////////////////////////////

        /**
         * Default constructor.
         */
        public SelectionPanel() {

            initVisualComponents();
        }

        /**
         * Initialize the visual parts of the component.
         */
        private void initVisualComponents() {

            setLayout(new GridLayout(1, 1));

            // create table
            table = new JTable();

            // allow for individual cell selection and turn off grid lines.
            table.setCellSelectionEnabled(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setShowGrid(false);

            // avoid reordering and resizing of columns
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setResizingAllowed(false);

            // add listener
            table.addMouseListener(this);

            // place table on form
            JPanel headerPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            headerPane.add(table.getTableHeader());

            JPanel tablePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            tablePane.add(table);

            contentPane = new JPanel();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

            contentPane.add(headerPane);
            contentPane.add(tablePane);

            add(contentPane);
        }

        /**
         * Displays the passed data in the grid.
         * 
         * @param data
         *            Data to display in the table. If this parameter is null
         *            the method will do nothing.
         * @param columnNames
         *            Column names which will be displayed in the header, too.
         *            If this parameter is null the method will do nothing.
         * @param selectedCell
         *            Defines the cell which has to be selected.
         * @param showHeader
         *            true if the header should be displayed; false if not.
         */
        public void fillGrid(Object[][] data, String[] columnNames, Point selectedCell, boolean showHeader) {

            // check if valid data is available
            if (data == null || columnNames == null)
                return;

            // change model of table
            table.setModel(new SelectionTableModel(data, columnNames));

            // set header visible or not
            table.getTableHeader().setVisible(showHeader);

            // add a cell renderer to all cells which shows cell content
            // centered
            TableCellRenderer cellRenderer = new CenterTableCellRenderer();

            for (int i = 0; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
            }

            // set the size of rows and columns
            resizeTableRowHeight();
            resizeTableColumnWidth();

            // select cell
            if (selectedCell != null)
                table.changeSelection(selectedCell.x, selectedCell.y, false, false);
        }

        /**
         * Checks if the selected cell has a valid value. The method will return
         * false if the cell value is null or the user clicked in an empty table
         * space where no cell is located; otherwise the method returns true.
         * 
         * @return boolean value if cell is unequal null and user clicked inside
         *         a cell.
         */
        private boolean isValidCellSelected(Point point) {

            return table.getModel().getValueAt(table.getSelectedRow(), table.getSelectedColumn()) != null && table.rowAtPoint(point) >= 0;
        }

        /**
         * Sets the size of the table to the size of the available space.
         */
        public void resizeTableSpace() {

            table.setSize(new Dimension(contentPane.getWidth() - 4, contentPane.getHeight()));
            table.setPreferredSize(new Dimension(contentPane.getWidth() - 4, contentPane.getHeight()));
        }

        /**
         * Computes the height of the rows so they will fit to the whole height
         * of the the table.
         */
        public void resizeTableRowHeight() {

            int headerHeight = 0;

            if (displayMode == DisplayMode.DAYS)
                headerHeight = table.getTableHeader().getHeight();

            int rowHeight = (table.getHeight() - headerHeight) / table.getRowCount();

            if (rowHeight > 0)
                table.setRowHeight(rowHeight);
        }

        /**
         * Sets the size of the columns so all columns will have the same size
         * and all columns will fit into the space of the table component.
         */
        public void resizeTableColumnWidth() {

            JTableHeader tableHeader = table.getTableHeader();

            for (int i = 0; i < tableHeader.getColumnModel().getColumnCount(); i++) {
                tableHeader.getColumnModel().getColumn(i).setWidth(table.getColumnModel().getColumn(i).getWidth());
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * When a cell in the table was clicked which contains a valid data, the
         * view controller and view mode will be changed.
         */
        public void mouseClicked(MouseEvent arg0) {

            if (arg0.getButton() == MouseEvent.BUTTON1) {

                if (isValidCellSelected(arg0.getPoint())) {

                    calendarViewController.setDateOfCellValue(table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));

                    switch (displayMode) {
                    case YEARS:
                        changeDisplayMode(DisplayMode.MONTHS);
                        updateDateDisplay();
                        break;
                    case MONTHS:
                        changeDisplayMode(DisplayMode.DAYS);
                        updateDateDisplay();
                        break;
                    case DAYS:
                        informAllJHVCalendarListeners(new JHVCalendarEvent(this.getParent()));
                        break;
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mouseEntered(MouseEvent arg0) {
        }

        /**
         * {@inheritDoc}
         */
        public void mouseExited(MouseEvent arg0) {
        }

        /**
         * {@inheritDoc}
         */
        public void mousePressed(MouseEvent arg0) {
        }

        /**
         * {@inheritDoc}
         */
        public void mouseReleased(MouseEvent arg0) {
        }
    }

    /**
     * Table model for the used JTable. This model did not allow to edit cells
     * of the table.
     * 
     * @author Stephan Pagel
     */
    private class SelectionTableModel extends DefaultTableModel {

        // ////////////////////////////////////////////////////////////
        // Definitions
        // ////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        // ////////////////////////////////////////////////////////////
        // Methods
        // ////////////////////////////////////////////////////////////

        /**
         * Default constructor.
         */
        public SelectionTableModel(Object[][] data, String[] columnNames) {

            super(data, columnNames);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns always false, so all cells cannot be edited.
         */

        public boolean isCellEditable(int row, int column) {

            return false;
        }
    }

    /**
     * Table cell renderer for the used JTable. This renderer displays all cell
     * entries centered.
     * 
     * @author Stephan Pagel
     * 
     */
    private class CenterTableCellRenderer extends DefaultTableCellRenderer {

        // ////////////////////////////////////////////////////////////
        // Definitions
        // ////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        // ////////////////////////////////////////////////////////////
        // Methods
        // ////////////////////////////////////////////////////////////

        /**
         * Default constructor.
         */
        public CenterTableCellRenderer() {

            setHorizontalAlignment(CENTER);
        }

        /**
         * Overrides this method so it will be set up correctly with super class
         * but returns its own instance.
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return this;
        }
    }

    /**
     * Panel which acts as a container of the button which displays the current
     * date and allows to set the current date to the calendar component.
     * 
     * @author Stephan Pagel
     */
    private class BottomPanel extends JPanel implements ActionListener {

        // ////////////////////////////////////////////////////////////
        // Definitions
        // ////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        private JButton dateButton = new JButton();

        // ////////////////////////////////////////////////////////////
        // Methods
        // ////////////////////////////////////////////////////////////

        /**
         * Default constructor.
         */
        public BottomPanel() {

            initVisualComponents();
        }

        /**
         * Initialize the visual parts of the component.
         */
        private void initVisualComponents() {

            // set basic layout
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));

            // set up button
            dateButton.setText("Today is " + dateFormat.format(new GregorianCalendar().getTime()));
            dateButton.setBorder(BorderFactory.createEtchedBorder());
            dateButton.addActionListener(this);

            // add label to component
            add(dateButton);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Sets the calendar component to the current date.
         */
        public void actionPerformed(ActionEvent e) {

            changeDisplayMode(DisplayMode.DAYS);
            setDate(new GregorianCalendar().getTime());
        }
    }
}
