package org.helioviewer.plugins.eveplugin.view.bandselector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.plugins.eveplugin.controller.Band;
import org.helioviewer.plugins.eveplugin.controller.BandController;

/**
 * @author Stephan Pagel
 * */
public class BandListEntry extends JPanel implements MouseListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;

    private final String identifier;
    
    private final Band band;
    private final BandList list;
    
    private final JProgressBar downloadProgressBar = new JProgressBar();
    
    private final JLabel visibilityLabel = new JLabel();
    private final JPanel downloadPane = new JPanel();    
    private final JLabel titleLabel = new JLabel();
    private final JLabel removeLabel = new JLabel();
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public BandListEntry(final BandList list, final Band band, final String identifier) {
        this.band = band;
        this.list = list;
        this.identifier = identifier;
        
        initVisualComponents();
        updateVisualComponentValues();
    }
    
    private void initVisualComponents() {
        setLayout(new BorderLayout());
        
        setMinimumSize(new Dimension(getMinimumSize().width, 26));
        setPreferredSize(new Dimension(getPreferredSize().width, 26));
        setMaximumSize(new Dimension(getMaximumSize().width, 26));
        
        downloadPane.setLayout(new BoxLayout(downloadPane, BoxLayout.X_AXIS));
        downloadPane.setPreferredSize(new Dimension(26, 26));
        downloadPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        downloadPane.setOpaque(false);
        downloadPane.add(downloadProgressBar);
        
        final JPanel westPane = new JPanel();
        westPane.setLayout(new BorderLayout());
        westPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        westPane.setOpaque(false);
        westPane.add(visibilityLabel, BorderLayout.WEST);
        westPane.add(downloadPane, BorderLayout.EAST);
        
        final JPanel eastPane = new JPanel();
        eastPane.setLayout(new BorderLayout());
        eastPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        eastPane.setOpaque(false);
        eastPane.add(removeLabel, BorderLayout.EAST);
        
        add(westPane, BorderLayout.WEST);
        add(titleLabel, BorderLayout.CENTER);
        add(eastPane, BorderLayout.EAST);
        
        downloadProgressBar.setPreferredSize(new Dimension(20, 12));
        downloadProgressBar.setIndeterminate(true);
        downloadProgressBar.setVisible(false);
        
        visibilityLabel.addMouseListener(this);
        downloadPane.addMouseListener(this);
        downloadProgressBar.addMouseListener(this);
        westPane.addMouseListener(this);
        eastPane.addMouseListener(this);
        titleLabel.addMouseListener(this);
        removeLabel.addMouseListener(this);
    }
    
    public Band getBand() {
        return band;
    }
    
    public void setDownloadActive(final boolean active) {
        downloadProgressBar.setVisible(active);
    }
    
    public void updateVisualComponentValues() {
        if (band.isVisible()) {
            visibilityLabel.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
            visibilityLabel.setToolTipText("Hide band");
        } else {
            visibilityLabel.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
            visibilityLabel.setToolTipText("Make band visible");
        }
        
        titleLabel.setText(band.getTitle());
        titleLabel.setForeground(band.getGraphColor());
        
        removeLabel.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
        removeLabel.setToolTipText("Remove Band");
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Mouse Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    public void mouseClicked(MouseEvent arg0) {
        if (arg0.getSource() == visibilityLabel) {
            BandController.getSingletonInstance().setBandVisibility(identifier, band.getBandType(), !band.isVisible());
        } else if (arg0.getSource() == removeLabel) {
            BandController.getSingletonInstance().removeBand(identifier, band);
        } else {
            list.selectItem(band);
        }
    }

    public void mouseEntered(MouseEvent arg0) {}

    public void mouseExited(MouseEvent arg0) {}

    public void mousePressed(MouseEvent arg0) {}

    public void mouseReleased(MouseEvent arg0) {}
}
