package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.component.FormPanel;
import org.helioviewer.jhv.gui.dialog.BaseDialog;
import org.helioviewer.jhv.plugins.swek.SWEKIconImages;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
class EventDescriptionPanel extends JPanel {

    EventDescriptionPanel(SWEKSession session, RelatedEvents related, SolarEvent event) {
        super(new BorderLayout(BaseDialog.GAP, 0));

        FormPanel times = new FormPanel();
        times.addRow("Start", timeText(event.start));
        times.addRow("End", timeText(event.end));

        JLabel colorLabel = new JLabel();
        colorLabel.setBackground(related.getColor());
        colorLabel.setOpaque(true);
        colorLabel.setPreferredSize(new Dimension(0, 4));

        JPanel summary = new JPanel(new BorderLayout(0, 2));
        summary.add(times, BorderLayout.CENTER);
        summary.add(colorLabel, BorderLayout.PAGE_END);

        add(new JLabel(new ImageIcon(SWEKIconImages.getImage(event.getSupplier().group().getIconKey()))), BorderLayout.LINE_START);
        add(summary, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                session.highlight(related);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                session.highlight(null);
            }
        });
    }

    private static JTextArea timeText(long time) {
        JTextArea text = new JTextArea(TimeUtils.formatShort(time));
        text.setEditable(false);
        text.setOpaque(false);
        return text;
    }

}
