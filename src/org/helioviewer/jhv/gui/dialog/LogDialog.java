package org.helioviewer.jhv.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.app.AppInfo;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.TransferAccess;
import org.helioviewer.jhv.gui.component.HTMLPane;
import org.helioviewer.jhv.thread.Task;

import com.jidesoft.dialog.ButtonPanel;

@SuppressWarnings({"serial", "this-escape"})
public final class LogDialog extends BaseDialog {

    private final String log;

    private LogDialog(String _log) {
        super("JHelioviewer Log", true);
        log = _log;
    }

    public static void open() {
        Task.submitBackground("log", Log::get, log -> new LogDialog(log).showDialog(), Log::error);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        JButton copy = new JButton("Copy Log");
        copy.addActionListener(e -> TransferAccess.writeClipboard(log));
        ButtonPanel panel = new CloseButtonPanel(this);
        panel.add(copy, ButtonPanel.OTHER_BUTTON);
        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        HTMLPane report = new HTMLPane();
        report.setOpaque(false);
        report.addHyperlinkListener(DesktopIntegration.hyperOpenURL);
        report.setText("If you encounter a problem, please attach this log to the problem description and open an issue at <a href='" + AppInfo.bugURL + "'>" + AppInfo.bugURL + "</a>.");

        JTextArea textArea = new JTextArea(log);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JPanel content = new JPanel(new BorderLayout(0, GAP));
        content.add(report, BorderLayout.PAGE_START);
        content.add(scrollPane, BorderLayout.CENTER);
        return content;
    }

}
