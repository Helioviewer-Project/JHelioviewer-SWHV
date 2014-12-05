package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class LineDataSelectorPanel extends JPanel implements LineDataSelectorModelListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private static final String PROGRESSBAR_TOOLTIP_ACTIVEDOWNLOAD = "Downloading data from server.";
    private static final String PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD = "No data selected to download from server.";

    private final String identifier;

    private final JLabel groupLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar();
    private final LineDataContainer bandsContainer;

    private final LineDataSelectorModel model = LineDataSelectorModel.getSingletonInstance();

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public LineDataSelectorPanel(final String identifer, final String plotName) {
        identifier = identifer;
        bandsContainer = new LineDataContainer(identifier);

        initVisualComponents(plotName);

        model.addLineDataSelectorModelListener(this);
    }

    private void initVisualComponents(final String plotName) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, getHeight()));

        add(bandsContainer, BorderLayout.CENTER);
        add(progressBar, BorderLayout.PAGE_END);

        progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD);
    }

    public void bandGroupChanged(final String identifer) {
        if (identifier.equals(identifer)) {
            // groupLabel.setText(BandController.getSingletonInstance().getSelectedGroup(identifer).getGroupLabel());
        }
    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
        if (element.getPlotIdentifier().equals(identifier) && element.isAvailable()) {
            progressBar.setIndeterminate(true);
            progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_ACTIVEDOWNLOAD);
        }
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
        if (element.getPlotIdentifier().equals(identifier)) {
            boolean active = model.atLeastOneDownloading(element.getPlotIdentifier());

            progressBar.setIndeterminate(active);

            if (!active) {
                progressBar.setToolTipText(PROGRESSBAR_TOOLTIP_INACTIVEDOWNLOAD);
            }
        }

    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }
}
