package org.helioviewer.plugins.eveplugin.view.bandselector;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.plugins.eveplugin.controller.Band;
import org.helioviewer.plugins.eveplugin.controller.BandController;
import org.helioviewer.plugins.eveplugin.controller.BandControllerListener;

/**
 * @author Stephan Pagel
 * */
public class BandContainer extends JPanel implements BandControllerListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;

    private final String identifier;
    
    private final CardLayout layout = new CardLayout();
    
    private final BandList list;
    private final JLabel emptyLabel = new JLabel("No Bands Added yet", JLabel.CENTER);
    
    private final JScrollPane listScrollPane;
    private final JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    /**
     * Default constructor.
     * */
    public BandContainer(final String identifier) {   
        this.identifier = identifier;
        
        list = new BandList(identifier);
        listScrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        BandController.getSingletonInstance().addBandControllerListener(this);
        
        initVisualComponent();
    }
    
    /**
     * Sets up the visual sub components and the component itself.
     * */
    private void initVisualComponent() {
        // setup tableScrollPane
        listScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
  
        // setup emptyScrollPane
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        emptyLabel.setOpaque(false);
        emptyLabel.setBackground(Color.WHITE);
        

        emptyScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // setup container panel
        this.setLayout(layout);        
        this.add(emptyScrollPane, "empty");
        this.add(listScrollPane, "list");
        
        update();
    }
    
    /**
     * 
     * */
    private void update() {
        if (BandController.getSingletonInstance().getNumberOfAvailableBands(identifier) > 0) {
            layout.show(this, "list");
        } else {
            layout.show(this, "empty");
        }
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    
    public void bandAdded(Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            update();    
        }
    }

    public void bandRemoved(Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            update();    
        }
    }

    public void bandUpdated(Band band, final String identifier) {}

    public void bandGroupChanged(final String identifier) {
        if (this.identifier.equals(identifier)) {
            update();    
        }
    }
}
