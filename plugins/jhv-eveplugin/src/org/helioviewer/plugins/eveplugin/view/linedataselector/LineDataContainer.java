package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.BandControllerListener;

/**
 * @author Stephan Pagel
 * */
public class LineDataContainer extends JPanel implements LineDataSelectorModelListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;

    private final String identifier;
    
    private final CardLayout layout = new CardLayout();
    
    private final LineDataList list;
    private final JLabel emptyLabel = new JLabel("No Bands Added yet", JLabel.CENTER);
    
    private final JScrollPane listScrollPane;
    private final JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    private LineDataSelectorModel lineDataModel;
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    /**
     * Default constructor.
     * */
    public LineDataContainer(final String identifier) {   
        this.identifier = identifier;
        
        lineDataModel = LineDataSelectorModel.getSingletonInstance();
        lineDataModel.addLineDataSelectorModelListener(this);
        list = new LineDataList(identifier);
        listScrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        //BandController.getSingletonInstance().addBandControllerListener(this);
        
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
        if (lineDataModel.getNumberOfAvailableLineData(identifier)>0) {
            layout.show(this, "list");
        } else {
            layout.show(this, "empty");
        }
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    @Override
	public void downloadStartded(LineDataSelectorElement element) {}

	@Override
	public void downloadFinished(LineDataSelectorElement element) {}

	@Override
	public void lineDataAdded(LineDataSelectorElement element) {
		update();
		
	}

	@Override
	public void lineDataRemoved(LineDataSelectorElement element) {
		update();
		
	}

	@Override
	public void lineDataUpdated(LineDataSelectorElement element) {
		update();		
	}
}
