package org.helioviewer.jhv.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.ImageSelectorPanel;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public final class ImageDialog extends StandardDialog implements Interfaces.DatasetSelectionHandler {

    private final Interfaces.DatasetSelectionHandler selectionHandler;
    private final ImageSelectorPanel imageSelectorPanel;
    private final AbstractAction load = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            imageSelectorPanel.loadSelectedDataset();
        }
    };
    private final JButton actionButton = new JButton(load);

    public ImageDialog(Interfaces.DatasetSelectionHandler _selectionHandler) {
        super(MainFrame.get(), "New Image Layer", false);
        selectionHandler = _selectionHandler;
        imageSelectorPanel = new ImageSelectorPanel(this);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        setType(Window.Type.UTILITY);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);
        setDefaultAction(load);
        setInitFocusedComponent(imageSelectorPanel.getFocusedComponent());

        JButton cancelButton = new JButton(close);
        cancelButton.setText("Cancel");
        getRootPane().setDefaultButton(actionButton);

        ButtonPanel panel = new ButtonPanel();
        panel.add(imageSelectorPanel.getAvailabilityButton(), ButtonPanel.OTHER_BUTTON);
        panel.add(cancelButton, ButtonPanel.CANCEL_BUTTON);
        panel.add(actionButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        content.add(imageSelectorPanel);
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog(boolean changeMode) {
        setTitle(changeMode ? "Change Dataset" : "New Image Layer");
        actionButton.setText(changeMode ? "Change" : "Add");
        if (!isVisible()) {
            pack();
            JPanel layersPanel = MainFrame.getLayersPanel();
            Point location = new Point(layersPanel.getWidth(), 0);
            SwingUtilities.convertPointToScreen(location, layersPanel);
            setLocation(location);
            setVisible(true);
        } else {
            toFront();
        }
    }

    public void selectDataset(String server, int sourceId) {
        imageSelectorPanel.selectDataset(server, sourceId);
    }

    @Override
    public void setDefaultTimeRange(long start, long end) {
        selectionHandler.setDefaultTimeRange(start, end);
    }

    @Override
    public void loadDataset(String server, int sourceId) {
        setVisible(false);
        selectionHandler.loadDataset(server, sourceId);
    }
}
