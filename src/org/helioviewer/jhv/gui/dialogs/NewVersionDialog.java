package org.helioviewer.jhv.gui.dialogs;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.swing.DesktopIntegration;
import org.helioviewer.jhv.threads.JHVThread;

import com.jidesoft.dialog.ButtonPanel;

import okio.BufferedSource;

@SuppressWarnings("serial")
public class NewVersionDialog extends TextDialog {

    private NewVersionDialog(String _text) {
        super("New Version Available", _text, false);
    }

    public static void check() {
        JHVThread.create(() -> {
            try (NetClient nc = NetClient.of(new URI(JHVGlobals.downloadURL + "VERSION")); BufferedSource source = nc.getSource()) {
                String version = source.readUtf8Line();
                if (version == null || version.isEmpty())
                    throw new Exception("Update Checker: Empty version string");

                String runningVersion = JHVGlobals.version + '.' + JHVGlobals.revision;
                if (JHVGlobals.alphanumComparator.compare(version, runningVersion) > 0) {
                    Log.info("Found newer version " + version);
                    EventQueue.invokeLater(() -> new NewVersionDialog(updateAvailableMessage(version, runningVersion)).showDialog());
                } else {
                    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(null, upToDateMessage(runningVersion)));
                }
            } catch (Exception e) {
                Log.warn(e);
                Message.warn("Update check error", failedMessage(e));
            }
        }, "JHV-CheckUpdate").start();
    }

    @Override
    public ButtonPanel createButtonPanel() {
        ButtonPanel panel = super.createButtonPanel();

        AbstractAction download = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DesktopIntegration.openURL(JHVGlobals.downloadURL);
                setVisible(false);
            }
        };
        setDefaultAction(download);

        JButton downBtn = new JButton(download);
        downBtn.setEnabled(DesktopIntegration.canBrowse);
        downBtn.setText("Download");
        setInitFocusedComponent(downBtn);

        panel.add(downBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        return panel;
    }

    private static String updateAvailableMessage(String version, String runningVersion) {
        return "JHelioviewer " + version + " is now available (you have " + runningVersion + ").";
    }

    private static String upToDateMessage(String runningVersion) {
        return "You are running the latest JHelioviewer version (" + runningVersion + ')';
    }

    private static String failedMessage(Exception e) {
        return "While checking for a newer version got " + e.getMessage();
    }

}
