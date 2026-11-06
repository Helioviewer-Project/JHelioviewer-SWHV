package org.helioviewer.jhv.gui.component;

import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;

// Right-aligned labels beside their controls, rows stacked without gaps.
@SuppressWarnings({"serial", "this-escape"})
public class FormPanel extends JPanel {

    private final GroupLayout layout = new GroupLayout(this);
    private final GroupLayout.ParallelGroup labels = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
    private final GroupLayout.ParallelGroup controls = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
    private final GroupLayout.SequentialGroup rows = layout.createSequentialGroup();

    public FormPanel() {
        setLayout(layout);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(labels)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(controls));
        layout.setVerticalGroup(rows);
    }

    // The controls of a row share its width up to the right edge.
    public final void addRow(String label, JComponent... row) {
        GroupLayout.ParallelGroup line = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
        if (!label.isEmpty()) {
            JLabel name = new JLabel(label);
            labels.addComponent(name);
            line.addComponent(name);
        }
        if (row.length == 1)
            controls.addComponent(row[0], GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        else {
            GroupLayout.SequentialGroup across = layout.createSequentialGroup();
            for (int i = 0; i < row.length; i++) {
                if (i > 0)
                    across.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
                across.addComponent(row[i]);
            }
            controls.addGroup(across);
        }
        for (JComponent control : row)
            line.addComponent(control);
        rows.addGroup(line);
    }

}
