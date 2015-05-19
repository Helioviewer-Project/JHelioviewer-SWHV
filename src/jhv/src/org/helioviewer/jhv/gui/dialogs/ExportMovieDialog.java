package org.helioviewer.jhv.gui.dialogs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * Dialog to export movies to standard video formats.
 *
 * @author Markus Langenberg
 * @author Andre Dau
 */
@SuppressWarnings({"serial"})
public class ExportMovieDialog extends JDialog implements ActionListener, ShowableDialog {

    private final JLabel movieLabel = new JLabel("", SwingConstants.CENTER);
    final JButton exportButton = new JButton("Click to start export");

    public void setLabelText(String exportingText) {
        this.movieLabel.setText(exportingText);
    }

    public void reset() {
        setVisible(false);
        remove(movieLabel);
        this.exportButton.setEnabled(true);
        this.exportButton.setVisible(true);
    }

    private class CloseDialogTask extends TimerTask {
        @Override
        public void run() {
            reset();
        }
    }

    public void fail() {
        this.movieLabel.setText("No image series. Aborting...");
        Timer timer = new Timer();
        timer.schedule(new CloseDialogTask(), 2000);
    }

    public ExportMovieDialog() {
        super(ImageViewerGui.getMainFrame(), "Export Movie", true);
        ImageViewerGui.getLeftContentPane().setEnabled(false);

        final ComponentView componentView = ImageViewerGui.getComponentView();
        final ExportMovieDialog exportMovieDialog = this;

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                add(movieLabel);
                movieLabel.setText("Export started...");
                componentView.startExport(exportMovieDialog);
                exportButton.setEnabled(false);
                exportButton.setVisible(false);
            }
        });
        this.add(exportButton);
    }

    @Override
    public void init() {
        ImageViewerGui.getLeftContentPane().setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
        setSize(new Dimension(180, 60));

        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

}
