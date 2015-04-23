package org.helioviewer.jhv.plugins.swek.view;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.view.filter.AbstractFilterPanel;
import org.helioviewer.jhv.plugins.swek.view.filter.FilterPanelFactory;

/**
 * Creates a filter dialog.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class FilterDialog extends JDialog implements FocusListener, WindowFocusListener {

    /** The event type */
    private final SWEKEventType eventType;

    public FilterDialog(SWEKEventType eventType) {
        super();
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Dialog owner, boolean modal, SWEKEventType eventType) {
        super(owner, modal);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc, SWEKEventType eventType) {
        super(owner, title, modal, gc);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Dialog owner, String title, boolean modal, SWEKEventType eventType) {
        super(owner, title, modal);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Dialog owner, String title, SWEKEventType eventType) {
        super(owner, title);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Dialog owner, SWEKEventType eventType) {
        super(owner);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Frame owner, boolean modal, SWEKEventType eventType) {
        super(owner, modal);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc, SWEKEventType eventType) {
        super(owner, title, modal, gc);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Frame owner, String title, boolean modal, SWEKEventType eventType) {
        super(owner, title, modal);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Frame owner, String title, SWEKEventType eventType) {
        super(owner, title);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Frame owner, SWEKEventType eventType) {
        super(owner);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Window owner, ModalityType modalityType, SWEKEventType eventType) {
        super(owner, modalityType);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc, SWEKEventType eventType) {
        super(owner, title, modalityType, gc);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Window owner, String title, ModalityType modalityType, SWEKEventType eventType) {
        super(owner, title, modalityType);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Window owner, String title, SWEKEventType eventType) {
        super(owner, title);
        this.eventType = eventType;
        initFilterDialog();
    }

    public FilterDialog(Window owner, SWEKEventType eventType) {
        super(owner);
        this.eventType = eventType;
        initFilterDialog();
    }

    @Override
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        setVisible(false);
        // dispose();
    }

    @Override
    public void windowGainedFocus(WindowEvent arg0) {
    }

    @Override
    public void windowLostFocus(WindowEvent arg0) {
        setVisible(false);
        // dispose();
    }

    private void initFilterDialog() {
        super.setUndecorated(true);
        // super.setPreferredSize(new Dimension(200, 100));
        super.addFocusListener(this);
        super.addWindowFocusListener(this);
        List<AbstractFilterPanel> filterPanels = FilterPanelFactory.createFilterPanel(eventType);
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridLayout(filterPanels.size(), 1));
        filterPanel.setOpaque(false);
        filterPanel.setBackground(Color.white);
        for (AbstractFilterPanel afp : filterPanels) {
            filterPanel.add(afp);
        }
        super.setContentPane(filterPanel);
        super.pack();
        super.validate();
    }

}
