package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.Color;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.Band;
import org.helioviewer.plugins.eveplugin.controller.BandController;
import org.helioviewer.plugins.eveplugin.controller.BandControllerListener;
import org.helioviewer.plugins.eveplugin.controller.DownloadController;
import org.helioviewer.plugins.eveplugin.controller.DownloadControllerListener;

/**
 * @author Stephan Pagel
 * */
public class LineDataList extends JPanel implements LineDataSelectorModelListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////    
    
    private static final long serialVersionUID = 1L;
    
    private static final Color[] bandColors = { 
        new Color(0, 0, 0), new Color(31, 26, 178), new Color(242, 159, 5), 
        new Color(19, 137, 0), new Color(140, 64, 47), new Color(199, 0, 125), 
        new Color(197, 230, 231), new Color(242, 92, 5), new Color(217, 37, 37), 
        new Color(136, 166, 27), new Color(129, 0, 81), new Color(217, 136, 75), 
        new Color(48, 110, 115), new Color(178, 156, 133), new Color(255, 83, 53), 
        new Color(242, 209, 110), new Color(201, 255, 237), new Color(14, 61, 89), 
        new Color(89, 115, 88), new Color(178, 3, 33), new Color(206, 224, 200), 
        new Color(59, 66, 76), new Color(219, 108, 124)};
    
    private final String identifier;
    
    private final Color selectionBackgroundColor = new JList().getSelectionBackground();
    private final Color selectionForegroundColor = new JList().getSelectionForeground();
    private final LinkedList<LineDataListEntry> entryList = new LinkedList<LineDataListEntry>();
    
    private LineDataSelectorModel model;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////    
    
    public LineDataList(final String identifier) {
        this.identifier = identifier;
        model =LineDataSelectorModel.getSingletonInstance();
        initVisualComponents();        
        
        model.addLineDataSelectorModelListener(this);
    }
    
    private void initVisualComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
    
    private void refillListEntries() {
        for (final LineDataListEntry entry : entryList) {
            remove(entry);
        }
        entryList.clear();
        
        final List<LineDataSelectorElement> elements = model.getAllLineDataSelectorElements(identifier);

        for (final LineDataSelectorElement el : elements) {
            addEntry(el);
        }
    }
    
    private void removeEntry(final Band band) {
        if (band == null)
            return;
        
        for (int i = 0; i < entryList.size(); i++) {
            final LineDataListEntry entry = entryList.get(i);
            
            if (entry.getLineDataSelectorElement().equals(band)) {
                remove(entry);
                entryList.remove(entry);
                
                selectItem(i);
            }
        }
    }
    
    private void addEntry(final LineDataSelectorElement element) {
        if (element == null) {
            return;
        }
        
        element.setDataColor(bandColors[entryList.size() % bandColors.length]); 
        
        final LineDataListEntry entry = new LineDataListEntry(this, element, identifier);
        add(entry);
        entryList.add(entry);
        selectItem(entryList.size() - 1);
    }
    
    public void selectItem(final LineDataSelectorElement element) {
        if (element == null)
            return;
        
        for (int i = 0; i < entryList.size(); i++) {
            final LineDataListEntry entry = entryList.get(i);
            
            if (entry.getLineDataSelectorElement().equals(element)) {
                entry.setForeground(selectionForegroundColor);
                entry.setBackground(selectionBackgroundColor);
            } else {
                entry.setForeground(Color.BLACK);
                entry.setBackground(Color.WHITE);
            }
        }
    }
    
    public void selectItem(final int index) {
        final int itemCount = entryList.size();
        
        if (itemCount == 0)
            return;
        
        final int pos = Math.max(0, Math.min(itemCount - 1, index));
        
        int i = 0;
        for (final LineDataListEntry entry : entryList) {
            if (i == pos) {
                entry.setForeground(selectionForegroundColor);
                entry.setBackground(selectionBackgroundColor);
            } else {
                entry.setForeground(Color.BLACK);
                entry.setBackground(Color.WHITE);
            }
            
            i++;
        }
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void bandAdded(final Band band, final String identifier) {
        
    }

    public void bandRemoved(final Band band, final String identifier) {
        if (this.identifier.equals(identifier)) {
            removeEntry(band);
            
            repaint();    
        }   
    }

    public void bandUpdated(final Band band, final String identifier) {
        if (this.identifier.equals(identifier) && band != null) {
            for (final LineDataListEntry entry : entryList) {
                if (entry.getLineDataSelectorElement().equals(band)) {
                    entry.updateVisualComponentValues();    
                }
            }    
        }
    }

    public void bandGroupChanged(final String identifier) {
        if (this.identifier.equals(identifier)) {
            refillListEntries();
            selectItem(0);    
        }
    }
    
    
	@Override
	public void downloadStartded(LineDataSelectorElement element) {
		for (final LineDataListEntry entry : entryList) {
            if (entry.getLineDataSelectorElement().equals(element)) {
                entry.setDownloadActive(true);
            }
        }
	}

	@Override
	public void downloadFinished(LineDataSelectorElement element) {
		if (!model.atLeastOneDownloading(element.getPlotIdentifier())) {
            for (final LineDataListEntry entry : entryList) {
                if (entry.getLineDataSelectorElement().equals(element)) {
                    entry.setDownloadActive(false);
                }
            }    
        }		
	}

	@Override
	public void lineDataAdded(LineDataSelectorElement element) {
		if (element.getPlotIdentifier().equals(identifier)){
		    addEntry(element);
            selectItem(element);
            
            repaint();
		}
	}

	@Override
	public void lineDataRemoved(LineDataSelectorElement element) {
		LineDataListEntry toRemove = null;
		if (this.identifier.equals(element.getPlotIdentifier()) && element != null) {
            for (final LineDataListEntry entry : entryList) {
                if (entry.getLineDataSelectorElement().equals(element)) {
                    entry.updateVisualComponentValues();
                    toRemove = entry;
                    break;
                }
            }    
        }
		if(toRemove != null){
			entryList.remove(toRemove);
			Log.debug("Remove entry from compnent.");
			remove(toRemove);
			repaint();
		}
	}

	@Override
	public void lineDataUpdated(LineDataSelectorElement element) {
		if (element != null) {
            for (final LineDataListEntry entry : entryList) {
                if (entry.getLineDataSelectorElement().equals(element)) {
                    entry.updateVisualComponentValues();    
                }
            }    
        }
	}
}
