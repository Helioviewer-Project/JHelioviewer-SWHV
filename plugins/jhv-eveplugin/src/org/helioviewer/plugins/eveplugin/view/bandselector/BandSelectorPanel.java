package org.helioviewer.plugins.eveplugin.view.bandselector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.Band;
import org.helioviewer.plugins.eveplugin.controller.BandController;
import org.helioviewer.plugins.eveplugin.controller.BandControllerListener;
import org.helioviewer.plugins.eveplugin.controller.DownloadController;
import org.helioviewer.plugins.eveplugin.controller.DownloadControllerListener;

public class BandSelectorPanel extends JPanel implements DownloadControllerListener, BandControllerListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;
    
    private static final String PROGRESSBAR_TOOLTIP_ACTIVEDOWNLOAD = "Downloading data from server.";
    private static final String PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD = "No data selected to download from server.";
    
    private final String identifier;
    
    private final JLabel groupLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar();
    private final BandContainer bandsContainer;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public BandSelectorPanel(final String identifer, final String plotName) {
        this.identifier = identifer;
        bandsContainer = new BandContainer(identifier);
        
        initVisualComponents(plotName);
        
        BandController.getSingletonInstance().addBandControllerListener(this);
        DownloadController.getSingletonInstance().addListener(this);
    }
    
    private void initVisualComponents(final String plotName) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, getHeight()));
        
        final JPanel headerPane = new JPanel();
        headerPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        headerPane.add(new JLabel(plotName));
        headerPane.add(groupLabel);
        
        add(headerPane, BorderLayout.PAGE_START);
        add(bandsContainer, BorderLayout.CENTER);
        add(progressBar, BorderLayout.PAGE_END);
        
        progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD);
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Download Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void downloadStarted(final Band band, final Interval<Date> interval) {
        if (BandController.getSingletonInstance().isBandAvailable(identifier, band)) {
            progressBar.setIndeterminate(true);
            progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_ACTIVEDOWNLOAD);    
        }
    }

    public void downloadFinished(final Band band, final Interval<Date> interval, final int activeBandDownloads) {
        final Band[] bands = BandController.getSingletonInstance().getBands(identifier);
        boolean active = false;
            
        for (final Band b : bands) {
            if (DownloadController.getSingletonInstance().isDownloadActive(b)) {
                active = true;
                break;
            }
        }
            
        progressBar.setIndeterminate(active);
            
        if (!active) {
            progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD);
        }    
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void bandAdded(final Band band, final String identifier) {}

    public void bandRemoved(final Band band, final String identifier) {}

    public void bandUpdated(final Band band, final String identifer) {}

    public void bandGroupChanged(final String identifer) {
        if (this.identifier.equals(identifer)) {
            groupLabel.setText(BandController.getSingletonInstance().getSelectedGroup(identifer).getGroupLabel());
        }
    }
}
