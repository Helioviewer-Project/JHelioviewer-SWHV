package ch.fhnw.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * Panel managing a collapsible area.
 * 
 * <p>
 * This panel consists of a toggle button and one arbitrary component. Clicking
 * the toggle button will toggle the visibility of the component.
 * 
 * @author Markus Langenberg
 * 
 *         Adapted by:
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 */
public class CollapsiblePane extends JComponent implements ActionListener {

    private static final long serialVersionUID = 1L;

    /**
     * Expanded icon
     */
    private static final ImageIcon expandedIcon = new ImageIcon(CollapsiblePane.class.getResource("arrow.plain.down.gif"));

    /**
     * Collapsed icon
     */
    private static final ImageIcon collapsedIcon = new ImageIcon(CollapsiblePane.class.getResource("arrow.plain.right.gif"));

    /**
     * Toggle button
     */
    private JToggleButton toggleButton;

    /**
     * The component itself which contains the gui elements
     */
    private Component component;

    /**
     * Listeners for receiving if its collapsed or not
     */
    private ArrayList<CollapsiblePaneResizeListener> listeners = new ArrayList<CollapsiblePane.CollapsiblePaneResizeListener>();

    /**
     * Default constructor.
     * 
     * @param title
     *            Text on the toggle button
     * @param component
     *            Component to manage
     * @param startExpanded
     *            if true, the component will be visible right from the start
     */
    public CollapsiblePane(String title, Component component, boolean startExpanded, int width) {
        setLayout(new BorderLayout());

        toggleButton = new JToggleButton(title);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setSelected(startExpanded);
        toggleButton.setFont(new Font("SansSerif", Font.BOLD, 12));

        if (startExpanded) {
            toggleButton.setIcon(expandedIcon);
        } else {
            toggleButton.setIcon(collapsedIcon);
        }

        toggleButton.setPreferredSize(new Dimension(width, (int) toggleButton.getPreferredSize().getHeight()));
        toggleButton.addActionListener(this);

        this.component = component;
        component.setVisible(startExpanded);

        add(toggleButton, BorderLayout.PAGE_START);
        add(component, BorderLayout.CENTER);

        addComponentListener(new ComponentListener() {

            public void componentShown(ComponentEvent arg0) {
                // TODO Auto-generated method stub

            }

            public void componentResized(ComponentEvent e) {
                CollapsiblePane panel = (CollapsiblePane) e.getSource();

                for (CollapsiblePaneResizeListener listener : listeners) {
                    listener.gotResized(panel, panel.getPreferredSize());
                }
            }

            public void componentMoved(ComponentEvent arg0) {
                // TODO Auto-generated method stub

            }

            public void componentHidden(ComponentEvent arg0) {
                // TODO Auto-generated method stub

            }
        });
    }

    /**
     * Expands the pane.
     */
    public void expand() {
        toggleButton.setSelected(true);
        component.setVisible(true);
        toggleButton.setIcon(expandedIcon);
    }

    /**
     * Collapses the pane.
     */
    public void collapse() {
        toggleButton.setSelected(false);
        component.setVisible(false);
        toggleButton.setIcon(collapsedIcon);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        if (component.isVisible()) {
            collapse();
        } else {
            expand();
        }
    }

    /**
     * Add a listener
     * 
     * @param listener
     *            CollapsiblePaneResizeListener
     */
    public void addListener(CollapsiblePaneResizeListener listener) {
        listeners.add(listener);
    }

    public interface CollapsiblePaneResizeListener {
        public void gotResized(CollapsiblePane pane, Dimension size);
    }
}
